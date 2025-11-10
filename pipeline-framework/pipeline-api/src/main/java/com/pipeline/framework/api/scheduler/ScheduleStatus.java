package com.pipeline.framework.api.scheduler;

import java.time.Instant;

/**
 * 调度状态接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ScheduleStatus {

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();

    /**
     * 是否已调度。
     *
     * @return true如果已调度
     */
    boolean isScheduled();

    /**
     * 是否已暂停。
     *
     * @return true如果已暂停
     */
    boolean isPaused();

    /**
     * 获取下次执行时间。
     *
     * @return 下次执行时间
     */
    Instant getNextExecutionTime();

    /**
     * 获取上次执行时间。
     *
     * @return 上次执行时间
     */
    Instant getLastExecutionTime();

    /**
     * 获取总执行次数。
     *
     * @return 总执行次数
     */
    long getExecutionCount();

    /**
     * 获取失败次数。
     *
     * @return 失败次数
     */
    long getFailureCount();
}
