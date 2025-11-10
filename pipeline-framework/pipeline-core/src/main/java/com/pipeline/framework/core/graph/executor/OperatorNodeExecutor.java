package com.pipeline.framework.core.graph.executor;

import com.pipeline.framework.api.graph.NodeExecutionContext;
import com.pipeline.framework.api.graph.NodeType;
import com.pipeline.framework.api.graph.StreamGraph;
import com.pipeline.framework.api.graph.StreamNode;
import com.pipeline.framework.api.operator.Operator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Operator 节点执行器。
 * <p>
 * 处理 OPERATOR 类型的节点，应用算子转换。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class OperatorNodeExecutor extends AbstractNodeExecutor<Object> {

    @Override
    protected Flux<Object> doBuildFlux(StreamNode node, NodeExecutionContext context) {
        // 1. 获取上游数据流
        Flux<Object> upstreamFlux = buildUpstreamFlux(node, context);
        
        // 2. 获取并应用 Operator
        Operator<Object, Object> operator = context.getOperator(node.getNodeId())
            .orElseThrow(() -> new IllegalStateException(
                "Operator not found for node: " + node.getNodeId()));
        
        log.info("Applying operator: {} (type: {})", 
            operator.getName(), operator.getType());
        
        return operator.apply(upstreamFlux)
            .doOnSubscribe(s -> log.debug("Operator started: {}", node.getNodeId()))
            .doOnNext(data -> log.trace("Operator produced: {}", data))
            .doOnComplete(() -> log.debug("Operator completed: {}", node.getNodeId()))
            .doOnError(e -> log.error("Operator error: {}", node.getNodeId(), e));
    }

    /**
     * 构建上游数据流。
     * <p>
     * 如果有多个上游，则合并所有上游的数据流。
     * </p>
     */
    private Flux<Object> buildUpstreamFlux(StreamNode node, NodeExecutionContext context) {
        List<String> upstreamIds = node.getUpstream();
        
        if (upstreamIds == null || upstreamIds.isEmpty()) {
            throw new IllegalStateException(
                "Operator node must have upstream: " + node.getNodeId());
        }
        
        if (upstreamIds.size() == 1) {
            // 单个上游
            return buildSingleUpstream(upstreamIds.get(0), context);
        } else {
            // 多个上游，合并
            return buildMergedUpstream(upstreamIds, context);
        }
    }

    /**
     * 构建单个上游流。
     */
    private Flux<Object> buildSingleUpstream(String upstreamId, NodeExecutionContext context) {
        StreamGraph graph = context.getGraph();
        StreamNode upstreamNode = graph.getNode(upstreamId);
        
        if (upstreamNode == null) {
            throw new IllegalStateException("Upstream node not found: " + upstreamId);
        }
        
        // 递归构建上游节点的 Flux
        return buildUpstreamNodeFlux(upstreamNode, context);
    }

    /**
     * 构建合并的上游流。
     */
    private Flux<Object> buildMergedUpstream(List<String> upstreamIds, NodeExecutionContext context) {
        log.debug("Merging {} upstream flows", upstreamIds.size());
        
        StreamGraph graph = context.getGraph();
        List<Flux<Object>> upstreamFluxes = new ArrayList<>();
        
        for (String upstreamId : upstreamIds) {
            StreamNode upstreamNode = graph.getNode(upstreamId);
            if (upstreamNode == null) {
                throw new IllegalStateException("Upstream node not found: " + upstreamId);
            }
            upstreamFluxes.add(buildUpstreamNodeFlux(upstreamNode, context));
        }
        
        return Flux.merge(upstreamFluxes);
    }

    /**
     * 根据节点类型构建上游 Flux。
     * <p>
     * 这里使用策略模式，委托给对应的 NodeExecutor。
     * </p>
     */
    private Flux<Object> buildUpstreamNodeFlux(StreamNode upstreamNode, NodeExecutionContext context) {
        // 从上下文获取缓存或者需要通过 NodeExecutorRegistry 获取对应的执行器
        // 这里简化处理，直接从缓存获取或抛出异常
        return context.getCachedFlux(upstreamNode.getNodeId())
            .orElseThrow(() -> new IllegalStateException(
                "Upstream flux not available for node: " + upstreamNode.getNodeId() +
                ". Make sure to build nodes in topological order."));
    }

    @Override
    public NodeType getSupportedNodeType() {
        return NodeType.OPERATOR;
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
