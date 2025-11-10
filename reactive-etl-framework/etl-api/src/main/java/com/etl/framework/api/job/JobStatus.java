package com.etl.framework.api.job;

/**
 * 任务状态枚举。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public enum JobStatus {
    /**
     * 已创建
     */
    CREATED,

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
     * 已完成
     */
    COMPLETED,

    /**
     * 失败
     */
    FAILED,

    /**
     * 已取消
     */
    CANCELLED
}
