package com.etl.framework.api.source;

import reactor.core.publisher.Flux;

/**
 * 数据源接口，所有Source实现必须实现此接口。
 * <p>
 * DataSource负责从外部系统读取数据并转换为响应式流。
 * 实现类必须支持背压机制，避免内存溢出。
 * </p>
 *
 * @param <T> 输出数据类型
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface DataSource<T> {

    /**
     * 获取数据流。
     * <p>
     * 此方法返回一个响应式流，数据源将持续发送数据直到：
     * 1. 数据源数据读取完毕（有界数据源）
     * 2. 显式调用stop()方法
     * 3. 发生不可恢复的错误
     * </p>
     *
     * @return 响应式数据流
     */
    Flux<T> getDataStream();

    /**
     * 获取数据源类型。
     *
     * @return 数据源类型
     */
    SourceType getSourceType();

    /**
     * 获取数据源配置。
     *
     * @return 配置对象
     */
    SourceConfig getConfig();

    /**
     * 启动数据源。
     * <p>
     * 初始化连接、资源等。此方法应该是幂等的。
     * </p>
     *
     * @throws SourceException 如果启动失败
     */
    void start() throws SourceException;

    /**
     * 停止数据源。
     * <p>
     * 释放所有资源，关闭连接。此方法应该是幂等的。
     * </p>
     */
    void stop();

    /**
     * 获取数据源名称。
     *
     * @return 数据源名称
     */
    String getName();

    /**
     * 判断数据源是否正在运行。
     *
     * @return true如果正在运行，否则返回false
     */
    boolean isRunning();
}
