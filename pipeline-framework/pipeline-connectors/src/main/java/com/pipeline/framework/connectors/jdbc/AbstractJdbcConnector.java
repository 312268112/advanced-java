package com.pipeline.framework.connectors.jdbc;

import com.pipeline.framework.api.connector.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC Connector抽象基类。
 * <p>
 * 使用模板方法模式，定义JDBC连接的通用逻辑。
 * 子类实现具体的读写操作。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public abstract class AbstractJdbcConnector<T> implements Connector<JdbcConnectorConfig> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final JdbcConnectorConfig config;
    protected Connection connection;
    protected volatile boolean opened = false;

    protected AbstractJdbcConnector(JdbcConnectorConfig config) {
        this.config = config;
        this.config.validate();
    }

    /**
     * 打开连接（模板方法）。
     *
     * @throws Exception 打开失败
     */
    public void open() throws Exception {
        if (opened) {
            logger.warn("Connector already opened: {}", getName());
            return;
        }

        logger.info("Opening JDBC connector: {}", getName());
        
        // 加载驱动
        loadDriver();
        
        // 建立连接
        establishConnection();
        
        // 配置连接
        configureConnection();
        
        // 初始化（钩子方法）
        doOpen();
        
        opened = true;
        logger.info("JDBC connector opened successfully: {}", getName());
    }

    /**
     * 加载JDBC驱动。
     *
     * @throws ClassNotFoundException 驱动类找不到
     */
    protected void loadDriver() throws ClassNotFoundException {
        Class.forName(config.getDriverClassName());
    }

    /**
     * 建立数据库连接。
     *
     * @throws SQLException 连接失败
     */
    protected void establishConnection() throws SQLException {
        connection = DriverManager.getConnection(
            config.getUrl(),
            config.getUsername(),
            config.getPassword()
        );
    }

    /**
     * 配置连接参数。
     *
     * @throws SQLException 配置失败
     */
    protected void configureConnection() throws SQLException {
        connection.setAutoCommit(config.isAutoCommit());
    }

    /**
     * 子类初始化逻辑（钩子方法）。
     *
     * @throws Exception 初始化失败
     */
    protected void doOpen() throws Exception {
        // 默认空实现，子类可覆盖
    }

    /**
     * 关闭连接（模板方法）。
     *
     * @throws Exception 关闭失败
     */
    public void close() throws Exception {
        if (!opened) {
            return;
        }

        logger.info("Closing JDBC connector: {}", getName());
        
        // 清理（钩子方法）
        doClose();
        
        // 关闭连接
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        
        opened = false;
        logger.info("JDBC connector closed: {}", getName());
    }

    /**
     * 子类清理逻辑（钩子方法）。
     *
     * @throws Exception 清理失败
     */
    protected void doClose() throws Exception {
        // 默认空实现，子类可覆盖
    }

    @Override
    public ConnectorType getType() {
        return ConnectorType.JDBC;
    }

    @Override
    public JdbcConnectorConfig getConfig() {
        return config;
    }

    @Override
    public boolean validate() throws ConnectorException {
        try {
            config.validate();
            return true;
        } catch (IllegalArgumentException e) {
            throw new ConnectorException("Validation failed", e, getType(), getName());
        }
    }

    /**
     * 检查连接是否有效。
     *
     * @return true表示有效
     */
    protected boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            logger.error("Connection validation failed", e);
            return false;
        }
    }

    /**
     * 重连（如果连接失效）。
     *
     * @throws Exception 重连失败
     */
    protected void reconnectIfNecessary() throws Exception {
        if (!isConnectionValid()) {
            logger.warn("Connection invalid, reconnecting...");
            close();
            open();
        }
    }
}
