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
import com.pipeline.framework.core.factory.SpringOperatorFactory;
import com.pipeline.framework.core.factory.SpringSinkFactory;
import com.pipeline.framework.core.factory.SpringSourceFactory;
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
 * 基于 Spring 的 Graph Pipeline 构建器。
 * <p>
 * 核心改进：
 * 1. 使用 Spring 依赖注入，不再手动创建工厂
 * 2. 使用策略模式，不再使用 switch case
 * 3. 使用 Reactor Scheduler 进行线程管理
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class SpringGraphBasedPipelineBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(SpringGraphBasedPipelineBuilder.class);
    
    private final SpringSourceFactory sourceFactory;
    private final SpringSinkFactory sinkFactory;
    private final SpringOperatorFactory operatorFactory;
    private final Scheduler pipelineScheduler;

    /**
     * 构造函数注入所有依赖。
     *
     * @param sourceFactory     Source 工厂
     * @param sinkFactory       Sink 工厂
     * @param operatorFactory   Operator 工厂
     * @param pipelineScheduler Pipeline 调度器
     */
    public SpringGraphBasedPipelineBuilder(
            SpringSourceFactory sourceFactory,
            SpringSinkFactory sinkFactory,
            SpringOperatorFactory operatorFactory,
            @Qualifier("pipelineScheduler") Scheduler pipelineScheduler) {
        this.sourceFactory = sourceFactory;
        this.sinkFactory = sinkFactory;
        this.operatorFactory = operatorFactory;
        this.pipelineScheduler = pipelineScheduler;
        
        log.info("SpringGraphBasedPipelineBuilder initialized");
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
     * 3. 使用 Spring Factory 创建组件
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
            
            // 4. 创建组件（使用 Spring Factory，无 switch case）
            return createSource(sourceNode)
                .flatMap(source -> createOperators(operatorNodes)
                    .flatMap(operators -> createSink(sinkNode)
                        .map(sink -> assemblePipeline(graph, source, operators, sink))));
        })
        .subscribeOn(pipelineScheduler)  // 在 pipeline 调度器上执行
        .doOnSuccess(p -> log.info("Pipeline built successfully: {}", graph.getGraphName()))
        .doOnError(e -> log.error("Failed to build pipeline from graph: {}", graph.getGraphId(), e));
    }

    /**
     * 查找 Source 节点。
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
     * 查找所有 Operator 节点。
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
     * 查找 Sink 节点。
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
     * 创建 Source 实例。
     * <p>
     * 使用 SpringSourceFactory，自动根据类型选择合适的 Creator。
     * 无需 switch case！
     * </p>
     */
    private Mono<DataSource<?>> createSource(StreamNode sourceNode) {
        log.debug("Creating source from node: {}", sourceNode.getNodeId());
        
        SourceConfig config = parseSourceConfig(sourceNode);
        return sourceFactory.createSource(config);
    }

    /**
     * 创建所有 Operator 实例。
     * <p>
     * 使用 Flux.concat 串行创建，保证顺序。
     * </p>
     */
    private Mono<List<Operator<?, ?>>> createOperators(List<StreamNode> operatorNodes) {
        log.debug("Creating {} operators", operatorNodes.size());
        
        if (operatorNodes.isEmpty()) {
            return Mono.just(new ArrayList<>());
        }
        
        // 使用 Flux 串行创建 Operator
        return Flux.fromIterable(operatorNodes)
            .concatMap(this::createOperator)  // 保证顺序
            .collectList();
    }

    /**
     * 创建单个 Operator 实例。
     * <p>
     * 使用 SpringOperatorFactory，无需 switch case！
     * </p>
     */
    private Mono<Operator<?, ?>> createOperator(StreamNode operatorNode) {
        log.debug("Creating operator from node: {}", operatorNode.getNodeId());
        
        OperatorConfig config = parseOperatorConfig(operatorNode);
        return operatorFactory.createOperator(config);
    }

    /**
     * 创建 Sink 实例。
     * <p>
     * 使用 SpringSinkFactory，无需 switch case！
     * </p>
     */
    private Mono<DataSink<?>> createSink(StreamNode sinkNode) {
        log.debug("Creating sink from node: {}", sinkNode.getNodeId());
        
        SinkConfig config = parseSinkConfig(sinkNode);
        return sinkFactory.createSink(config);
    }

    /**
     * 组装成完整的 Pipeline。
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
     * 解析 Source 配置。
     */
    private SourceConfig parseSourceConfig(StreamNode node) {
        return new SimpleSourceConfig(node.getConfig());
    }

    /**
     * 解析 Operator 配置。
     */
    private OperatorConfig parseOperatorConfig(StreamNode node) {
        String operatorType = node.getOperatorType();
        return new SimpleOperatorConfig(
            OperatorType.valueOf(operatorType.toUpperCase()),
            node.getConfig()
        );
    }

    /**
     * 解析 Sink 配置。
     */
    private SinkConfig parseSinkConfig(StreamNode node) {
        return new SimpleSinkConfig(node.getConfig());
    }
}
