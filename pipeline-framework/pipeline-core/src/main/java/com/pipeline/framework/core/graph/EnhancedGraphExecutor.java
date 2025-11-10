package com.pipeline.framework.core.graph;

import com.pipeline.framework.api.graph.*;
import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 增强的图执行器。
 * <p>
 * 使用策略模式替代 switch case，通过 NodeExecutorRegistry 获取对应的执行器。
 * 完全消除了硬编码的条件判断。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class EnhancedGraphExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(EnhancedGraphExecutor.class);
    
    private final NodeExecutorRegistry executorRegistry;

    public EnhancedGraphExecutor(NodeExecutorRegistry executorRegistry) {
        this.executorRegistry = executorRegistry;
        log.info("EnhancedGraphExecutor initialized with {} executors", 
            executorRegistry.getSupportedTypes().size());
    }

    /**
     * 执行整个图。
     * <p>
     * 流程：
     * 1. 验证图的有效性
     * 2. 创建执行上下文
     * 3. 拓扑排序获取执行顺序
     * 4. 使用策略模式构建每个节点的 Flux
     * 5. 并行执行所有 Sink 分支
     * </p>
     *
     * @param graph     StreamGraph
     * @param sources   Source 组件映射
     * @param operators Operator 组件映射
     * @param sinks     Sink 组件映射
     * @return 执行完成的 Mono
     */
    public Mono<Void> execute(StreamGraph graph,
                             Map<String, DataSource<?>> sources,
                             Map<String, Operator<?, ?>> operators,
                             Map<String, DataSink<?>> sinks) {
        log.info("Starting enhanced graph execution: {}", graph.getGraphId());
        
        return Mono.defer(() -> {
            // 1. 验证图
            if (!graph.validate()) {
                return Mono.error(new IllegalStateException("Invalid graph structure"));
            }
            
            // 2. 创建执行上下文
            NodeExecutionContext context = new DefaultNodeExecutionContext(
                graph, sources, operators, sinks
            );
            
            // 3. 拓扑排序
            List<StreamNode> sortedNodes = graph.topologicalSort();
            log.debug("Graph has {} nodes in topological order", sortedNodes.size());
            
            // 4. 按拓扑顺序构建所有节点的 Flux
            buildAllNodes(sortedNodes, context);
            
            // 5. 执行所有 Sink 分支
            List<StreamNode> sinkNodes = graph.getSinkNodes();
            List<Mono<Void>> sinkExecutions = new ArrayList<>();
            
            for (StreamNode sinkNode : sinkNodes) {
                Mono<Void> execution = executeSinkPipeline(sinkNode, context, sinks);
                sinkExecutions.add(execution);
            }
            
            // 并行执行所有 Sink
            return Mono.when(sinkExecutions)
                .doOnSuccess(v -> log.info("Graph execution completed: {}", graph.getGraphId()))
                .doOnError(e -> log.error("Graph execution failed: {}", graph.getGraphId(), e));
        });
    }

    /**
     * 构建所有节点的 Flux。
     * <p>
     * 核心方法：使用策略模式，无 switch case！
     * </p>
     */
    private void buildAllNodes(List<StreamNode> sortedNodes, NodeExecutionContext context) {
        for (StreamNode node : sortedNodes) {
            // 获取对应类型的执行器（策略模式）
            NodeExecutor<Object> executor = executorRegistry.getExecutor(node.getNodeType());
            
            // 构建 Flux（执行器自动处理缓存）
            executor.buildFlux(node, context);
            
            log.debug("Built flux for node: {} (type: {})", 
                node.getNodeId(), node.getNodeType());
        }
    }

    /**
     * 执行 Sink Pipeline。
     */
    @SuppressWarnings("unchecked")
    private Mono<Void> executeSinkPipeline(StreamNode sinkNode,
                                           NodeExecutionContext context,
                                           Map<String, DataSink<?>> sinks) {
        log.debug("Executing sink pipeline: {}", sinkNode.getNodeId());
        
        // 从上下文获取 Sink 的输入数据流
        Flux<Object> dataFlow = context.getCachedFlux(sinkNode.getNodeId())
            .orElseThrow(() -> new IllegalStateException(
                "Flux not found for sink node: " + sinkNode.getNodeId()));
        
        // 获取 Sink 组件
        DataSink<Object> sink = (DataSink<Object>) sinks.get(sinkNode.getNodeId());
        if (sink == null) {
            return Mono.error(new IllegalStateException(
                "Sink not found for node: " + sinkNode.getNodeId()));
        }
        
        // 写入 Sink
        return sink.write(dataFlow)
            .doOnSuccess(v -> log.info("Sink pipeline completed: {}", sinkNode.getNodeId()))
            .doOnError(e -> log.error("Sink pipeline failed: {}", sinkNode.getNodeId(), e));
    }
}
