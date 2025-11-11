package com.pipeline.framework.api.connector;

/**
 * Connector基础接口。
 * <p>
 * 所有连接器的顶层抽象，提供生命周期管理和元数据访问。
 * 不依赖Reactor，可以独立使用。
 * </p>
 *
 * @param <C> 配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Connector<C extends ConnectorConfig> {

    /**
     * 获取连接器名称。
     *
     * @return 连接器名称
     */
    String getName();

    /**
     * 获取连接器类型。
     *
     * @return 连接器类型
     */
    ConnectorType getType();

    /**
     * 获取连接器配置。
     *
     * @return 配置对象
     */
    C getConfig();

    /**
     * 获取连接器元数据。
     *
     * @return 元数据
     */
    default ConnectorMetadata getMetadata() {
        return ConnectorMetadata.builder()
            .name(getName())
            .type(getType())
            .version("1.0.0")
            .build();
    }

    /**
     * 验证配置是否有效。
     *
     * @return true表示配置有效
     * @throws ConnectorException 配置无效时抛出
     */
    default boolean validate() throws ConnectorException {
        return getConfig() != null;
    }
}
