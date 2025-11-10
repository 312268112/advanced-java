package com.pipeline.framework.checkpoint;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 检查点协调器接口。
 * <p>
 * 负责协调检查点的创建和恢复。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface CheckpointCoordinator {

    /**
     * 触发检查点。
     *
     * @return 检查点对象
     */
    Mono<Checkpoint> triggerCheckpoint();

    /**
     * 定期触发检查点。
     *
     * @param interval 检查点间隔
     * @return 检查点流
     */
    Flux<Checkpoint> scheduleCheckpoints(Duration interval);

    /**
     * 从检查点恢复。
     *
     * @param checkpointId 检查点ID
     * @return 恢复结果
     */
    Mono<Void> restoreFromCheckpoint(String checkpointId);

    /**
     * 获取最新的检查点。
     *
     * @return 最新的检查点
     */
    Mono<Checkpoint> getLatestCheckpoint();

    /**
     * 删除检查点。
     *
     * @param checkpointId 检查点ID
     * @return 删除结果
     */
    Mono<Void> deleteCheckpoint(String checkpointId);

    /**
     * 清理过期的检查点。
     *
     * @param retentionCount 保留数量
     * @return 清理结果
     */
    Mono<Integer> cleanupExpiredCheckpoints(int retentionCount);
}
