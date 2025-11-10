package com.pipeline.framework.core.graph.executor;

import com.pipeline.framework.api.graph.NodeExecutionContext;
import com.pipeline.framework.api.graph.NodeExecutor;
import com.pipeline.framework.api.graph.StreamNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * 节点执行器抽象基类。
 * <p>
 * 提供通用的缓存逻辑和日志记录。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public abstract class AbstractNodeExecutor<T> implements NodeExecutor<T> {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Flux<T> buildFlux(StreamNode node, NodeExecutionContext context) {
        // 检查缓存
        Optional<Flux<T>> cachedFlux = context.getCachedFlux(node.getNodeId());
        if (cachedFlux.isPresent()) {
            log.debug("Using cached flux for node: {}", node.getNodeId());
            return cachedFlux.get();
        }
        
        // 构建新的 Flux
        log.debug("Building new flux for node: {} (type: {})", 
            node.getNodeId(), getSupportedNodeType());
        
        Flux<T> flux = doBuildFlux(node, context);
        
        // 缓存结果
        context.cacheFlux(node.getNodeId(), flux);
        
        return flux;
    }

    /**
     * 子类实现具体的构建逻辑。
     *
     * @param node    节点
     * @param context 上下文
     * @return 数据流
     */
    protected abstract Flux<T> doBuildFlux(StreamNode node, NodeExecutionContext context);
}
