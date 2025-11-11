package com.pipeline.framework.api.connector.factory;

import com.pipeline.framework.api.connector.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connector工厂注册中心。
 * <p>
 * 管理所有Connector工厂，支持动态注册和查找。
 * 使用单例模式 + 注册表模式。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConnectorFactoryRegistry {

    private static final ConnectorFactoryRegistry INSTANCE = new ConnectorFactoryRegistry();

    private final Map<ConnectorType, ConnectorFactory<?, ?>> factories = new ConcurrentHashMap<>();

    private ConnectorFactoryRegistry() {
    }

    public static ConnectorFactoryRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册工厂。
     *
     * @param type    Connector类型
     * @param factory 工厂实例
     * @param <T>     数据类型
     * @param <C>     配置类型
     */
    public <T, C extends ConnectorConfig> void register(ConnectorType type, ConnectorFactory<T, C> factory) {
        factories.put(type, factory);
    }

    /**
     * 获取工厂。
     *
     * @param type Connector类型
     * @param <T>  数据类型
     * @param <C>  配置类型
     * @return 工厂实例（Optional）
     */
    @SuppressWarnings("unchecked")
    public <T, C extends ConnectorConfig> Optional<ConnectorFactory<T, C>> getFactory(ConnectorType type) {
        return Optional.ofNullable((ConnectorFactory<T, C>) factories.get(type));
    }

    /**
     * 创建Reader。
     *
     * @param type   Connector类型
     * @param config 配置
     * @param <T>    数据类型
     * @param <C>    配置类型
     * @return Reader实例
     * @throws ConnectorException 创建失败
     */
    public <T, C extends ConnectorConfig> ConnectorReader<T, C> createReader(ConnectorType type, C config)
        throws ConnectorException {
        ConnectorFactory<T, C> factory = this.<T, C>getFactory(type)
            .orElseThrow(() -> new ConnectorException("No factory found for type: " + type));
        return factory.createReader(config);
    }

    /**
     * 创建Writer。
     *
     * @param type   Connector类型
     * @param config 配置
     * @param <T>    数据类型
     * @param <C>    配置类型
     * @return Writer实例
     * @throws ConnectorException 创建失败
     */
    public <T, C extends ConnectorConfig> ConnectorWriter<T, C> createWriter(ConnectorType type, C config)
        throws ConnectorException {
        ConnectorFactory<T, C> factory = this.<T, C>getFactory(type)
            .orElseThrow(() -> new ConnectorException("No factory found for type: " + type));
        return factory.createWriter(config);
    }

    /**
     * 注销工厂。
     *
     * @param type Connector类型
     */
    public void unregister(ConnectorType type) {
        factories.remove(type);
    }

    /**
     * 清空所有工厂。
     */
    public void clear() {
        factories.clear();
    }
}
