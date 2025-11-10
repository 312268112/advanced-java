package com.pipeline.framework.api.executor;

/**
 * 任务执行结果。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface JobResult {

    /**
     * 是否成功。
     *
     * @return true如果成功，否则返回false
     */
    boolean isSuccess();

    /**
     * 获取错误信息。
     *
     * @return 错误信息，如果成功返回null
     */
    String getErrorMessage();

    /**
     * 获取执行时长（毫秒）。
     *
     * @return 执行时长
     */
    long getDurationMs();

    /**
     * 获取读取记录数。
     *
     * @return 读取记录数
     */
    long getRecordsRead();

    /**
     * 获取处理记录数。
     *
     * @return 处理记录数
     */
    long getRecordsProcessed();

    /**
     * 获取写入记录数。
     *
     * @return 写入记录数
     */
    long getRecordsWritten();
}
