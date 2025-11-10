package com.pipeline.framework.checkpoint;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 检查点协调器接口。
 * <p>
 * 负责协调检查点的创建和恢复。
 * 所有操作都是响应式的。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface CheckpointCoordinator {

    /**
     * 触发检查点。
     * <p>
     * 异步触发创建检查点。
     * </p>
     *
     * @return 检查点对象的Mono
     */
    Mono<Checkpoint> triggerCheckpoint();

    /**
     * 触发指定类型的检查点。
     *
     * @param type 检查点类型
     * @return 检查点对象的Mono
     */
    Mono<Checkpoint> triggerCheckpoint(CheckpointType type);

    /**
     * 定期触发检查点。
     * <p>
     * 按指定间隔自动创建检查点。
     * </p>
     *
     * @param interval 检查点间隔
     * @return 检查点流
     */
    Flux<Checkpoint> scheduleCheckpoints(Duration interval);

    /**
     * 从检查点恢复。
     * <p>
     * 异步从指定检查点恢复状态。
     * </p>
     *
     * @param checkpointId 检查点ID
     * @return 恢复完成信号
     */
    Mono<Void> restoreFromCheckpoint(String checkpointId);

    /**
     * 获取最新的检查点。
     *
     * @return 最新的检查点的Mono
     */
    Mono<Checkpoint> getLatestCheckpoint();

    /**
     * 获取指定任务的最新检查点。
     *
     * @param jobId 任务ID
     * @return 最新的检查点的Mono
     */
    Mono<Checkpoint> getLatestCheckpoint(String jobId);

    /**
     * 删除检查点。
     *
     * @param checkpointId 检查点ID
     * @return 删除完成信号
     */
    Mono<Void> deleteCheckpoint(String checkpointId);

    /**
     * 清理过期的检查点。
     * <p>
     * 只保留最新的N个检查点。
     * </p>
     *
     * @param retentionCount 保留数量
     * @return 清理的检查点数量
     */
    Mono<Integer> cleanupExpiredCheckpoints(int retentionCount);

    /**
     * 获取所有检查点。
     *
     * @param jobId 任务ID
     * @return 检查点流
     */
    Flux<Checkpoint> getAllCheckpoints(String jobId);

    /**
     * 停止检查点调度。
     *
     * @return 停止完成信号
     */
    Mono<Void> stop();
}
