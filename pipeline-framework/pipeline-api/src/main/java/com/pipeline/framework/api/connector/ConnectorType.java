package com.pipeline.framework.api.connector;

/**
 * Connector类型枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum ConnectorType {
    /**
     * JDBC数据库连接器
     */
    JDBC,

    /**
     * Kafka消息队列连接器
     */
    KAFKA,

    /**
     * Redis缓存连接器
     */
    REDIS,

    /**
     * 文件系统连接器
     */
    FILE,

    /**
     * HTTP/REST API连接器
     */
    HTTP,

    /**
     * MongoDB连接器
     */
    MONGODB,

    /**
     * Elasticsearch连接器
     */
    ELASTICSEARCH,

    /**
     * 控制台连接器（用于调试）
     */
    CONSOLE,

    /**
     * 自定义连接器
     */
    CUSTOM
}
