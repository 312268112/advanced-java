package com.pipeline.framework.api.job;

import java.time.Duration;
import java.util.Map;

/**
 * 任务配置接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface JobConfig {

    /**
     * 获取任务类型。
     *
     * @return 任务类型
     */
    JobType getJobType();

    /**
     * 获取配置属性。
     *
     * @param key 配置键
     * @param <T> 值类型
     * @return 配置值
     */
    <T> T getProperty(String key);

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
     * 获取所有配置属性。
     *
     * @return 配置属性Map
     */
    Map<String, Object> getProperties();

    /**
     * 是否启用检查点。
     *
     * @return true如果启用
     */
    boolean isCheckpointEnabled();

    /**
     * 获取检查点间隔。
     *
     * @return 检查点间隔
     */
    Duration getCheckpointInterval();

    /**
     * 获取重启策略。
     *
     * @return 重启策略
     */
    RestartStrategy getRestartStrategy();

    /**
     * 获取最大重启次数。
     *
     * @return 最大重启次数
     */
    int getMaxRestartAttempts();

    /**
     * 获取重启延迟。
     *
     * @return 重启延迟
     */
    Duration getRestartDelay();

    /**
     * 获取全局并行度。
     *
     * @return 并行度
     */
    int getParallelism();

    /**
     * 获取任务超时时间。
     *
     * @return 超时时间
     */
    Duration getTimeout();
}
