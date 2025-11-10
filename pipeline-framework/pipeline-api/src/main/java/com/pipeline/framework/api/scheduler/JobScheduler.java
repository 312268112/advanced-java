package com.pipeline.framework.api.scheduler;

import com.pipeline.framework.api.job.Job;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 任务调度器接口。
 * <p>
 * 负责任务的调度和生命周期管理。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface JobScheduler {

    /**
     * 调度任务。
     * <p>
     * 根据调度配置安排任务执行。
     * </p>
     *
     * @param job    任务对象
     * @param config 调度配置
     * @return 调度结果
     */
    Mono<ScheduleResult> schedule(Job job, ScheduleConfig config);

    /**
     * 取消任务调度。
     *
     * @param jobId 任务ID
     * @return 取消完成信号
     */
    Mono<Void> cancel(String jobId);

    /**
     * 暂停任务调度。
     *
     * @param jobId 任务ID
     * @return 暂停完成信号
     */
    Mono<Void> pause(String jobId);

    /**
     * 恢复任务调度。
     *
     * @param jobId 任务ID
     * @return 恢复完成信号
     */
    Mono<Void> resume(String jobId);

    /**
     * 立即触发任务执行。
     *
     * @param jobId 任务ID
     * @return 触发完成信号
     */
    Mono<Void> trigger(String jobId);

    /**
     * 获取任务的调度状态。
     *
     * @param jobId 任务ID
     * @return 调度状态
     */
    Mono<ScheduleStatus> getScheduleStatus(String jobId);

    /**
     * 获取所有已调度的任务。
     *
     * @return 已调度任务流
     */
    Flux<Job> getScheduledJobs();

    /**
     * 更新调度配置。
     *
     * @param jobId  任务ID
     * @param config 新的调度配置
     * @return 更新完成信号
     */
    Mono<Void> updateSchedule(String jobId, ScheduleConfig config);
}
