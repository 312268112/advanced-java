package com.pipeline.framework.api.job;

/**
 * 任务类型枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum JobType {
    /**
     * 流式任务（持续运行）
     */
    STREAMING,

    /**
     * 批处理任务（一次性）
     */
    BATCH
}
