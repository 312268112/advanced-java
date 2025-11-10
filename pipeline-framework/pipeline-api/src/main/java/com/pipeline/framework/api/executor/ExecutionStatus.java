package com.pipeline.framework.api.executor;

/**
 * 执行状态枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum ExecutionStatus {
    /**
     * 已提交
     */
    SUBMITTED,

    /**
     * 初始化中
     */
    INITIALIZING,

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
    CANCELLED,

    /**
     * 重启中
     */
    RESTARTING
}
