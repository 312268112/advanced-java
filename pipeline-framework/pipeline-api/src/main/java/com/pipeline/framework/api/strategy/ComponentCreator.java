package com.pipeline.framework.api.strategy;

import reactor.core.publisher.Mono;

/**
 * 组件创建策略接口。
 * <p>
 * 使用策略模式替代 switch case，每个类型的组件都有自己的创建器。
 * </p>
 *
 * @param <T> 组件类型
 * @param <C> 配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ComponentCreator<T, C> {

    /**
     * 创建组件实例。
     *
     * @param config 配置信息
     * @return 组件实例的 Mono
     */
    Mono<T> create(C config);

    /**
     * 获取支持的类型标识。
     *
     * @return 类型标识（如 "kafka", "mysql", "filter" 等）
     */
    String getType();

    /**
     * 获取创建器优先级。
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
