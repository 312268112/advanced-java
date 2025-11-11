package com.pipeline.framework.connectors.jdbc;

import com.pipeline.framework.api.connector.ConnectorConfig;

/**
 * JDBC Connector配置。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class JdbcConnectorConfig extends ConnectorConfig {

    private static final long serialVersionUID = 1L;

    /**
     * JDBC URL
     */
    private String url;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 驱动类名
     */
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    /**
     * 查询SQL（用于Reader）
     */
    private String querySql;

    /**
     * 表名（用于Writer）
     */
    private String tableName;

    /**
     * 批次大小
     */
    private int batchSize = 1000;

    /**
     * 连接池最大连接数
     */
    private int maxPoolSize = 10;

    /**
     * 是否自动提交
     */
    private boolean autoCommit = false;

    /**
     * 查询超时时间（秒）
     */
    private int queryTimeout = 60;

    /**
     * Fetch Size
     */
    private int fetchSize = 1000;

    @Override
    public void validate() throws IllegalArgumentException {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("JDBC URL cannot be null or empty");
        }
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
    }

    // Getters and Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getQuerySql() {
        return querySql;
    }

    public void setQuerySql(String querySql) {
        this.querySql = querySql;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }
}
