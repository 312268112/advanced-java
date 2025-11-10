package com.etl.framework.api.scheduler;

/**
 * 调度类型枚举。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public enum ScheduleType {
    /**
     * 立即执行
     */
    IMMEDIATE,

    /**
     * 定时调度（Cron）
     */
    CRON,

    /**
     * 手动触发
     */
    MANUAL
}
