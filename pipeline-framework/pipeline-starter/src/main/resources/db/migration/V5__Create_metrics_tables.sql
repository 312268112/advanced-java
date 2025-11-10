-- =============================================
-- Pipeline Framework - 监控指标相关表
-- =============================================

-- 任务运行指标表
CREATE TABLE `pipeline_job_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `instance_id` VARCHAR(64) NOT NULL COMMENT '实例ID',
    `metric_time` DATETIME NOT NULL COMMENT '指标时间',
    `records_read_total` BIGINT DEFAULT 0 COMMENT '累计读取记录数',
    `records_processed_total` BIGINT DEFAULT 0 COMMENT '累计处理记录数',
    `records_written_total` BIGINT DEFAULT 0 COMMENT '累计写入记录数',
    `records_read_rate` DECIMAL(20,2) DEFAULT 0 COMMENT '读取速率(记录/秒)',
    `records_write_rate` DECIMAL(20,2) DEFAULT 0 COMMENT '写入速率(记录/秒)',
    `processing_latency_ms` BIGINT DEFAULT 0 COMMENT '处理延迟(毫秒)',
    `backpressure_count` INT DEFAULT 0 COMMENT '背压次数',
    `error_count` INT DEFAULT 0 COMMENT '错误次数',
    `checkpoint_count` INT DEFAULT 0 COMMENT '检查点次数',
    `restart_count` INT DEFAULT 0 COMMENT '重启次数',
    `jvm_heap_used_mb` DECIMAL(10,2) COMMENT 'JVM堆内存使用(MB)',
    `jvm_heap_max_mb` DECIMAL(10,2) COMMENT 'JVM堆内存最大(MB)',
    `cpu_usage_percent` DECIMAL(5,2) COMMENT 'CPU使用率(%)',
    `thread_count` INT COMMENT '线程数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_metric_time` (`metric_time`),
    KEY `idx_job_metric_time` (`job_id`, `metric_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务运行指标表';
