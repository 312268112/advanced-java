package com.pipeline.framework.api.job;

/**
 * 重启策略枚举。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public enum RestartStrategy {
    /**
     * 不重启
     */
    NO_RESTART,

    /**
     * 固定延迟重启
     */
    FIXED_DELAY,

    /**
     * 指数退避重启
     */
    EXPONENTIAL_BACKOFF
}
