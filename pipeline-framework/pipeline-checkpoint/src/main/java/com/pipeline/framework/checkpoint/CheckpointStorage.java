package com.pipeline.framework.checkpoint;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 检查点存储接口。
 * <p>
 * 负责检查点的持久化存储。
 * 所有操作都是响应式的。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface CheckpointStorage {

    /**
     * 保存检查点。
     * <p>
     * 异步保存检查点到持久化存储。
     * </p>
     *
     * @param checkpoint 检查点对象
     * @return 保存完成信号
     */
    Mono<Void> save(Checkpoint checkpoint);

    /**
     * 加载检查点。
     * <p>
     * 异步从存储加载检查点。
     * </p>
     *
     * @param checkpointId 检查点ID
     * @return 检查点对象的Mono
     */
    Mono<Checkpoint> load(String checkpointId);

    /**
     * 删除检查点。
     *
     * @param checkpointId 检查点ID
     * @return 删除完成信号
     */
    Mono<Void> delete(String checkpointId);

    /**
     * 列出所有检查点。
     *
     * @param jobId 任务ID
     * @return 检查点流
     */
    Flux<Checkpoint> list(String jobId);

    /**
     * 判断检查点是否存在。
     *
     * @param checkpointId 检查点ID
     * @return true如果存在
     */
    Mono<Boolean> exists(String checkpointId);

    /**
     * 获取存储大小。
     * <p>
     * 获取指定任务的所有检查点占用的存储空间。
     * </p>
     *
     * @param jobId 任务ID
     * @return 存储大小（字节）
     */
    Mono<Long> getStorageSize(String jobId);

    /**
     * 清空指定任务的所有检查点。
     *
     * @param jobId 任务ID
     * @return 清空完成信号
     */
    Mono<Void> clear(String jobId);
}
