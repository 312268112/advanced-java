-- =============================================
-- Pipeline Framework - 任务管理相关表
-- =============================================

-- 任务定义表
CREATE TABLE `pipeline_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务唯一标识',
    `job_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `job_type` VARCHAR(32) NOT NULL COMMENT '任务类型: STREAMING/BATCH',
    `job_status` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '任务状态: CREATED/SCHEDULED/RUNNING/PAUSED/COMPLETED/FAILED/CANCELLED',
    `description` TEXT COMMENT '任务描述',
    `stream_graph_id` VARCHAR(64) COMMENT 'StreamGraph ID',
    `restart_strategy` VARCHAR(32) DEFAULT 'FIXED_DELAY' COMMENT '重启策略: FIXED_DELAY/EXPONENTIAL_BACKOFF/NO_RESTART',
    `restart_attempts` INT DEFAULT 3 COMMENT '最大重启次数',
    `restart_delay_seconds` INT DEFAULT 10 COMMENT '重启延迟(秒)',
    `checkpoint_enabled` TINYINT DEFAULT 1 COMMENT '是否启用检查点: 0-否, 1-是',
    `checkpoint_interval_seconds` INT DEFAULT 60 COMMENT '检查点间隔(秒)',
    `source_config` JSON COMMENT 'Source配置(JSON)',
    `operators_config` JSON COMMENT 'Operators配置列表(JSON)',
    `sink_config` JSON COMMENT 'Sink配置(JSON)',
    `job_config` JSON COMMENT '任务全局配置(JSON)',
    `creator` VARCHAR(64) COMMENT '创建人',
    `updater` VARCHAR(64) COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_id` (`job_id`),
    KEY `idx_job_name` (`job_name`),
    KEY `idx_job_status` (`job_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Pipeline任务定义表';

-- 任务实例表
CREATE TABLE `pipeline_job_instance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id` VARCHAR(64) NOT NULL COMMENT '实例ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `job_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `instance_status` VARCHAR(32) NOT NULL COMMENT '实例状态: RUNNING/COMPLETED/FAILED/CANCELLED',
    `host_address` VARCHAR(128) COMMENT '运行主机地址',
    `process_id` VARCHAR(64) COMMENT '进程ID',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `duration_ms` BIGINT COMMENT '执行时长(毫秒)',
    `records_read` BIGINT DEFAULT 0 COMMENT '读取记录数',
    `records_processed` BIGINT DEFAULT 0 COMMENT '处理记录数',
    `records_written` BIGINT DEFAULT 0 COMMENT '写入记录数',
    `records_filtered` BIGINT DEFAULT 0 COMMENT '过滤记录数',
    `records_failed` BIGINT DEFAULT 0 COMMENT '失败记录数',
    `error_message` TEXT COMMENT '错误信息',
    `error_stack_trace` TEXT COMMENT '错误堆栈',
    `last_checkpoint_id` VARCHAR(64) COMMENT '最后检查点ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance_id` (`instance_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_status` (`instance_status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_host` (`host_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务实例表';

-- 任务调度配置表
CREATE TABLE `pipeline_job_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `schedule_id` VARCHAR(64) NOT NULL COMMENT '调度ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `schedule_type` VARCHAR(32) NOT NULL COMMENT '调度类型: ONCE/CRON/FIXED_RATE/FIXED_DELAY/MANUAL',
    `schedule_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `cron_expression` VARCHAR(128) COMMENT 'Cron表达式',
    `timezone` VARCHAR(64) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    `next_fire_time` DATETIME COMMENT '下次触发时间',
    `last_fire_time` DATETIME COMMENT '上次触发时间',
    `fire_count` BIGINT DEFAULT 0 COMMENT '触发次数',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_id` (`schedule_id`),
    UNIQUE KEY `uk_job_id` (`job_id`),
    KEY `idx_schedule_type` (`schedule_type`),
    KEY `idx_next_fire_time` (`next_fire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务调度配置表';
