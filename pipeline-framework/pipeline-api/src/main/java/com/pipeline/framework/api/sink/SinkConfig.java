package com.pipeline.framework.api.sink;

import java.util.Map;

/**
 * 数据输出配置接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface SinkConfig {

    /**
     * 获取输出类型。
     *
     * @return 输出类型
     */
    SinkType getType();

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
     * 验证配置是否有效。
     *
     * @return true如果配置有效
     */
    boolean validate();

    /**
     * 获取批次大小。
     *
     * @return 批次大小
     */
    int getBatchSize();

    /**
     * 获取刷新间隔（毫秒）。
     *
     * @return 刷新间隔
     */
    long getFlushInterval();

    /**
     * 是否启用重试。
     *
     * @return true如果启用重试
     */
    boolean isRetryEnabled();

    /**
     * 获取最大重试次数。
     *
     * @return 最大重试次数
     */
    int getMaxRetries();
}
