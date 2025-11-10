package com.pipeline.framework.api.executor;

import com.pipeline.framework.api.job.Job;
import reactor.core.publisher.Mono;

/**
 * 任务执行器接口。
 * <p>
 * 负责实际执行ETL任务，将StreamGraph转换为可执行的Reactor流。
 * </p>
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface JobExecutor {

    /**
     * 执行任务。
     *
     * @param job 任务对象
     * @return 执行结果
     */
    Mono<JobResult> execute(Job job);

    /**
     * 停止任务。
     *
     * @param jobId 任务ID
     * @return 停止结果
     */
    Mono<Void> stop(String jobId);

    /**
     * 获取执行状态。
     *
     * @param jobId 任务ID
     * @return 执行状态
     */
    Mono<ExecutionStatus> getStatus(String jobId);

    /**
     * 获取执行指标。
     *
     * @param jobId 任务ID
     * @return 执行指标
     */
    Mono<ExecutionMetrics> getMetrics(String jobId);
}
