package com.pipeline.framework.api.component;

import reactor.core.publisher.Mono;

/**
 * 生命周期感知接口。
 * <p>
 * 提供组件启动、停止等生命周期管理能力。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface LifecycleAware {

    /**
     * 启动组件。
     *
     * @return 启动完成的 Mono
     */
    Mono<Void> start();

    /**
     * 停止组件。
     *
     * @return 停止完成的 Mono
     */
    Mono<Void> stop();

    /**
     * 是否正在运行。
     *
     * @return 是否运行中
     */
    default boolean isRunning() {
        return false;
    }
}
