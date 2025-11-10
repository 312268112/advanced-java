package com.pipeline.framework.api.sink;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * 数据输出接口，所有Sink实现必须实现此接口。
 * <p>
 * DataSink负责将处理后的数据写入外部系统。
 * 支持批量写入以提高效率。
 * </p>
 *
 * @param <T> 输入数据类型
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface DataSink<T> {

    /**
     * 写入数据。
     *
     * @param dataStream 数据流
     * @return 完成信号
     */
    Mono<Void> write(Flux<T> dataStream);

    /**
     * 获取Sink配置。
     *
     * @return 配置对象
     */
    SinkConfig getConfig();

    /**
     * 判断是否支持批量写入。
     *
     * @return true如果支持批量写入，否则返回false
     */
    boolean supportsBatch();

    /**
     * 判断是否支持事务。
     *
     * @return true如果支持事务，否则返回false
     */
    boolean supportsTransaction();

    /**
     * 启动Sink。
     *
     * @throws SinkException 如果启动失败
     */
    void start() throws SinkException;

    /**
     * 停止Sink。
     */
    void stop();

    /**
     * 获取Sink名称。
     *
     * @return Sink名称
     */
    String getName();

    /**
     * 判断Sink是否正在运行。
     *
     * @return true如果正在运行，否则返回false
     */
    boolean isRunning();
}
