package com.pipeline.framework.api.graph;

/**
 * 分区策略枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum PartitionStrategy {
    /**
     * 轮询
     */
    ROUND_ROBIN,

    /**
     * 随机
     */
    RANDOM,

    /**
     * 按键分区
     */
    KEY_BY,

    /**
     * 广播
     */
    BROADCAST,

    /**
     * 重平衡
     */
    REBALANCE,

    /**
     * 转发（无分区）
     */
    FORWARD
}
