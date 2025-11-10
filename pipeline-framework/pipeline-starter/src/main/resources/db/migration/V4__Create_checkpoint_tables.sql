-- =============================================
-- Pipeline Framework - 检查点相关表
-- =============================================

-- 检查点表
CREATE TABLE `pipeline_checkpoint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `checkpoint_id` VARCHAR(64) NOT NULL COMMENT '检查点ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `instance_id` VARCHAR(64) NOT NULL COMMENT '实例ID',
    `checkpoint_type` VARCHAR(32) DEFAULT 'AUTO' COMMENT '检查点类型: AUTO/MANUAL',
    `checkpoint_status` VARCHAR(32) NOT NULL COMMENT '状态: IN_PROGRESS/COMPLETED/FAILED',
    `trigger_time` DATETIME NOT NULL COMMENT '触发时间',
    `complete_time` DATETIME COMMENT '完成时间',
    `duration_ms` BIGINT COMMENT '耗时(毫秒)',
    `state_size_bytes` BIGINT COMMENT '状态大小(字节)',
    `storage_path` VARCHAR(512) COMMENT '存储路径',
    `state_snapshot` JSON COMMENT '状态快照(小状态直接存储)',
    `error_message` TEXT COMMENT '错误信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_checkpoint_id` (`checkpoint_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_trigger_time` (`trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查点表';
