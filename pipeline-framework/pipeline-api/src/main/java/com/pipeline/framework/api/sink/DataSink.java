package com.pipeline.framework.api.sink;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 数据输出接口。
 * <p>
 * 负责将处理后的数据写入目标系统。
 * 支持响应式流和背压控制。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface DataSink<T> {

    /**
     * 写入数据流。
     * <p>
     * 接收数据流并写入目标系统，返回写入结果。
     * 支持背压，当目标系统处理不过来时会减慢上游速度。
     * </p>
     *
     * @param data 数据流
     * @return 写入完成信号
     */
    Mono<Void> write(Flux<T> data);

    /**
     * 批量写入。
     * <p>
     * 按批次写入数据，提高写入效率。
     * </p>
     *
     * @param data      数据流
     * @param batchSize 批次大小
     * @return 写入完成信号
     */
    Mono<Void> writeBatch(Flux<T> data, int batchSize);

    /**
     * 启动数据输出。
     *
     * @return 启动完成信号
     */
    Mono<Void> start();

    /**
     * 停止数据输出。
     * <p>
     * 优雅地关闭，确保所有数据都已写入。
     * </p>
     *
     * @return 停止完成信号
     */
    Mono<Void> stop();

    /**
     * 刷新缓冲区。
     * <p>
     * 强制将缓冲区中的数据写入目标系统。
     * </p>
     *
     * @return 刷新完成信号
     */
    Mono<Void> flush();

    /**
     * 获取输出类型。
     *
     * @return 输出类型
     */
    SinkType getType();

    /**
     * 获取输出名称。
     *
     * @return 输出名称
     */
    String getName();

    /**
     * 获取输出配置。
     *
     * @return 输出配置
     */
    SinkConfig getConfig();

    /**
     * 判断是否正在运行。
     *
     * @return true如果正在运行
     */
    boolean isRunning();

    /**
     * 健康检查。
     *
     * @return 健康状态
     */
    Mono<Boolean> healthCheck();
}
