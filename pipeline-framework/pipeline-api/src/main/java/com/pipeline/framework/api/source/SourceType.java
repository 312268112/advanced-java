package com.pipeline.framework.api.source;

/**
 * 数据源类型枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum SourceType {
    /**
     * JDBC数据库源
     */
    JDBC,

    /**
     * Kafka消息源
     */
    KAFKA,

    /**
     * HTTP API源
     */
    HTTP,

    /**
     * 文件源
     */
    FILE,

    /**
     * Redis源
     */
    REDIS,

    /**
     * Elasticsearch源
     */
    ELASTICSEARCH,

    /**
     * 内存源（测试用）
     */
    MEMORY,

    /**
     * 自定义源
     */
    CUSTOM
}
