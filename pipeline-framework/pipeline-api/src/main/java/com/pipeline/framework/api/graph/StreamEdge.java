package com.pipeline.framework.api.graph;

/**
 * 流边接口。
 * <p>
 * 表示流图中节点之间的连接关系。
 * </p>
 *
 * @author Pipeline Framework Team
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
     * 获取分区策略。
     *
     * @return 分区策略
     */
    PartitionStrategy getPartitionStrategy();

    /**
     * 获取选择器（用于条件路由）。
     *
     * @return 选择器表达式
     */
    String getSelector();
}
