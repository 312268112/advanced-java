package com.pipeline.framework.api.scheduler;

import java.time.Instant;

/**
 * 调度结果接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ScheduleResult {

    /**
     * 获取调度ID。
     *
     * @return 调度ID
     */
    String getScheduleId();

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();

    /**
     * 是否调度成功。
     *
     * @return true如果成功
     */
    boolean isSuccess();

    /**
     * 获取调度时间。
     *
     * @return 调度时间
     */
    Instant getScheduleTime();

    /**
     * 获取下次执行时间。
     *
     * @return 下次执行时间
     */
    Instant getNextExecutionTime();

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    String getErrorMessage();
}
