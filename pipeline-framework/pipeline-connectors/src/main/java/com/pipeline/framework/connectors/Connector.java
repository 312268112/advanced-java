package com.pipeline.framework.connectors;

import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;

/**
 * 连接器接口。
 * <p>
 * 连接器提供Source和Sink的创建能力。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Connector {

    /**
     * 获取连接器类型。
     *
     * @return 连接器类型（如：jdbc, kafka, http）
     */
    String getType();

    /**
     * 获取连接器名称。
     *
     * @return 连接器名称
     */
    String getName();

    /**
     * 是否支持Source。
     *
     * @return true如果支持
     */
    boolean supportsSource();

    /**
     * 是否支持Sink。
     *
     * @return true如果支持
     */
    boolean supportsSink();

    /**
     * 创建Source。
     *
     * @param config Source配置
     * @param <T>    数据类型
     * @return DataSource实例
     */
    <T> DataSource<T> createSource(SourceConfig config);

    /**
     * 创建Sink。
     *
     * @param config Sink配置
     * @param <T>    数据类型
     * @return DataSink实例
     */
    <T> DataSink<T> createSink(SinkConfig config);

    /**
     * 验证配置。
     *
     * @param config 配置对象
     * @return true如果配置有效
     */
    boolean validateConfig(Object config);
}
