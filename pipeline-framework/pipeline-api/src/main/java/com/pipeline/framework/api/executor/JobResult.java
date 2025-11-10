package com.pipeline.framework.api.executor;

import java.time.Duration;
import java.time.Instant;

/**
 * 任务执行结果接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface JobResult {

    /**
     * 获取任务实例ID。
     *
     * @return 任务实例ID
     */
    String getInstanceId();

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();

    /**
     * 是否执行成功。
     *
     * @return true如果成功
     */
    boolean isSuccess();

    /**
     * 获取执行状态。
     *
     * @return 执行状态
     */
    ExecutionStatus getStatus();

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
     * 获取处理记录数。
     *
     * @return 处理记录数
     */
    long getProcessedRecords();

    /**
     * 获取失败记录数。
     *
     * @return 失败记录数
     */
    long getFailedRecords();

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    String getErrorMessage();

    /**
     * 获取异常。
     *
     * @return 异常对象
     */
    Throwable getException();

    /**
     * 获取执行指标。
     *
     * @return 执行指标
     */
    ExecutionMetrics getMetrics();
}
