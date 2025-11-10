package com.pipeline.framework.core.graph.executor;

import com.pipeline.framework.api.graph.NodeExecutionContext;
import com.pipeline.framework.api.graph.NodeType;
import com.pipeline.framework.api.graph.StreamGraph;
import com.pipeline.framework.api.graph.StreamNode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Sink 节点执行器。
 * <p>
 * 处理 SINK 类型的节点，获取上游数据流。
 * 实际的写入操作由 GraphExecutor 统一处理。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class SinkNodeExecutor extends AbstractNodeExecutor<Object> {

    @Override
    protected Flux<Object> doBuildFlux(StreamNode node, NodeExecutionContext context) {
        // Sink 节点只需要获取上游数据流
        List<String> upstreamIds = node.getUpstream();
        
        if (upstreamIds == null || upstreamIds.isEmpty()) {
            throw new IllegalStateException(
                "Sink node must have upstream: " + node.getNodeId());
        }
        
        log.debug("Building upstream flux for sink: {}", node.getNodeId());
        
        StreamGraph graph = context.getGraph();
        String upstreamId = upstreamIds.get(0);  // Sink 通常只有一个上游
        StreamNode upstreamNode = graph.getNode(upstreamId);
        
        if (upstreamNode == null) {
            throw new IllegalStateException("Upstream node not found: " + upstreamId);
        }
        
        // 从缓存获取上游 Flux
        return context.getCachedFlux(upstreamNode.getNodeId())
            .orElseThrow(() -> new IllegalStateException(
                "Upstream flux not available for sink node: " + node.getNodeId()));
    }

    @Override
    public NodeType getSupportedNodeType() {
        return NodeType.SINK;
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
