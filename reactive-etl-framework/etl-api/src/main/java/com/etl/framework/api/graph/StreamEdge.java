package com.etl.framework.api.graph;

/**
 * 流图边，描述节点之间的数据流向。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface StreamEdge {

    /**
     * 获取边ID。
     *
     * @return 边ID
     */
    String getEdgeId();

    /**
     * 获取源节点ID。
     *
     * @return 源节点ID
     */
    String getSourceNodeId();

    /**
     * 获取目标节点ID。
     *
     * @return 目标节点ID
     */
    String getTargetNodeId();

    /**
     * 获取边标签（可选）。
     *
     * @return 边标签
     */
    String getLabel();
}
