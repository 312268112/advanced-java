package com.pipeline.framework.checkpoint;

/**
 * 检查点类型枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum CheckpointType {
    /**
     * 自动检查点
     */
    AUTO,

    /**
     * 手动检查点
     */
    MANUAL,

    /**
     * 保存点（用于升级、迁移）
     */
    SAVEPOINT
}
