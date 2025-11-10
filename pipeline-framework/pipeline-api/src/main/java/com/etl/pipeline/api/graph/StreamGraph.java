package com.pipeline.framework.api.graph;

import java.util.List;

/**
 * 流图，描述数据流的逻辑结构。
 * <p>
 * StreamGraph是用户定义的逻辑执行图，描述了Source → Operators → Sink的数据流向。
 * </p>
 *
 * @author ETL Framework Team
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
     * 根据节点ID获取节点。
     *
     * @param nodeId 节点ID
     * @return 节点对象，如果不存在返回null
     */
    StreamNode getNode(String nodeId);

    /**
     * 添加节点。
     *
     * @param node 节点对象
     */
    void addNode(StreamNode node);

    /**
     * 添加边。
     *
     * @param edge 边对象
     */
    void addEdge(StreamEdge edge);

    /**
     * 验证图结构是否合法。
     *
     * @throws GraphValidationException 如果图结构不合法
     */
    void validate() throws GraphValidationException;
}
