package com.pipeline.framework.connector.sdk;

/**
 * 生命周期管理接口。
 * <p>
 * Connector实现此接口以管理资源的打开和关闭。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Lifecycle {

    /**
     * 打开连接器，初始化资源。
     *
     * @throws Exception 打开失败
     */
    void open() throws Exception;

    /**
     * 关闭连接器，释放资源。
     *
     * @throws Exception 关闭失败
     */
    void close() throws Exception;
}
