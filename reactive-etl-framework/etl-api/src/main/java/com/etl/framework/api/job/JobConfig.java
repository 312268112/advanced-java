package com.etl.framework.api.job;

import java.util.Map;

/**
 * 任务配置接口。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface JobConfig {

    /**
     * 是否启用检查点。
     *
     * @return true如果启用，否则返回false
     */
    boolean isCheckpointEnabled();

    /**
     * 获取检查点间隔（秒）。
     *
     * @return 检查点间隔
     */
    int getCheckpointIntervalSeconds();

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
     * 获取重启延迟（秒）。
     *
     * @return 重启延迟
     */
    int getRestartDelaySeconds();

    /**
     * 获取全局配置参数。
     *
     * @return 配置参数Map
     */
    Map<String, Object> getGlobalConfig();
}
