package com.pipeline.framework.api.sink;

/**
 * 数据输出类型枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum SinkType {
    /**
     * JDBC数据库输出
     */
    JDBC,

    /**
     * Kafka消息输出
     */
    KAFKA,

    /**
     * HTTP API输出
     */
    HTTP,

    /**
     * 文件输出
     */
    FILE,

    /**
     * Redis输出
     */
    REDIS,

    /**
     * Elasticsearch输出
     */
    ELASTICSEARCH,

    /**
     * 日志输出
     */
    LOG,

    /**
     * 黑洞输出（丢弃数据，用于测试）
     */
    BLACKHOLE,

    /**
     * 自定义输出
     */
    CUSTOM
}
