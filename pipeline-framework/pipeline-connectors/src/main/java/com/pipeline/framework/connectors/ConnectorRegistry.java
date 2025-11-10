package com.pipeline.framework.connectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 连接器注册中心接口。
 * <p>
 * 管理所有已注册的连接器。
 * 使用响应式API。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ConnectorRegistry {

    /**
     * 注册连接器。
     * <p>
     * 异步注册连接器到注册中心。
     * </p>
     *
     * @param connector 连接器实例
     * @return 注册完成信号
     */
    Mono<Void> register(Connector connector);

    /**
     * 根据类型获取连接器。
     * <p>
     * 异步查找并返回连接器。
     * </p>
     *
     * @param type 连接器类型
     * @return 连接器实例的Mono
     */
    Mono<Connector> getConnector(String type);

    /**
     * 获取所有已注册的连接器。
     * <p>
     * 返回所有连接器的响应式流。
     * </p>
     *
     * @return 连接器流
     */
    Flux<Connector> getAllConnectors();

    /**
     * 判断连接器是否已注册。
     *
     * @param type 连接器类型
     * @return true如果已注册
     */
    Mono<Boolean> isRegistered(String type);

    /**
     * 注销连接器。
     *
     * @param type 连接器类型
     * @return 注销完成信号
     */
    Mono<Void> unregister(String type);

    /**
     * 重新加载连接器。
     * <p>
     * 重新加载指定类型的连接器。
     * </p>
     *
     * @param type 连接器类型
     * @return 重新加载完成信号
     */
    Mono<Void> reload(String type);
}
