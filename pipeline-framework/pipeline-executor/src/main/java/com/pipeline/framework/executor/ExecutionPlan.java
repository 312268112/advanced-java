package com.pipeline.framework.executor;

import com.pipeline.framework.api.graph.StreamNode;

import java.util.List;

/**
 * 执行计划接口。
 * <p>
 * 定义任务的执行计划和拓扑顺序。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ExecutionPlan {

    /**
     * 获取执行计划ID。
     *
     * @return 执行计划ID
     */
    String getPlanId();

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();

    /**
     * 获取执行节点列表（拓扑排序）。
     *
     * @return 执行节点列表
     */
    List<StreamNode> getExecutionNodes();

    /**
     * 获取并行度。
     *
     * @return 并行度
     */
    int getParallelism();

    /**
     * 判断执行计划是否有效。
     *
     * @return true如果有效
     */
    boolean isValid();
}
