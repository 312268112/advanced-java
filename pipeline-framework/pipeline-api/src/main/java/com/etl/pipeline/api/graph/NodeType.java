package com.pipeline.framework.api.graph;

/**
 * 节点类型枚举。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public enum NodeType {
    /**
     * 数据源节点
     */
    SOURCE,

    /**
     * 算子节点
     */
    OPERATOR,

    /**
     * 输出节点
     */
    SINK
}
