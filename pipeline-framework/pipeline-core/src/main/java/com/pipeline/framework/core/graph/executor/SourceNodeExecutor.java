package com.pipeline.framework.core.graph.executor;

import com.pipeline.framework.api.graph.NodeExecutionContext;
import com.pipeline.framework.api.graph.NodeType;
import com.pipeline.framework.api.graph.StreamNode;
import com.pipeline.framework.api.source.DataSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Source 节点执行器。
 * <p>
 * 处理 SOURCE 类型的节点，从 DataSource 读取数据。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class SourceNodeExecutor extends AbstractNodeExecutor<Object> {

    @Override
    protected Flux<Object> doBuildFlux(StreamNode node, NodeExecutionContext context) {
        DataSource<Object> source = context.getSource(node.getNodeId())
            .orElseThrow(() -> new IllegalStateException(
                "Source not found for node: " + node.getNodeId()));
        
        log.info("Building flux for source: {} (type: {})", 
            source.getName(), source.getType());
        
        return source.read()
            .doOnSubscribe(s -> log.info("Source started: {}", node.getNodeId()))
            .doOnNext(data -> log.trace("Source produced: {}", data))
            .doOnComplete(() -> log.info("Source completed: {}", node.getNodeId()))
            .doOnError(e -> log.error("Source error: {}", node.getNodeId(), e))
            .cast(Object.class);
    }

    @Override
    public NodeType getSupportedNodeType() {
        return NodeType.SOURCE;
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
