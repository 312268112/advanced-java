package com.pipeline.framework.api.graph;

import reactor.core.publisher.Flux;

/**
 * 节点执行器接口。
 * <p>
 * 使用策略模式，为不同类型的节点提供不同的执行策略。
 * 替代 switch case 的设计。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface NodeExecutor<T> {

    /**
     * 构建节点的数据流。
     *
     * @param node    当前节点
     * @param context 执行上下文
     * @return 数据流
     */
    Flux<T> buildFlux(StreamNode node, NodeExecutionContext context);

    /**
     * 获取支持的节点类型。
     *
     * @return 节点类型
     */
    NodeType getSupportedNodeType();

    /**
     * 获取执行器优先级。
     * <p>
     * 数值越小优先级越高，默认为 0。
     * </p>
     *
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }
}
