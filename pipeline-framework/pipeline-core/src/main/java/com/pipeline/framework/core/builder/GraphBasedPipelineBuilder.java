package com.pipeline.framework.core.builder;

import com.pipeline.framework.api.graph.NodeType;
import com.pipeline.framework.api.graph.StreamGraph;
import com.pipeline.framework.api.graph.StreamNode;
import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;
import com.pipeline.framework.api.operator.OperatorType;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import com.pipeline.framework.connectors.Connector;
import com.pipeline.framework.connectors.ConnectorRegistry;
import com.pipeline.framework.core.pipeline.Pipeline;
import com.pipeline.framework.core.pipeline.SimplePipeline;
import com.pipeline.framework.operators.OperatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于Graph的Pipeline构建器。
 * <p>
 * 核心功能：
 * 1. 从StreamGraph读取定义
 * 2. 创建Source、Operators、Sink实例
 * 3. 串联成完整的Pipeline
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class GraphBasedPipelineBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(GraphBasedPipelineBuilder.class);
    
    private final ConnectorRegistry connectorRegistry;
    private final OperatorFactory operatorFactory;

    public GraphBasedPipelineBuilder(ConnectorRegistry connectorRegistry,
                                    OperatorFactory operatorFactory) {
        this.connectorRegistry = connectorRegistry;
        this.operatorFactory = operatorFactory;
    }

    /**
     * 从StreamGraph构建Pipeline。
     * <p>
     * 完整流程：
     * 1. 验证Graph
     * 2. 拓扑排序获取执行顺序
     * 3. 创建Source
     * 4. 创建Operators
     * 5. 创建Sink
     * 6. 组装成Pipeline
     * </p>
     *
     * @param graph StreamGraph定义
     * @return Pipeline的Mono
     */
    public Mono<Pipeline<?, ?>> buildFromGraph(StreamGraph graph) {
        log.info("Building pipeline from graph: {}", graph.getGraphId());
        
        return Mono.defer(() -> {
            // 1. 验证Graph
            if (!graph.validate()) {
                return Mono.error(new IllegalArgumentException("Invalid graph: " + graph.getGraphId()));
            }
            
            // 2. 获取拓扑排序的节点
            List<StreamNode> sortedNodes = graph.topologicalSort();
            log.debug("Graph has {} nodes", sortedNodes.size());
            
            // 3. 分类节点
            StreamNode sourceNode = findSourceNode(graph);
            List<StreamNode> operatorNodes = findOperatorNodes(sortedNodes);
            StreamNode sinkNode = findSinkNode(graph);
            
            // 4. 创建组件
            return createSource(sourceNode)
                .flatMap(source -> createOperators(operatorNodes)
                    .flatMap(operators -> createSink(sinkNode)
                        .map(sink -> assemblePipeline(graph, source, operators, sink))));
        })
        .doOnSuccess(p -> log.info("Pipeline built successfully: {}", graph.getGraphName()))
        .doOnError(e -> log.error("Failed to build pipeline from graph: {}", graph.getGraphId(), e));
    }

    /**
     * 查找Source节点。
     */
    private StreamNode findSourceNode(StreamGraph graph) {
        List<StreamNode> sourceNodes = graph.getSourceNodes();
        if (sourceNodes.isEmpty()) {
            throw new IllegalStateException("No source node found in graph");
        }
        if (sourceNodes.size() > 1) {
            throw new IllegalStateException("Multiple source nodes not supported yet");
        }
        return sourceNodes.get(0);
    }

    /**
     * 查找所有Operator节点。
     */
    private List<StreamNode> findOperatorNodes(List<StreamNode> sortedNodes) {
        List<StreamNode> operatorNodes = new ArrayList<>();
        for (StreamNode node : sortedNodes) {
            if (node.getNodeType() == NodeType.OPERATOR) {
                operatorNodes.add(node);
            }
        }
        return operatorNodes;
    }

    /**
     * 查找Sink节点。
     */
    private StreamNode findSinkNode(StreamGraph graph) {
        List<StreamNode> sinkNodes = graph.getSinkNodes();
        if (sinkNodes.isEmpty()) {
            throw new IllegalStateException("No sink node found in graph");
        }
        if (sinkNodes.size() > 1) {
            throw new IllegalStateException("Multiple sink nodes not supported yet");
        }
        return sinkNodes.get(0);
    }

    /**
     * 创建Source实例。
     * <p>
     * 步骤：
     * 1. 从节点配置解析SourceConfig
     * 2. 根据类型获取Connector
     * 3. 使用Connector创建Source
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Mono<DataSource<?>> createSource(StreamNode sourceNode) {
        log.debug("Creating source from node: {}", sourceNode.getNodeId());
        
        return Mono.defer(() -> {
            // 解析配置
            SourceConfig config = parseSourceConfig(sourceNode);
            
            // 获取Connector
            return connectorRegistry.getConnector(config.getType().name().toLowerCase())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                    "Connector not found for type: " + config.getType())))
                // 创建Source
                .flatMap(connector -> connector.createSource(config))
                .doOnSuccess(source -> log.info("Source created: {} (type: {})", 
                    source.getName(), config.getType()));
        });
    }

    /**
     * 创建所有Operator实例。
     */
    private Mono<List<Operator<?, ?>>> createOperators(List<StreamNode> operatorNodes) {
        log.debug("Creating {} operators", operatorNodes.size());
        
        List<Mono<Operator<?, ?>>> operatorMonos = new ArrayList<>();
        
        for (StreamNode node : operatorNodes) {
            Mono<Operator<?, ?>> operatorMono = createOperator(node);
            operatorMonos.add(operatorMono);
        }
        
        // 并行创建所有Operator
        return Mono.zip(operatorMonos, objects -> {
            List<Operator<?, ?>> operators = new ArrayList<>();
            for (Object obj : objects) {
                operators.add((Operator<?, ?>) obj);
            }
            return operators;
        });
    }

    /**
     * 创建单个Operator实例。
     */
    private Mono<Operator<?, ?>> createOperator(StreamNode operatorNode) {
        log.debug("Creating operator from node: {}", operatorNode.getNodeId());
        
        return Mono.defer(() -> {
            // 解析配置
            OperatorConfig config = parseOperatorConfig(operatorNode);
            
            // 使用Factory创建Operator
            return operatorFactory.createOperator(config.getType(), config)
                .doOnSuccess(operator -> log.info("Operator created: {} (type: {})", 
                    operator.getName(), config.getType()));
        });
    }

    /**
     * 创建Sink实例。
     */
    @SuppressWarnings("unchecked")
    private Mono<DataSink<?>> createSink(StreamNode sinkNode) {
        log.debug("Creating sink from node: {}", sinkNode.getNodeId());
        
        return Mono.defer(() -> {
            // 解析配置
            SinkConfig config = parseSinkConfig(sinkNode);
            
            // 获取Connector
            return connectorRegistry.getConnector(config.getType().name().toLowerCase())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                    "Connector not found for type: " + config.getType())))
                // 创建Sink
                .flatMap(connector -> connector.createSink(config))
                .doOnSuccess(sink -> log.info("Sink created: {} (type: {})", 
                    sink.getName(), config.getType()));
        });
    }

    /**
     * 组装成完整的Pipeline。
     */
    @SuppressWarnings("unchecked")
    private Pipeline<?, ?> assemblePipeline(StreamGraph graph,
                                           DataSource<?> source,
                                           List<Operator<?, ?>> operators,
                                           DataSink<?> sink) {
        log.info("Assembling pipeline: {}", graph.getGraphName());
        
        return new SimplePipeline<>(
            graph.getGraphName(),
            (DataSource<Object>) source,
            operators,
            (DataSink<Object>) sink
        );
    }

    /**
     * 解析Source配置。
     */
    private SourceConfig parseSourceConfig(StreamNode node) {
        Map<String, Object> config = node.getConfig();
        
        // 这里简化处理，实际应该根据配置创建具体的Config对象
        return new SimpleSourceConfig(config);
    }

    /**
     * 解析Operator配置。
     */
    private OperatorConfig parseOperatorConfig(StreamNode node) {
        Map<String, Object> config = node.getConfig();
        String operatorType = node.getOperatorType();
        
        return new SimpleOperatorConfig(
            OperatorType.valueOf(operatorType.toUpperCase()),
            config
        );
    }

    /**
     * 解析Sink配置。
     */
    private SinkConfig parseSinkConfig(StreamNode node) {
        Map<String, Object> config = node.getConfig();
        
        return new SimpleSinkConfig(config);
    }
}
