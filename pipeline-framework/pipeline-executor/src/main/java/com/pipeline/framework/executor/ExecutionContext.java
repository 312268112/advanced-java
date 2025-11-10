package com.pipeline.framework.executor;

import com.pipeline.framework.api.job.Job;
import com.pipeline.framework.checkpoint.CheckpointCoordinator;
import com.pipeline.framework.state.StateManager;

/**
 * 执行上下文接口。
 * <p>
 * 提供任务执行所需的上下文信息。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ExecutionContext {

    /**
     * 获取任务对象。
     *
     * @return 任务对象
     */
    Job getJob();

    /**
     * 获取执行计划。
     *
     * @return 执行计划
     */
    ExecutionPlan getExecutionPlan();

    /**
     * 获取状态管理器。
     *
     * @return 状态管理器
     */
    StateManager getStateManager();

    /**
     * 获取检查点协调器。
     *
     * @return 检查点协调器
     */
    CheckpointCoordinator getCheckpointCoordinator();

    /**
     * 获取执行配置。
     *
     * @param key 配置键
     * @param <T> 值类型
     * @return 配置值
     */
    <T> T getConfig(String key);
}
