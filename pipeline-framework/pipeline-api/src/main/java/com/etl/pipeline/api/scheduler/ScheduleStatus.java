package com.pipeline.framework.api.scheduler;

/**
 * 调度状态枚举。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public enum ScheduleStatus {
    /**
     * 已调度
     */
    SCHEDULED,

    /**
     * 运行中
     */
    RUNNING,

    /**
     * 已暂停
     */
    PAUSED,

    /**
     * 已取消
     */
    CANCELLED
}
