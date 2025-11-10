package com.pipeline.framework.api.source;

/**
 * 数据源类型枚举。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public enum SourceType {
    /**
     * 有界数据源，数据有限（如文件、数据库表）
     */
    BOUNDED,

    /**
     * 无界数据源，数据持续产生（如Kafka、WebSocket）
     */
    UNBOUNDED
}
