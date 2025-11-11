package com.pipeline.framework.api.connector.factory;

import com.pipeline.framework.api.connector.*;

/**
 * Connector工厂接口。
 * <p>
 * 使用工厂模式创建各种类型的Connector。
 * 支持泛型，提供类型安全的创建方法。
 * </p>
 *
 * @param <T> 数据类型
 * @param <C> 配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ConnectorFactory<T, C extends ConnectorConfig> {

    /**
     * 创建Reader。
     *
     * @param config 配置
     * @return Reader实例
     * @throws ConnectorException 创建失败
     */
    ConnectorReader<T, C> createReader(C config) throws ConnectorException;

    /**
     * 创建Writer。
     *
     * @param config 配置
     * @return Writer实例
     * @throws ConnectorException 创建失败
     */
    ConnectorWriter<T, C> createWriter(C config) throws ConnectorException;

    /**
     * 获取支持的Connector类型。
     *
     * @return Connector类型
     */
    ConnectorType getSupportedType();

    /**
     * 验证配置是否支持。
     *
     * @param config 配置
     * @return true表示支持
     */
    default boolean supports(C config) {
        return config != null;
    }
}
