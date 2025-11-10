package com.pipeline.framework.core.runtime;

/**
 * 运行时指标接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface RuntimeMetrics {

    /**
     * 记录读取的记录数。
     *
     * @param count 记录数
     */
    void recordRead(long count);

    /**
     * 记录处理的记录数。
     *
     * @param count 记录数
     */
    void recordProcessed(long count);

    /**
     * 记录写入的记录数。
     *
     * @param count 记录数
     */
    void recordWritten(long count);

    /**
     * 记录过滤的记录数。
     *
     * @param count 记录数
     */
    void recordFiltered(long count);

    /**
     * 记录错误次数。
     */
    void recordError();

    /**
     * 记录背压事件。
     */
    void recordBackpressure();

    /**
     * 获取总读取记录数。
     *
     * @return 读取记录数
     */
    long getTotalRead();

    /**
     * 获取总处理记录数。
     *
     * @return 处理记录数
     */
    long getTotalProcessed();

    /**
     * 获取总写入记录数。
     *
     * @return 写入记录数
     */
    long getTotalWritten();
}
