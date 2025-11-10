package com.pipeline.framework.api.source;

import java.util.Map;

/**
 * 数据源配置接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface SourceConfig {

    /**
     * 获取数据源类型。
     *
     * @return 数据源类型
     */
    SourceType getType();

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
     * 获取并行度。
     *
     * @return 并行度
     */
    int getParallelism();
}
