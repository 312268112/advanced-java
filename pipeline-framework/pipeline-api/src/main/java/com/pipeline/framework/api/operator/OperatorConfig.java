package com.pipeline.framework.api.operator;

import java.util.Map;

/**
 * 算子配置接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface OperatorConfig {

    /**
     * 获取算子类型。
     *
     * @return 算子类型
     */
    OperatorType getType();

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
     * 获取并行度。
     *
     * @return 并行度
     */
    int getParallelism();

    /**
     * 获取缓冲区大小。
     *
     * @return 缓冲区大小
     */
    int getBufferSize();
}
