package com.pipeline.framework.scheduler;

/**
 * 调度类型枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum ScheduleType {
    /**
     * 立即执行一次
     */
    ONCE,

    /**
     * Cron表达式调度
     */
    CRON,

    /**
     * 固定间隔调度
     */
    FIXED_RATE,

    /**
     * 固定延迟调度
     */
    FIXED_DELAY,

    /**
     * 手动触发
     */
    MANUAL
}
