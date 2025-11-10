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
import com.pipeline.framework.core.factory.OperatorFactory;
import com.pipeline.framework.core.factory.SinkFactory;
import com.pipeline.framework.core.factory.SourceFactory;
import com.pipeline.framework.core.pipeline.Pipeline;
import com.pipeline.framework.core.pipeline.SimplePipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Graph 的 Pipeline 构建器。
 * <p>
 * 核心功能：
 * 1. 从 StreamGraph 读取定义
 * 2. 创建 Source、Operators、Sink 实例
 * 3. 串联成完整的 Pipeline
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class GraphPipelineBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(GraphPipelineBuilder.class);
    
    private final SourceFactory sourceFactory;
    private final SinkFactory sinkFactory;
    private final OperatorFactory operatorFactory;
    private final Scheduler pipelineScheduler;

    /**
     * 构造函数注入所有依赖。
     *
     * @param sourceFactory     Source 工厂
     * @param sinkFactory       Sink 工厂
     * @param operatorFactory   Operator 工厂
     * @param pipelineScheduler Pipeline 调度器
     */
    public GraphPipelineBuilder(
            SourceFactory sourceFactory,
            SinkFactory sinkFactory,
            OperatorFactory operatorFactory,
            @Qualifier("pipelineScheduler") Scheduler pipelineScheduler) {
        this.sourceFactory = sourceFactory;
        this.sinkFactory = sinkFactory;
        this.operatorFactory = operatorFactory;
        this.pipelineScheduler = pipelineScheduler;
        
        log.info("GraphPipelineBuilder initialized");
        log.info("Supported sources: {}", sourceFactory.getSupportedTypes());
        log.info("Supported sinks: {}", sinkFactory.getSupportedTypes());
        log.info("Supported operators: {}", operatorFactory.getSupportedTypes());
    }

    /**
     * 从 StreamGraph 构建 Pipeline。
     * <p>
     * 完整流程：
     * 1. 验证 Graph
     * 2. 拓扑排序
     * 3. 创建组件
     * 4. 组装 Pipeline
     * </p>
     *
     * @param graph StreamGraph 定义
     * @return Pipeline 的 Mono
     */
    public Mono<Pipeline<?, ?>> buildFromGraph(StreamGraph graph) {
        log.info("Building pipeline from graph: {}", graph.getGraphId());
        
        return Mono.defer(() -> {
            // 1. 验证 Graph
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
        .subscribeOn(pipelineScheduler)
        .doOnSuccess(p -> log.info("Pipeline built successfully: {}", graph.getGraphName()))
        .doOnError(e -> log.error("Failed to build pipeline from graph: {}", graph.getGraphId(), e));
    }

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

    private List<StreamNode> findOperatorNodes(List<StreamNode> sortedNodes) {
        List<StreamNode> operatorNodes = new ArrayList<>();
        for (StreamNode node : sortedNodes) {
            if (node.getNodeType() == NodeType.OPERATOR) {
                operatorNodes.add(node);
            }
        }
        return operatorNodes;
    }

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

    private Mono<DataSource<?>> createSource(StreamNode sourceNode) {
        log.debug("Creating source from node: {}", sourceNode.getNodeId());
        SourceConfig config = SourceConfigAdapter.from(sourceNode);
        return sourceFactory.createSource(config);
    }

    private Mono<List<Operator<?, ?>>> createOperators(List<StreamNode> operatorNodes) {
        log.debug("Creating {} operators", operatorNodes.size());
        
        if (operatorNodes.isEmpty()) {
            return Mono.just(new ArrayList<>());
        }
        
        return Flux.fromIterable(operatorNodes)
            .concatMap(this::createOperator)
            .collectList();
    }

    private Mono<Operator<?, ?>> createOperator(StreamNode operatorNode) {
        log.debug("Creating operator from node: {}", operatorNode.getNodeId());
        OperatorConfig config = OperatorConfigAdapter.from(operatorNode);
        return operatorFactory.createOperator(config);
    }

    private Mono<DataSink<?>> createSink(StreamNode sinkNode) {
        log.debug("Creating sink from node: {}", sinkNode.getNodeId());
        SinkConfig config = SinkConfigAdapter.from(sinkNode);
        return sinkFactory.createSink(config);
    }

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
}
