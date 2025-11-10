package com.pipeline.framework.api.scheduler;

import com.pipeline.framework.api.job.Job;
import reactor.core.publisher.Mono;

/**
 * 任务调度器接口。
 * <p>
 * 负责任务的调度策略，支持多种触发方式。
 * </p>
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface JobScheduler {

    /**
     * 提交任务进行调度。
     *
     * @param job    任务对象
     * @param policy 调度策略
     * @return 调度结果
     */
    Mono<ScheduleResult> schedule(Job job, SchedulePolicy policy);

    /**
     * 取消任务调度。
     *
     * @param jobId 任务ID
     * @return 取消结果
     */
    Mono<Void> cancel(String jobId);

    /**
     * 暂停任务调度。
     *
     * @param jobId 任务ID
     * @return 暂停结果
     */
    Mono<Void> pause(String jobId);

    /**
     * 恢复任务调度。
     *
     * @param jobId 任务ID
     * @return 恢复结果
     */
    Mono<Void> resume(String jobId);

    /**
     * 获取调度状态。
     *
     * @param jobId 任务ID
     * @return 调度状态
     */
    Mono<ScheduleStatus> getStatus(String jobId);
}
