package com.pipeline.framework.api.graph;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点执行上下文。
 * <p>
 * 提供节点执行过程中所需的所有资源和缓存。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface NodeExecutionContext {

    /**
     * 获取 StreamGraph。
     *
     * @return StreamGraph 实例
     */
    StreamGraph getGraph();

    /**
     * 获取 Source 组件。
     *
     * @param nodeId 节点 ID
     * @param <T>    数据类型
     * @return Source 实例
     */
    <T> Optional<DataSource<T>> getSource(String nodeId);

    /**
     * 获取 Operator 组件。
     *
     * @param nodeId 节点 ID
     * @param <IN>   输入类型
     * @param <OUT>  输出类型
     * @return Operator 实例
     */
    <IN, OUT> Optional<Operator<IN, OUT>> getOperator(String nodeId);

    /**
     * 获取 Sink 组件。
     *
     * @param nodeId 节点 ID
     * @param <T>    数据类型
     * @return Sink 实例
     */
    <T> Optional<DataSink<T>> getSink(String nodeId);

    /**
     * 获取节点的缓存 Flux。
     *
     * @param nodeId 节点 ID
     * @param <T>    数据类型
     * @return 缓存的 Flux
     */
    <T> Optional<Flux<T>> getCachedFlux(String nodeId);

    /**
     * 缓存节点的 Flux。
     *
     * @param nodeId 节点 ID
     * @param flux   数据流
     * @param <T>    数据类型
     */
    <T> void cacheFlux(String nodeId, Flux<T> flux);

    /**
     * 获取上下文属性。
     *
     * @param key 属性键
     * @param <T> 属性类型
     * @return 属性值
     */
    <T> Optional<T> getAttribute(String key);

    /**
     * 设置上下文属性。
     *
     * @param key   属性键
     * @param value 属性值
     */
    void setAttribute(String key, Object value);
}
