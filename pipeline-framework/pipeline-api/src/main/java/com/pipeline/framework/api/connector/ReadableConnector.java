package com.pipeline.framework.api.connector;

/**
 * 可读连接器接口。
 * <p>
 * 标记接口，表示该连接器支持读取操作。
 * 结合ConnectorReader使用。
 * </p>
 *
 * @param <T> 数据类型
 * @param <C> 配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ReadableConnector<T, C extends ConnectorConfig> extends ConnectorReader<T, C> {

    /**
     * 创建读取器的副本（用于并行读取）。
     *
     * @return 新的读取器实例
     * @throws ConnectorException 创建失败
     */
    default ConnectorReader<T, C> duplicate() throws ConnectorException {
        throw new ConnectorException("Duplication not supported for " + getName());
    }

    /**
     * 是否支持并行读取。
     *
     * @return true表示支持
     */
    default boolean supportsParallelRead() {
        return false;
    }
}
