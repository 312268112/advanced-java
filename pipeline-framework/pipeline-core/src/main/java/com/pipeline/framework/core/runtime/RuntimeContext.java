package com.pipeline.framework.core.runtime;

import com.pipeline.framework.api.job.Job;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * 运行时上下文接口。
 * <p>
 * 提供任务运行时所需的各种上下文信息和服务。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface RuntimeContext {

    /**
     * 获取当前Job。
     *
     * @return Job对象的Mono
     */
    Mono<Job> getJob();

    /**
     * 获取Reactor调度器。
     *
     * @return 调度器
     */
    Scheduler getScheduler();

    /**
     * 获取配置属性。
     *
     * @param key 配置键
     * @param <T> 值类型
     * @return 配置值的Mono
     */
    <T> Mono<T> getProperty(String key);

    /**
     * 获取配置属性（带默认值）。
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 配置值
     */
    <T> T getProperty(String key, T defaultValue);

    /**
     * 获取运行时指标。
     *
     * @return 运行时指标对象
     */
    RuntimeMetrics getMetrics();

    /**
     * 获取实例ID。
     *
     * @return 实例ID
     */
    String getInstanceId();

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();
}
