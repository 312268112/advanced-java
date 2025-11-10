package com.pipeline.framework.api.job;

import com.pipeline.framework.api.graph.StreamGraph;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * 任务接口。
 * <p>
 * 表示一个完整的数据处理任务。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Job {

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();

    /**
     * 获取任务名称。
     *
     * @return 任务名称
     */
    String getJobName();

    /**
     * 获取任务类型。
     *
     * @return 任务类型
     */
    JobType getType();

    /**
     * 获取任务状态。
     *
     * @return 任务状态
     */
    JobStatus getStatus();

    /**
     * 获取StreamGraph。
     *
     * @return StreamGraph
     */
    StreamGraph getStreamGraph();

    /**
     * 获取任务配置。
     *
     * @return 任务配置
     */
    JobConfig getConfig();

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    Instant getCreateTime();

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    Instant getUpdateTime();

    /**
     * 启动任务。
     *
     * @return 启动完成信号
     */
    Mono<Void> start();

    /**
     * 停止任务。
     *
     * @return 停止完成信号
     */
    Mono<Void> stop();

    /**
     * 暂停任务。
     *
     * @return 暂停完成信号
     */
    Mono<Void> pause();

    /**
     * 恢复任务。
     *
     * @return 恢复完成信号
     */
    Mono<Void> resume();

    /**
     * 取消任务。
     *
     * @return 取消完成信号
     */
    Mono<Void> cancel();
}
