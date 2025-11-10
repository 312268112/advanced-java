package com.pipeline.framework.connectors;

import java.util.List;
import java.util.Optional;

/**
 * 连接器注册中心接口。
 * <p>
 * 管理所有已注册的连接器。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ConnectorRegistry {

    /**
     * 注册连接器。
     *
     * @param connector 连接器实例
     */
    void register(Connector connector);

    /**
     * 根据类型获取连接器。
     *
     * @param type 连接器类型
     * @return 连接器实例
     */
    Optional<Connector> getConnector(String type);

    /**
     * 获取所有已注册的连接器。
     *
     * @return 连接器列表
     */
    List<Connector> getAllConnectors();

    /**
     * 判断连接器是否已注册。
     *
     * @param type 连接器类型
     * @return true如果已注册
     */
    boolean isRegistered(String type);

    /**
     * 注销连接器。
     *
     * @param type 连接器类型
     */
    void unregister(String type);
}
