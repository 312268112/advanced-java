package com.pipeline.framework.scheduler;

import java.time.Instant;

/**
 * 调度计划接口。
 * <p>
 * 定义任务的调度计划。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Schedule {

    /**
     * 获取调度计划ID。
     *
     * @return 调度计划ID
     */
    String getScheduleId();

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();

    /**
     * 获取调度类型。
     *
     * @return 调度类型
     */
    ScheduleType getType();

    /**
     * 获取Cron表达式（针对CRON类型）。
     *
     * @return Cron表达式
     */
    String getCronExpression();

    /**
     * 获取下次执行时间。
     *
     * @return 下次执行时间
     */
    Instant getNextExecutionTime();

    /**
     * 判断调度计划是否启用。
     *
     * @return true如果启用
     */
    boolean isEnabled();
}
