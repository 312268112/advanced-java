package com.pipeline.framework.api.graph;

import java.util.List;
import java.util.Map;

/**
 * 流节点接口。
 * <p>
 * 表示流图中的一个处理节点（Source、Operator或Sink）。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface StreamNode {

    /**
     * 获取节点ID。
     *
     * @return 节点ID
     */
    String getNodeId();

    /**
     * 获取节点名称。
     *
     * @return 节点名称
     */
    String getNodeName();

    /**
     * 获取节点类型。
     *
     * @return 节点类型
     */
    NodeType getNodeType();

    /**
     * 获取算子类型（仅对Operator节点有效）。
     *
     * @return 算子类型
     */
    String getOperatorType();

    /**
     * 获取上游节点ID列表。
     *
     * @return 上游节点ID列表
     */
    List<String> getUpstream();

    /**
     * 获取下游节点ID列表。
     *
     * @return 下游节点ID列表
     */
    List<String> getDownstream();

    /**
     * 获取节点配置。
     *
     * @return 配置Map
     */
    Map<String, Object> getConfig();

    /**
     * 获取并行度。
     *
     * @return 并行度，-1表示使用全局配置
     */
    int getParallelism();

    /**
     * 获取节点描述。
     *
     * @return 节点描述
     */
    String getDescription();
}
