package com.pipeline.framework.api.graph;

import java.util.List;

/**
 * 流图接口。
 * <p>
 * 表示数据处理的DAG（有向无环图）。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface StreamGraph {

    /**
     * 获取图ID。
     *
     * @return 图ID
     */
    String getGraphId();

    /**
     * 获取图名称。
     *
     * @return 图名称
     */
    String getGraphName();

    /**
     * 获取所有节点。
     *
     * @return 节点列表
     */
    List<StreamNode> getNodes();

    /**
     * 获取所有边。
     *
     * @return 边列表
     */
    List<StreamEdge> getEdges();

    /**
     * 根据ID获取节点。
     *
     * @param nodeId 节点ID
     * @return 节点对象
     */
    StreamNode getNode(String nodeId);

    /**
     * 获取源节点列表。
     *
     * @return 源节点列表
     */
    List<StreamNode> getSourceNodes();

    /**
     * 获取Sink节点列表。
     *
     * @return Sink节点列表
     */
    List<StreamNode> getSinkNodes();

    /**
     * 获取节点的上游节点。
     *
     * @param nodeId 节点ID
     * @return 上游节点列表
     */
    List<StreamNode> getUpstreamNodes(String nodeId);

    /**
     * 获取节点的下游节点。
     *
     * @param nodeId 节点ID
     * @return 下游节点列表
     */
    List<StreamNode> getDownstreamNodes(String nodeId);

    /**
     * 验证图的有效性。
     * <p>
     * 检查是否存在环、孤立节点等问题。
     * </p>
     *
     * @return true如果图有效
     */
    boolean validate();

    /**
     * 获取拓扑排序后的节点列表。
     *
     * @return 拓扑排序后的节点列表
     */
    List<StreamNode> topologicalSort();
}
