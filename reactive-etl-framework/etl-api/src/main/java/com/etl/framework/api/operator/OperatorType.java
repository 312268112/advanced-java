package com.etl.framework.api.operator;

/**
 * 算子类型枚举。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public enum OperatorType {
    /**
     * 映射转换（一对一）
     */
    MAP,

    /**
     * 过滤
     */
    FILTER,

    /**
     * 扁平映射（一对多）
     */
    FLATMAP,

    /**
     * 聚合
     */
    AGGREGATE,

    /**
     * 窗口
     */
    WINDOW,

    /**
     * 关联
     */
    JOIN,

    /**
     * 去重
     */
    DEDUPLICATE,

    /**
     * 自定义算子
     */
    CUSTOM
}
