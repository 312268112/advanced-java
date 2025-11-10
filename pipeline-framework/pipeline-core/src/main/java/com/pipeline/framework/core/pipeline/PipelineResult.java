package com.pipeline.framework.core.pipeline;

import java.time.Duration;
import java.time.Instant;

/**
 * Pipeline执行结果接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface PipelineResult {

    /**
     * 是否执行成功。
     *
     * @return true如果成功
     */
    boolean isSuccess();

    /**
     * 获取开始时间。
     *
     * @return 开始时间
     */
    Instant getStartTime();

    /**
     * 获取结束时间。
     *
     * @return 结束时间
     */
    Instant getEndTime();

    /**
     * 获取执行时长。
     *
     * @return 执行时长
     */
    Duration getDuration();

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

    /**
     * 获取错误信息。
     *
     * @return 错误信息，如果成功则返回null
     */
    String getErrorMessage();

    /**
     * 获取异常。
     *
     * @return 异常对象，如果成功则返回null
     */
    Throwable getException();
}
