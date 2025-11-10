package com.pipeline.framework.api.executor;

import java.time.Instant;

/**
 * 执行指标接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ExecutionMetrics {

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();

    /**
     * 获取实例ID。
     *
     * @return 实例ID
     */
    String getInstanceId();

    /**
     * 获取指标时间戳。
     *
     * @return 指标时间戳
     */
    Instant getTimestamp();

    /**
     * 获取总读取记录数。
     *
     * @return 总读取记录数
     */
    long getRecordsRead();

    /**
     * 获取总处理记录数。
     *
     * @return 总处理记录数
     */
    long getRecordsProcessed();

    /**
     * 获取总写入记录数。
     *
     * @return 总写入记录数
     */
    long getRecordsWritten();

    /**
     * 获取读取速率（记录/秒）。
     *
     * @return 读取速率
     */
    double getReadRate();

    /**
     * 获取写入速率（记录/秒）。
     *
     * @return 写入速率
     */
    double getWriteRate();

    /**
     * 获取处理延迟（毫秒）。
     *
     * @return 处理延迟
     */
    long getLatency();

    /**
     * 获取背压次数。
     *
     * @return 背压次数
     */
    long getBackpressureCount();

    /**
     * 获取错误次数。
     *
     * @return 错误次数
     */
    long getErrorCount();

    /**
     * 获取检查点次数。
     *
     * @return 检查点次数
     */
    long getCheckpointCount();

    /**
     * 获取重启次数。
     *
     * @return 重启次数
     */
    long getRestartCount();

    /**
     * 获取CPU使用率（百分比）。
     *
     * @return CPU使用率
     */
    double getCpuUsage();

    /**
     * 获取内存使用量（字节）。
     *
     * @return 内存使用量
     */
    long getMemoryUsed();

    /**
     * 获取线程数。
     *
     * @return 线程数
     */
    int getThreadCount();
}
