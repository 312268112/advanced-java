package com.pipeline.framework.api.source;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 数据源接口。
 * <p>
 * 使用响应式流方式提供数据，支持背压和非阻塞操作。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface DataSource<T> {

    /**
     * 获取数据流。
     * <p>
     * 返回一个响应式流，支持背压控制。
     * </p>
     *
     * @return 数据流
     */
    Flux<T> read();

    /**
     * 启动数据源。
     * <p>
     * 异步启动数据源，返回Mono表示启动操作的完成。
     * </p>
     *
     * @return 启动完成信号
     */
    Mono<Void> start();

    /**
     * 停止数据源。
     * <p>
     * 优雅地停止数据源，释放资源。
     * </p>
     *
     * @return 停止完成信号
     */
    Mono<Void> stop();

    /**
     * 获取数据源类型。
     *
     * @return 数据源类型
     */
    SourceType getType();

    /**
     * 获取数据源名称。
     *
     * @return 数据源名称
     */
    String getName();

    /**
     * 获取数据源配置。
     *
     * @return 数据源配置
     */
    SourceConfig getConfig();

    /**
     * 判断数据源是否正在运行。
     *
     * @return true如果正在运行
     */
    boolean isRunning();

    /**
     * 健康检查。
     * <p>
     * 异步检查数据源健康状态。
     * </p>
     *
     * @return 健康状态，true表示健康
     */
    Mono<Boolean> healthCheck();
}
