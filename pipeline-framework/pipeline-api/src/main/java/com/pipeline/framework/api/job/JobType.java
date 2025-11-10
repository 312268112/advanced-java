package com.pipeline.framework.api.job;

/**
 * 任务类型枚举。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public enum JobType {
    /**
     * 流式任务（持续运行）
     * 用于实时数据流处理，如Kafka消费、实时ETL等
     */
    STREAMING,

    /**
     * 批处理任务（一次性）
     * 用于一次性数据处理任务，如文件导入、数据迁移等
     */
    BATCH,

    /**
     * SQL批量任务（多表整合）
     * 用于大SQL多表关联、复杂聚合等批量数据处理
     */
    SQL_BATCH
}
