-- =============================================
-- 响应式ETL框架 - 数据库表结构设计（单机版）
-- 版本: v2.0
-- 创建日期: 2025-11-09
-- 说明: 单机执行模式，一个Job作为最小执行单元
-- =============================================

-- =============================================
-- 1. 任务管理相关表
-- =============================================

-- 1.1 任务定义表
CREATE TABLE `etl_job` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ETL任务定义表';

-- 1.2 任务实例表（记录每个Job的运行实例）
CREATE TABLE `etl_job_instance` (
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

-- 1.3 任务调度配置表
CREATE TABLE `etl_job_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `schedule_id` VARCHAR(64) NOT NULL COMMENT '调度ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `schedule_type` VARCHAR(32) NOT NULL COMMENT '调度类型: IMMEDIATE/CRON/MANUAL',
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

-- =============================================
-- 2. 图结构相关表（简化）
-- =============================================

-- 2.1 StreamGraph表
CREATE TABLE `etl_stream_graph` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `graph_id` VARCHAR(64) NOT NULL COMMENT '图ID',
    `graph_name` VARCHAR(128) NOT NULL COMMENT '图名称',
    `job_id` VARCHAR(64) COMMENT '关联任务ID',
    `graph_definition` JSON NOT NULL COMMENT '图定义(完整的节点和边JSON)',
    `description` TEXT COMMENT '描述',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_graph_id` (`graph_id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='StreamGraph定义表';

-- =============================================
-- 3. 连接器配置相关表
-- =============================================

-- 3.1 连接器注册表
CREATE TABLE `etl_connector` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `connector_id` VARCHAR(64) NOT NULL COMMENT '连接器ID',
    `connector_name` VARCHAR(128) NOT NULL COMMENT '连接器名称',
    `connector_type` VARCHAR(64) NOT NULL COMMENT '连接器类型: JDBC/KAFKA/HTTP/FILE/REDIS/ELASTICSEARCH等',
    `connector_class` VARCHAR(256) NOT NULL COMMENT '连接器实现类全限定名',
    `version` VARCHAR(32) DEFAULT '1.0.0' COMMENT '版本号',
    `description` TEXT COMMENT '描述',
    `support_source` TINYINT DEFAULT 0 COMMENT '是否支持Source: 0-否, 1-是',
    `support_sink` TINYINT DEFAULT 0 COMMENT '是否支持Sink: 0-否, 1-是',
    `config_schema` JSON COMMENT '配置Schema定义(JSON Schema)',
    `is_builtin` TINYINT DEFAULT 0 COMMENT '是否内置: 0-否, 1-是',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connector_id` (`connector_id`),
    KEY `idx_connector_type` (`connector_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器注册表';

-- 3.2 数据源配置表
CREATE TABLE `etl_datasource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `datasource_id` VARCHAR(64) NOT NULL COMMENT '数据源ID',
    `datasource_name` VARCHAR(128) NOT NULL COMMENT '数据源名称',
    `connector_id` VARCHAR(64) NOT NULL COMMENT '连接器ID',
    `datasource_type` VARCHAR(64) NOT NULL COMMENT '数据源类型',
    `connection_config` JSON NOT NULL COMMENT '连接配置(JSON)',
    `description` TEXT COMMENT '描述',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_datasource_id` (`datasource_id`),
    KEY `idx_connector_id` (`connector_id`),
    KEY `idx_datasource_name` (`datasource_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

-- =============================================
-- 4. 检查点相关表（简化）
-- =============================================

-- 4.1 检查点表
CREATE TABLE `etl_checkpoint` (
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

-- =============================================
-- 5. 监控指标相关表（简化）
-- =============================================

-- 5.1 任务运行指标表
CREATE TABLE `etl_job_metrics` (
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

-- =============================================
-- 6. 系统配置和告警相关表
-- =============================================

-- 6.1 系统配置表
CREATE TABLE `etl_system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(128) NOT NULL COMMENT '配置Key',
    `config_value` TEXT NOT NULL COMMENT '配置Value',
    `config_type` VARCHAR(32) NOT NULL COMMENT '配置类型: STRING/INT/BOOLEAN/JSON',
    `config_group` VARCHAR(64) COMMENT '配置分组: SYSTEM/EXECUTOR/CHECKPOINT/METRICS',
    `description` TEXT COMMENT '描述',
    `is_encrypted` TINYINT DEFAULT 0 COMMENT '是否加密: 0-否, 1-是',
    `is_readonly` TINYINT DEFAULT 0 COMMENT '是否只读: 0-否, 1-是',
    `updater` VARCHAR(64) COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 6.2 告警规则表
CREATE TABLE `etl_alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `rule_id` VARCHAR(64) NOT NULL COMMENT '规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
    `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型: JOB_FAILED/JOB_TIMEOUT/HIGH_ERROR_RATE/CHECKPOINT_FAILED',
    `job_id` VARCHAR(64) COMMENT '目标任务ID(空表示所有任务)',
    `condition_expression` TEXT COMMENT '条件表达式',
    `alert_level` VARCHAR(32) NOT NULL DEFAULT 'WARNING' COMMENT '告警级别: INFO/WARNING/ERROR/CRITICAL',
    `notification_channels` VARCHAR(256) COMMENT '通知渠道(逗号分隔): EMAIL/SMS/WEBHOOK/DINGTALK',
    `notification_config` JSON COMMENT '通知配置(JSON)',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_id` (`rule_id`),
    KEY `idx_rule_type` (`rule_type`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- 6.3 告警记录表
CREATE TABLE `etl_alert_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alert_id` VARCHAR(64) NOT NULL COMMENT '告警ID',
    `rule_id` VARCHAR(64) NOT NULL COMMENT '规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
    `alert_level` VARCHAR(32) NOT NULL COMMENT '告警级别',
    `job_id` VARCHAR(64) COMMENT '任务ID',
    `instance_id` VARCHAR(64) COMMENT '实例ID',
    `alert_time` DATETIME NOT NULL COMMENT '告警时间',
    `alert_message` TEXT NOT NULL COMMENT '告警消息',
    `alert_context` JSON COMMENT '告警上下文(JSON)',
    `is_resolved` TINYINT DEFAULT 0 COMMENT '是否已解决: 0-否, 1-是',
    `resolve_time` DATETIME COMMENT '解决时间',
    `notification_status` VARCHAR(32) COMMENT '通知状态: PENDING/SENT/FAILED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_alert_id` (`alert_id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_alert_time` (`alert_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- =============================================
-- 7. 用户和审计相关表
-- =============================================

-- 7.1 用户表
CREATE TABLE `etl_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) COMMENT '密码(加密)',
    `email` VARCHAR(128) COMMENT '邮箱',
    `phone` VARCHAR(32) COMMENT '手机号',
    `real_name` VARCHAR(64) COMMENT '真实姓名',
    `role` VARCHAR(32) DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    `status` VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 7.2 操作日志表
CREATE TABLE `etl_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `log_id` VARCHAR(64) NOT NULL COMMENT '日志ID',
    `user_id` VARCHAR(64) COMMENT '用户ID',
    `username` VARCHAR(64) COMMENT '用户名',
    `operation_type` VARCHAR(64) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/START/STOP/RESTART',
    `resource_type` VARCHAR(32) NOT NULL COMMENT '资源类型: JOB/DATASOURCE/SCHEDULE',
    `resource_id` VARCHAR(64) COMMENT '资源ID',
    `operation_desc` TEXT COMMENT '操作描述',
    `request_params` JSON COMMENT '请求参数(JSON)',
    `operation_status` VARCHAR(32) NOT NULL COMMENT '操作状态: SUCCESS/FAILED',
    `error_message` TEXT COMMENT '错误信息',
    `ip_address` VARCHAR(64) COMMENT 'IP地址',
    `operation_time` DATETIME NOT NULL COMMENT '操作时间',
    `duration_ms` BIGINT COMMENT '耗时(毫秒)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_log_id` (`log_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_resource` (`resource_type`, `resource_id`),
    KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- =============================================
-- 初始化数据
-- =============================================

-- 插入内置连接器
INSERT INTO `etl_connector` (`connector_id`, `connector_name`, `connector_type`, `connector_class`, `version`, `description`, `support_source`, `support_sink`, `is_builtin`, `is_enabled`, `creator`) VALUES
('jdbc-connector', 'JDBC Connector', 'JDBC', 'com.etl.connector.jdbc.JdbcConnector', '1.0.0', 'JDBC数据库连接器，支持MySQL、PostgreSQL、Oracle等', 1, 1, 1, 1, 'system'),
('kafka-connector', 'Kafka Connector', 'KAFKA', 'com.etl.connector.kafka.KafkaConnector', '1.0.0', 'Apache Kafka消息队列连接器', 1, 1, 1, 1, 'system'),
('http-connector', 'HTTP Connector', 'HTTP', 'com.etl.connector.http.HttpConnector', '1.0.0', 'HTTP/HTTPS API连接器', 1, 1, 1, 1, 'system'),
('file-connector', 'File Connector', 'FILE', 'com.etl.connector.file.FileConnector', '1.0.0', '文件系统连接器，支持CSV、JSON、Parquet等格式', 1, 1, 1, 1, 'system'),
('redis-connector', 'Redis Connector', 'REDIS', 'com.etl.connector.redis.RedisConnector', '1.0.0', 'Redis缓存连接器', 1, 1, 1, 1, 'system'),
('elasticsearch-connector', 'Elasticsearch Connector', 'ELASTICSEARCH', 'com.etl.connector.es.ElasticsearchConnector', '1.0.0', 'Elasticsearch搜索引擎连接器', 1, 1, 1, 1, 'system');

-- 插入系统配置
INSERT INTO `etl_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`) VALUES
('system.thread.pool.core.size', '10', 'INT', 'EXECUTOR', '执行器线程池核心大小'),
('system.thread.pool.max.size', '50', 'INT', 'EXECUTOR', '执行器线程池最大大小'),
('system.thread.pool.queue.capacity', '1000', 'INT', 'EXECUTOR', '线程池队列容量'),
('system.checkpoint.enabled', 'true', 'BOOLEAN', 'CHECKPOINT', '全局是否启用检查点'),
('system.checkpoint.interval.seconds', '60', 'INT', 'CHECKPOINT', '默认检查点间隔(秒)'),
('system.checkpoint.storage.path', '/data/checkpoints', 'STRING', 'CHECKPOINT', '检查点存储路径'),
('system.checkpoint.retention.count', '5', 'INT', 'CHECKPOINT', '保留检查点数量'),
('system.metrics.enabled', 'true', 'BOOLEAN', 'METRICS', '是否启用监控指标采集'),
('system.metrics.collect.interval.seconds', '10', 'INT', 'METRICS', '指标采集间隔(秒)'),
('system.scheduler.enabled', 'true', 'BOOLEAN', 'SYSTEM', '是否启用调度器'),
('system.restart.max.attempts', '3', 'INT', 'EXECUTOR', '默认最大重启次数');

-- 插入默认告警规则
INSERT INTO `etl_alert_rule` (`rule_id`, `rule_name`, `rule_type`, `alert_level`, `condition_expression`, `is_enabled`, `creator`) VALUES
('alert-job-failed', '任务失败告警', 'JOB_FAILED', 'ERROR', 'instance_status == FAILED', 1, 'system'),
('alert-job-timeout', '任务超时告警', 'JOB_TIMEOUT', 'WARNING', 'duration_ms > 3600000', 1, 'system'),
('alert-high-error-rate', '高错误率告警', 'HIGH_ERROR_RATE', 'WARNING', 'error_count / records_read_total > 0.01', 1, 'system'),
('alert-checkpoint-failed', '检查点失败告警', 'CHECKPOINT_FAILED', 'WARNING', 'checkpoint_status == FAILED', 1, 'system');

-- 插入默认管理员用户（密码: admin123，需要BCrypt加密）
INSERT INTO `etl_user` (`user_id`, `username`, `password`, `email`, `real_name`, `role`, `status`) VALUES
('user-admin', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', '系统管理员', 'ADMIN', 'ACTIVE');

-- =============================================
-- 视图定义（方便查询）
-- =============================================

-- 任务实例统计视图
CREATE OR REPLACE VIEW `v_job_instance_stats` AS
SELECT 
    j.job_id,
    j.job_name,
    j.job_type,
    j.job_status,
    COUNT(i.id) as total_runs,
    SUM(CASE WHEN i.instance_status = 'COMPLETED' THEN 1 ELSE 0 END) as success_runs,
    SUM(CASE WHEN i.instance_status = 'FAILED' THEN 1 ELSE 0 END) as failed_runs,
    AVG(i.duration_ms) as avg_duration_ms,
    MAX(i.start_time) as last_run_time
FROM etl_job j
LEFT JOIN etl_job_instance i ON j.job_id = i.job_id
WHERE j.is_deleted = 0
GROUP BY j.job_id, j.job_name, j.job_type, j.job_status;

-- 当前运行任务视图
CREATE OR REPLACE VIEW `v_running_jobs` AS
SELECT 
    i.instance_id,
    i.job_id,
    i.job_name,
    i.instance_status,
    i.host_address,
    i.start_time,
    TIMESTAMPDIFF(SECOND, i.start_time, NOW()) as running_seconds,
    i.records_read,
    i.records_processed,
    i.records_written
FROM etl_job_instance i
WHERE i.instance_status = 'RUNNING'
ORDER BY i.start_time DESC;

-- =============================================
-- 索引优化建议（根据实际查询调整）
-- =============================================
-- ALTER TABLE `etl_job_instance` ADD INDEX `idx_job_status_time` (`job_id`, `instance_status`, `start_time`);
-- ALTER TABLE `etl_job_metrics` ADD INDEX `idx_instance_metric_time` (`instance_id`, `metric_time`);

-- =============================================
-- 分区建议（数据量大时）
-- =============================================
-- ALTER TABLE `etl_job_metrics` PARTITION BY RANGE (TO_DAYS(metric_time)) (
--     PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
--     PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );
