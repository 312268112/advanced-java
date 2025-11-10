package com.pipeline.framework.api.executor;

import com.pipeline.framework.api.job.Job;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 任务执行器接口。
 * <p>
 * 负责执行Pipeline任务。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface JobExecutor {

    /**
     * 提交任务执行。
     * <p>
     * 异步提交任务，立即返回执行结果的Mono。
     * </p>
     *
     * @param job 任务对象
     * @return 执行结果
     */
    Mono<JobResult> submit(Job job);

    /**
     * 停止任务执行。
     *
     * @param jobId 任务ID
     * @return 停止完成信号
     */
    Mono<Void> stop(String jobId);

    /**
     * 暂停任务执行。
     *
     * @param jobId 任务ID
     * @return 暂停完成信号
     */
    Mono<Void> pause(String jobId);

    /**
     * 恢复任务执行。
     *
     * @param jobId 任务ID
     * @return 恢复完成信号
     */
    Mono<Void> resume(String jobId);

    /**
     * 取消任务执行。
     *
     * @param jobId 任务ID
     * @return 取消完成信号
     */
    Mono<Void> cancel(String jobId);

    /**
     * 获取任务执行状态。
     *
     * @param jobId 任务ID
     * @return 执行状态
     */
    Mono<ExecutionStatus> getStatus(String jobId);

    /**
     * 获取任务执行指标。
     *
     * @param jobId 任务ID
     * @return 执行指标流
     */
    Flux<ExecutionMetrics> getMetrics(String jobId);

    /**
     * 获取所有正在运行的任务。
     *
     * @return 运行中的任务流
     */
    Flux<Job> getRunningJobs();

    /**
     * 重启任务。
     *
     * @param jobId 任务ID
     * @return 重启完成信号
     */
    Mono<Void> restart(String jobId);
}
