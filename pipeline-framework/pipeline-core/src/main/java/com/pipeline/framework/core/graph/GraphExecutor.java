package com.pipeline.framework.core.graph;

import com.pipeline.framework.api.graph.StreamGraph;
import com.pipeline.framework.api.graph.StreamNode;
import com.pipeline.framework.api.graph.NodeType;
import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图执行器实现。
 * <p>
 * 负责将StreamGraph转换为可执行的响应式流Pipeline。
 * 核心思想：将DAG图转换为Flux的链式操作。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class GraphExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(GraphExecutor.class);
    
    private final StreamGraph graph;
    private final Map<String, DataSource<?>> sources;
    private final Map<String, Operator<?, ?>> operators;
    private final Map<String, DataSink<?>> sinks;
    
    // 缓存节点的Flux
    private final Map<String, Flux<?>> nodeFluxCache = new ConcurrentHashMap<>();

    public GraphExecutor(StreamGraph graph,
                        Map<String, DataSource<?>> sources,
                        Map<String, Operator<?, ?>> operators,
                        Map<String, DataSink<?>> sinks) {
        this.graph = graph;
        this.sources = sources;
        this.operators = operators;
        this.sinks = sinks;
    }

    /**
     * 执行整个图。
     * <p>
     * 1. 拓扑排序获取执行顺序
     * 2. 从Source节点开始构建Flux
     * 3. 依次应用Operator
     * 4. 最后连接到Sink
     * </p>
     *
     * @return 执行完成的Mono
     */
    public Mono<Void> execute() {
        log.info("Starting graph execution: {}", graph.getGraphId());
        
        // 验证图的有效性
        if (!graph.validate()) {
            return Mono.error(new IllegalStateException("Invalid graph structure"));
        }
        
        // 获取拓扑排序后的节点
        List<StreamNode> sortedNodes = graph.topologicalSort();
        
        // 获取所有Sink节点
        List<StreamNode> sinkNodes = graph.getSinkNodes();
        
        // 为每个Sink节点构建并执行流
        List<Mono<Void>> sinkExecutions = new ArrayList<>();
        
        for (StreamNode sinkNode : sinkNodes) {
            Mono<Void> sinkExecution = buildAndExecuteSinkPipeline(sinkNode);
            sinkExecutions.add(sinkExecution);
        }
        
        // 并行执行所有Sink分支
        return Mono.when(sinkExecutions)
            .doOnSuccess(v -> log.info("Graph execution completed: {}", graph.getGraphId()))
            .doOnError(e -> log.error("Graph execution failed: {}", graph.getGraphId(), e));
    }

    /**
     * 为指定的Sink节点构建并执行完整的Pipeline。
     *
     * @param sinkNode Sink节点
     * @return 执行完成的Mono
     */
    private Mono<Void> buildAndExecuteSinkPipeline(StreamNode sinkNode) {
        log.debug("Building pipeline for sink: {}", sinkNode.getNodeId());
        
        // 构建从Source到Sink的Flux
        Flux<?> dataFlow = buildFluxForNode(sinkNode);
        
        // 获取Sink实例
        DataSink<Object> sink = (DataSink<Object>) sinks.get(sinkNode.getNodeId());
        if (sink == null) {
            return Mono.error(new IllegalStateException(
                "Sink not found for node: " + sinkNode.getNodeId()));
        }
        
        // 连接到Sink并执行
        return sink.write((Flux<Object>) dataFlow)
            .doOnSuccess(v -> log.info("Sink pipeline completed: {}", sinkNode.getNodeId()))
            .doOnError(e -> log.error("Sink pipeline failed: {}", sinkNode.getNodeId(), e));
    }

    /**
     * 递归构建指定节点的Flux。
     * <p>
     * 使用缓存避免重复构建同一节点。
     * </p>
     *
     * @param node 目标节点
     * @return 该节点的数据流
     */
    @SuppressWarnings("unchecked")
    private Flux<?> buildFluxForNode(StreamNode node) {
        // 检查缓存
        if (nodeFluxCache.containsKey(node.getNodeId())) {
            return nodeFluxCache.get(node.getNodeId());
        }
        
        Flux<?> flux;
        
        switch (node.getNodeType()) {
            case SOURCE:
                flux = buildSourceFlux(node);
                break;
                
            case OPERATOR:
                flux = buildOperatorFlux(node);
                break;
                
            case SINK:
                // Sink节点从上游获取数据
                flux = buildOperatorFlux(node);
                break;
                
            default:
                throw new IllegalStateException("Unknown node type: " + node.getNodeType());
        }
        
        // 缓存结果
        nodeFluxCache.put(node.getNodeId(), flux);
        return flux;
    }

    /**
     * 构建Source节点的Flux。
     *
     * @param node Source节点
     * @return 数据流
     */
    private Flux<?> buildSourceFlux(StreamNode node) {
        DataSource<?> source = sources.get(node.getNodeId());
        if (source == null) {
            throw new IllegalStateException("Source not found: " + node.getNodeId());
        }
        
        log.debug("Building source flux: {}", node.getNodeId());
        
        return source.read()
            .doOnSubscribe(s -> log.info("Source started: {}", node.getNodeId()))
            .doOnComplete(() -> log.info("Source completed: {}", node.getNodeId()))
            .doOnError(e -> log.error("Source error: {}", node.getNodeId(), e));
    }

    /**
     * 构建Operator节点的Flux。
     * <p>
     * 处理步骤：
     * 1. 获取所有上游节点的Flux
     * 2. 合并上游数据流（如果有多个上游）
     * 3. 应用当前Operator
     * </p>
     *
     * @param node Operator节点
     * @return 数据流
     */
    @SuppressWarnings("unchecked")
    private Flux<?> buildOperatorFlux(StreamNode node) {
        log.debug("Building operator flux: {}", node.getNodeId());
        
        // 获取上游节点
        List<String> upstreamIds = node.getUpstream();
        if (upstreamIds == null || upstreamIds.isEmpty()) {
            throw new IllegalStateException(
                "Operator node must have upstream: " + node.getNodeId());
        }
        
        // 构建上游Flux
        Flux<Object> upstreamFlux;
        if (upstreamIds.size() == 1) {
            // 单个上游
            StreamNode upstreamNode = graph.getNode(upstreamIds.get(0));
            upstreamFlux = (Flux<Object>) buildFluxForNode(upstreamNode);
        } else {
            // 多个上游，需要合并
            List<Flux<?>> upstreamFluxes = new ArrayList<>();
            for (String upstreamId : upstreamIds) {
                StreamNode upstreamNode = graph.getNode(upstreamId);
                upstreamFluxes.add(buildFluxForNode(upstreamNode));
            }
            upstreamFlux = Flux.merge(upstreamFluxes).cast(Object.class);
        }
        
        // 如果是Sink节点，直接返回上游Flux
        if (node.getNodeType() == NodeType.SINK) {
            return upstreamFlux;
        }
        
        // 获取并应用Operator
        Operator<Object, Object> operator = (Operator<Object, Object>) 
            operators.get(node.getNodeId());
        
        if (operator == null) {
            throw new IllegalStateException("Operator not found: " + node.getNodeId());
        }
        
        return operator.apply(upstreamFlux)
            .doOnSubscribe(s -> log.debug("Operator started: {}", node.getNodeId()))
            .doOnComplete(() -> log.debug("Operator completed: {}", node.getNodeId()))
            .doOnError(e -> log.error("Operator error: {}", node.getNodeId(), e));
    }

    /**
     * 停止执行（用于流式任务）。
     *
     * @return 停止完成的Mono
     */
    public Mono<Void> stop() {
        log.info("Stopping graph execution: {}", graph.getGraphId());
        
        // 停止所有Source
        List<Mono<Void>> stopMonos = new ArrayList<>();
        
        for (DataSource<?> source : sources.values()) {
            stopMonos.add(source.stop()
                .doOnSuccess(v -> log.debug("Source stopped: {}", source.getName()))
                .onErrorResume(e -> {
                    log.warn("Error stopping source: {}", source.getName(), e);
                    return Mono.empty();
                }));
        }
        
        // 停止所有Sink
        for (DataSink<?> sink : sinks.values()) {
            stopMonos.add(sink.stop()
                .doOnSuccess(v -> log.debug("Sink stopped: {}", sink.getName()))
                .onErrorResume(e -> {
                    log.warn("Error stopping sink: {}", sink.getName(), e);
                    return Mono.empty();
                }));
        }
        
        return Mono.when(stopMonos)
            .doOnSuccess(v -> log.info("Graph stopped: {}", graph.getGraphId()));
    }
}
