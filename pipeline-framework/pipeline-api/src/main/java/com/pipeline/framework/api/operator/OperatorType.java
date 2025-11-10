package com.pipeline.framework.api.operator;

/**
 * 算子类型枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum OperatorType {
    /**
     * 映射转换（Map）
     */
    MAP,

    /**
     * 过滤（Filter）
     */
    FILTER,

    /**
     * 平铺映射（FlatMap）
     */
    FLAT_MAP,

    /**
     * 聚合（Aggregate）
     */
    AGGREGATE,

    /**
     * 窗口（Window）
     */
    WINDOW,

    /**
     * 连接（Join）
     */
    JOIN,

    /**
     * 去重（Deduplicate）
     */
    DEDUPLICATE,

    /**
     * 排序（Sort）
     */
    SORT,

    /**
     * 分组（GroupBy）
     */
    GROUP_BY,

    /**
     * 限流（Throttle）
     */
    THROTTLE,

    /**
     * 自定义算子
     */
    CUSTOM
}
