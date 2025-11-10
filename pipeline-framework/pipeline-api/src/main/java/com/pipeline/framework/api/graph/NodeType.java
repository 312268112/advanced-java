package com.pipeline.framework.api.graph;

/**
 * 节点类型枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum NodeType {
    /**
     * 数据源节点
     */
    SOURCE,

    /**
     * 转换算子节点
     */
    OPERATOR,

    /**
     * 数据输出节点
     */
    SINK
}
