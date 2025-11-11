package com.pipeline.framework.api.connector;

/**
 * 可写连接器接口。
 * <p>
 * 标记接口，表示该连接器支持写入操作。
 * 结合ConnectorWriter使用。
 * </p>
 *
 * @param <T> 数据类型
 * @param <C> 配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface WritableConnector<T, C extends ConnectorConfig> extends ConnectorWriter<T, C> {

    /**
     * 创建写入器的副本（用于并行写入）。
     *
     * @return 新的写入器实例
     * @throws ConnectorException 创建失败
     */
    default ConnectorWriter<T, C> duplicate() throws ConnectorException {
        throw new ConnectorException("Duplication not supported for " + getName());
    }

    /**
     * 是否支持并行写入。
     *
     * @return true表示支持
     */
    default boolean supportsParallelWrite() {
        return false;
    }

    /**
     * 是否支持幂等写入。
     *
     * @return true表示支持
     */
    default boolean supportsIdempotentWrite() {
        return false;
    }
}
