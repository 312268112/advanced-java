package com.etl.framework.api.executor;

/**
 * 执行状态枚举。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public enum ExecutionStatus {
    /**
     * 运行中
     */
    RUNNING,

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
