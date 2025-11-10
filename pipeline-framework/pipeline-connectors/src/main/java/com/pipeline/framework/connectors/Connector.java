package com.pipeline.framework.connectors;

import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import reactor.core.publisher.Mono;

/**
 * 连接器接口。
 * <p>
 * 连接器提供Source和Sink的创建能力。
 * 所有操作都是响应式的。
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
     * 获取连接器版本。
     *
     * @return 版本号
     */
    String getVersion();

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
     * <p>
     * 异步创建并初始化Source。
     * </p>
     *
     * @param config Source配置
     * @param <T>    数据类型
     * @return DataSource实例的Mono
     */
    <T> Mono<DataSource<T>> createSource(SourceConfig config);

    /**
     * 创建Sink。
     * <p>
     * 异步创建并初始化Sink。
     * </p>
     *
     * @param config Sink配置
     * @param <T>    数据类型
     * @return DataSink实例的Mono
     */
    <T> Mono<DataSink<T>> createSink(SinkConfig config);

    /**
     * 验证配置。
     * <p>
     * 异步验证连接器配置的有效性。
     * </p>
     *
     * @param config 配置对象
     * @return 验证结果，true表示有效
     */
    Mono<Boolean> validateConfig(Object config);

    /**
     * 健康检查。
     * <p>
     * 检查连接器及其依赖的外部系统是否正常。
     * </p>
     *
     * @return 健康状态，true表示健康
     */
    Mono<Boolean> healthCheck();
}
