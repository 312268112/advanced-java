package com.pipeline.framework.api.executor;

/**
 * 执行指标接口。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface ExecutionMetrics {

    /**
     * 获取读取速率（记录/秒）。
     *
     * @return 读取速率
     */
    double getRecordsReadRate();

    /**
     * 获取写入速率（记录/秒）。
     *
     * @return 写入速率
     */
    double getRecordsWriteRate();

    /**
     * 获取处理延迟（毫秒）。
     *
     * @return 处理延迟
     */
    long getProcessingLatencyMs();

    /**
     * 获取背压次数。
     *
     * @return 背压次数
     */
    int getBackpressureCount();

    /**
     * 获取错误次数。
     *
     * @return 错误次数
     */
    int getErrorCount();
}
