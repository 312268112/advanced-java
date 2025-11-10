package com.pipeline.framework.checkpoint;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 检查点存储接口。
 * <p>
 * 负责检查点的持久化存储。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface CheckpointStorage {

    /**
     * 保存检查点。
     *
     * @param checkpoint 检查点对象
     * @return 保存结果
     */
    Mono<Void> save(Checkpoint checkpoint);

    /**
     * 加载检查点。
     *
     * @param checkpointId 检查点ID
     * @return 检查点对象
     */
    Mono<Checkpoint> load(String checkpointId);

    /**
     * 删除检查点。
     *
     * @param checkpointId 检查点ID
     * @return 删除结果
     */
    Mono<Void> delete(String checkpointId);

    /**
     * 列出所有检查点。
     *
     * @param jobId 任务ID
     * @return 检查点列表
     */
    Flux<Checkpoint> list(String jobId);

    /**
     * 判断检查点是否存在。
     *
     * @param checkpointId 检查点ID
     * @return true如果存在
     */
    Mono<Boolean> exists(String checkpointId);
}
