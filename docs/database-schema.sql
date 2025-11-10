-- =============================================
-- 响应式ETL框架 - 数据库表结构设计
-- 版本: v1.0
-- 创建日期: 2025-11-09
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
    `job_graph_id` VARCHAR(64) COMMENT 'JobGraph ID',
    `parallelism` INT DEFAULT 1 COMMENT '并行度',
    `max_parallelism` INT DEFAULT 128 COMMENT '最大并行度',
    `restart_strategy` VARCHAR(32) DEFAULT 'FIXED_DELAY' COMMENT '重启策略',
    `restart_attempts` INT DEFAULT 3 COMMENT '重启次数',
    `restart_delay_seconds` INT DEFAULT 10 COMMENT '重启延迟(秒)',
    `checkpoint_enabled` TINYINT DEFAULT 1 COMMENT '是否启用检查点: 0-否, 1-是',
    `checkpoint_interval_seconds` INT DEFAULT 60 COMMENT '检查点间隔(秒)',
    `config` JSON COMMENT '任务配置(JSON)',
    `metadata` JSON COMMENT '扩展元数据(JSON)',
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

-- 1.2 任务执行历史表
CREATE TABLE `etl_job_execution` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `execution_id` VARCHAR(64) NOT NULL COMMENT '执行ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `job_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `execution_status` VARCHAR(32) NOT NULL COMMENT '执行状态: RUNNING/COMPLETED/FAILED/CANCELLED',
    `start_time` DATETIME COMMENT '开始时间',
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
    `metrics` JSON COMMENT '执行指标(JSON)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_execution_id` (`execution_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_status` (`execution_status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行历史表';

-- 1.3 任务调度配置表
CREATE TABLE `etl_job_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `schedule_id` VARCHAR(64) NOT NULL COMMENT '调度ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `schedule_type` VARCHAR(32) NOT NULL COMMENT '调度类型: IMMEDIATE/CRON/DEPENDENCY/EVENT',
    `schedule_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `cron_expression` VARCHAR(128) COMMENT 'Cron表达式',
    `timezone` VARCHAR(64) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    `dependency_job_ids` TEXT COMMENT '依赖任务ID列表(逗号分隔)',
    `event_type` VARCHAR(64) COMMENT '事件类型',
    `priority` INT DEFAULT 0 COMMENT '优先级(数字越大优先级越高)',
    `max_concurrent_runs` INT DEFAULT 1 COMMENT '最大并发执行数',
    `next_fire_time` DATETIME COMMENT '下次触发时间',
    `last_fire_time` DATETIME COMMENT '上次触发时间',
    `fire_count` BIGINT DEFAULT 0 COMMENT '触发次数',
    `config` JSON COMMENT '调度配置(JSON)',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_id` (`schedule_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_schedule_type` (`schedule_type`),
    KEY `idx_next_fire_time` (`next_fire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务调度配置表';

-- =============================================
-- 2. 图结构相关表
-- =============================================

-- 2.1 StreamGraph表
CREATE TABLE `etl_stream_graph` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `graph_id` VARCHAR(64) NOT NULL COMMENT '图ID',
    `graph_name` VARCHAR(128) NOT NULL COMMENT '图名称',
    `graph_type` VARCHAR(32) NOT NULL DEFAULT 'STREAM_GRAPH' COMMENT '图类型',
    `job_id` VARCHAR(64) COMMENT '关联任务ID',
    `node_count` INT DEFAULT 0 COMMENT '节点数量',
    `edge_count` INT DEFAULT 0 COMMENT '边数量',
    `graph_json` JSON COMMENT '图结构(JSON)',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_graph_id` (`graph_id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='StreamGraph逻辑图表';

-- 2.2 JobGraph表
CREATE TABLE `etl_job_graph` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `graph_id` VARCHAR(64) NOT NULL COMMENT '图ID',
    `graph_name` VARCHAR(128) NOT NULL COMMENT '图名称',
    `stream_graph_id` VARCHAR(64) COMMENT '源StreamGraph ID',
    `job_id` VARCHAR(64) COMMENT '关联任务ID',
    `vertex_count` INT DEFAULT 0 COMMENT '顶点数量',
    `edge_count` INT DEFAULT 0 COMMENT '边数量',
    `parallelism` INT DEFAULT 1 COMMENT '并行度',
    `graph_json` JSON COMMENT '图结构(JSON)',
    `optimization_info` JSON COMMENT '优化信息(JSON)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_graph_id` (`graph_id`),
    KEY `idx_stream_graph_id` (`stream_graph_id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JobGraph物理图表';

-- 2.3 图节点表
CREATE TABLE `etl_graph_node` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `node_id` VARCHAR(64) NOT NULL COMMENT '节点ID',
    `graph_id` VARCHAR(64) NOT NULL COMMENT '所属图ID',
    `node_name` VARCHAR(128) NOT NULL COMMENT '节点名称',
    `node_type` VARCHAR(32) NOT NULL COMMENT '节点类型: SOURCE/OPERATOR/SINK',
    `operator_type` VARCHAR(64) COMMENT '算子类型: MAP/FILTER/FLATMAP/AGGREGATE/WINDOW等',
    `parallelism` INT DEFAULT 1 COMMENT '并行度',
    `is_chained` TINYINT DEFAULT 0 COMMENT '是否已链接: 0-否, 1-是',
    `chain_head_id` VARCHAR(64) COMMENT '算子链头节点ID',
    `chain_position` INT COMMENT '在算子链中的位置',
    `config` JSON COMMENT '节点配置(JSON)',
    `metadata` JSON COMMENT '节点元数据(JSON)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node_id` (`node_id`),
    KEY `idx_graph_id` (`graph_id`),
    KEY `idx_node_type` (`node_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图节点表';

-- 2.4 图边表
CREATE TABLE `etl_graph_edge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `edge_id` VARCHAR(64) NOT NULL COMMENT '边ID',
    `graph_id` VARCHAR(64) NOT NULL COMMENT '所属图ID',
    `source_node_id` VARCHAR(64) NOT NULL COMMENT '源节点ID',
    `target_node_id` VARCHAR(64) NOT NULL COMMENT '目标节点ID',
    `edge_type` VARCHAR(32) DEFAULT 'FORWARD' COMMENT '边类型: FORWARD/SHUFFLE/BROADCAST',
    `partition_strategy` VARCHAR(32) COMMENT '分区策略',
    `config` JSON COMMENT '边配置(JSON)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_edge_id` (`edge_id`),
    KEY `idx_graph_id` (`graph_id`),
    KEY `idx_source_node` (`source_node_id`),
    KEY `idx_target_node` (`target_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图边表';

-- =============================================
-- 3. 连接器配置相关表
-- =============================================

-- 3.1 连接器定义表
CREATE TABLE `etl_connector` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `connector_id` VARCHAR(64) NOT NULL COMMENT '连接器ID',
    `connector_name` VARCHAR(128) NOT NULL COMMENT '连接器名称',
    `connector_type` VARCHAR(64) NOT NULL COMMENT '连接器类型: JDBC/KAFKA/HTTP/FILE/CUSTOM',
    `connector_class` VARCHAR(256) NOT NULL COMMENT '连接器实现类',
    `version` VARCHAR(32) COMMENT '版本号',
    `description` TEXT COMMENT '描述',
    `support_source` TINYINT DEFAULT 0 COMMENT '是否支持Source: 0-否, 1-是',
    `support_sink` TINYINT DEFAULT 0 COMMENT '是否支持Sink: 0-否, 1-是',
    `config_schema` JSON COMMENT '配置Schema(JSON Schema)',
    `is_builtin` TINYINT DEFAULT 0 COMMENT '是否内置: 0-否, 1-是',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connector_id` (`connector_id`),
    KEY `idx_connector_type` (`connector_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器定义表';

-- 3.2 连接器配置实例表
CREATE TABLE `etl_connector_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_id` VARCHAR(64) NOT NULL COMMENT '配置ID',
    `config_name` VARCHAR(128) NOT NULL COMMENT '配置名称',
    `connector_id` VARCHAR(64) NOT NULL COMMENT '连接器ID',
    `connector_type` VARCHAR(64) NOT NULL COMMENT '连接器类型',
    `usage_type` VARCHAR(32) NOT NULL COMMENT '用途: SOURCE/SINK',
    `connection_config` JSON NOT NULL COMMENT '连接配置(JSON)',
    `extra_config` JSON COMMENT '扩展配置(JSON)',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_id` (`config_id`),
    KEY `idx_connector_id` (`connector_id`),
    KEY `idx_config_name` (`config_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连接器配置实例表';

-- =============================================
-- 4. 检查点相关表
-- =============================================

-- 4.1 检查点元数据表
CREATE TABLE `etl_checkpoint` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `checkpoint_id` VARCHAR(64) NOT NULL COMMENT '检查点ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `execution_id` VARCHAR(64) NOT NULL COMMENT '执行ID',
    `checkpoint_type` VARCHAR(32) DEFAULT 'PERIODIC' COMMENT '检查点类型: PERIODIC/SAVEPOINT',
    `checkpoint_status` VARCHAR(32) NOT NULL COMMENT '状态: IN_PROGRESS/COMPLETED/FAILED',
    `trigger_time` DATETIME NOT NULL COMMENT '触发时间',
    `complete_time` DATETIME COMMENT '完成时间',
    `duration_ms` BIGINT COMMENT '耗时(毫秒)',
    `state_size_bytes` BIGINT COMMENT '状态大小(字节)',
    `checkpoint_path` VARCHAR(512) COMMENT '检查点存储路径',
    `operator_count` INT COMMENT '算子数量',
    `error_message` TEXT COMMENT '错误信息',
    `metadata` JSON COMMENT '元数据(JSON)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_checkpoint_id` (`checkpoint_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_execution_id` (`execution_id`),
    KEY `idx_trigger_time` (`trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查点元数据表';

-- 4.2 算子状态表
CREATE TABLE `etl_operator_state` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `state_id` VARCHAR(64) NOT NULL COMMENT '状态ID',
    `checkpoint_id` VARCHAR(64) NOT NULL COMMENT '检查点ID',
    `operator_id` VARCHAR(64) NOT NULL COMMENT '算子ID',
    `operator_name` VARCHAR(128) NOT NULL COMMENT '算子名称',
    `state_type` VARCHAR(32) NOT NULL COMMENT '状态类型: VALUE/LIST/MAP',
    `state_name` VARCHAR(128) NOT NULL COMMENT '状态名称',
    `state_size_bytes` BIGINT COMMENT '状态大小(字节)',
    `state_path` VARCHAR(512) COMMENT '状态存储路径',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_state_id` (`state_id`),
    KEY `idx_checkpoint_id` (`checkpoint_id`),
    KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算子状态表';

-- =============================================
-- 5. 监控指标相关表
-- =============================================

-- 5.1 任务指标表
CREATE TABLE `etl_job_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `execution_id` VARCHAR(64) NOT NULL COMMENT '执行ID',
    `metric_time` DATETIME NOT NULL COMMENT '指标时间',
    `records_read_total` BIGINT DEFAULT 0 COMMENT '累计读取记录数',
    `records_processed_total` BIGINT DEFAULT 0 COMMENT '累计处理记录数',
    `records_written_total` BIGINT DEFAULT 0 COMMENT '累计写入记录数',
    `records_read_rate` DECIMAL(20,2) DEFAULT 0 COMMENT '读取速率(记录/秒)',
    `records_processed_rate` DECIMAL(20,2) DEFAULT 0 COMMENT '处理速率(记录/秒)',
    `records_written_rate` DECIMAL(20,2) DEFAULT 0 COMMENT '写入速率(记录/秒)',
    `backpressure_count` BIGINT DEFAULT 0 COMMENT '背压次数',
    `checkpoint_count` INT DEFAULT 0 COMMENT '检查点次数',
    `restart_count` INT DEFAULT 0 COMMENT '重启次数',
    `cpu_usage_percent` DECIMAL(5,2) COMMENT 'CPU使用率',
    `memory_usage_bytes` BIGINT COMMENT '内存使用量(字节)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_execution_id` (`execution_id`),
    KEY `idx_metric_time` (`metric_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务指标表';

-- 5.2 算子指标表
CREATE TABLE `etl_operator_metrics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `execution_id` VARCHAR(64) NOT NULL COMMENT '执行ID',
    `operator_id` VARCHAR(64) NOT NULL COMMENT '算子ID',
    `operator_name` VARCHAR(128) NOT NULL COMMENT '算子名称',
    `metric_time` DATETIME NOT NULL COMMENT '指标时间',
    `records_in` BIGINT DEFAULT 0 COMMENT '输入记录数',
    `records_out` BIGINT DEFAULT 0 COMMENT '输出记录数',
    `records_filtered` BIGINT DEFAULT 0 COMMENT '过滤记录数',
    `processing_time_ms` BIGINT DEFAULT 0 COMMENT '处理耗时(毫秒)',
    `backpressure_time_ms` BIGINT DEFAULT 0 COMMENT '背压时间(毫秒)',
    `error_count` INT DEFAULT 0 COMMENT '错误次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_execution_id` (`execution_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_metric_time` (`metric_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算子指标表';

-- =============================================
-- 6. 系统配置相关表
-- =============================================

-- 6.1 系统配置表
CREATE TABLE `etl_system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(128) NOT NULL COMMENT '配置Key',
    `config_value` TEXT NOT NULL COMMENT '配置Value',
    `config_type` VARCHAR(32) NOT NULL COMMENT '配置类型: STRING/INT/BOOLEAN/JSON',
    `config_group` VARCHAR(64) COMMENT '配置分组',
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
    `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型: JOB_FAILED/HIGH_LATENCY/BACKPRESSURE/CHECKPOINT_FAILED',
    `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型: JOB/OPERATOR',
    `target_id` VARCHAR(64) COMMENT '目标ID(空表示所有)',
    `metric_name` VARCHAR(64) COMMENT '指标名称',
    `condition_operator` VARCHAR(16) COMMENT '条件运算符: >/</=/>=/<=',
    `threshold_value` DECIMAL(20,2) COMMENT '阈值',
    `duration_seconds` INT COMMENT '持续时间(秒)',
    `alert_level` VARCHAR(32) NOT NULL DEFAULT 'WARNING' COMMENT '告警级别: INFO/WARNING/ERROR/CRITICAL',
    `notification_channels` VARCHAR(256) COMMENT '通知渠道(逗号分隔): EMAIL/SMS/WEBHOOK',
    `notification_config` JSON COMMENT '通知配置(JSON)',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    `creator` VARCHAR(64) COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_id` (`rule_id`),
    KEY `idx_rule_type` (`rule_type`),
    KEY `idx_target_type_id` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- 6.3 告警历史表
CREATE TABLE `etl_alert_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alert_id` VARCHAR(64) NOT NULL COMMENT '告警ID',
    `rule_id` VARCHAR(64) NOT NULL COMMENT '规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
    `alert_level` VARCHAR(32) NOT NULL COMMENT '告警级别',
    `job_id` VARCHAR(64) COMMENT '任务ID',
    `operator_id` VARCHAR(64) COMMENT '算子ID',
    `alert_time` DATETIME NOT NULL COMMENT '告警时间',
    `alert_message` TEXT NOT NULL COMMENT '告警消息',
    `current_value` DECIMAL(20,2) COMMENT '当前值',
    `threshold_value` DECIMAL(20,2) COMMENT '阈值',
    `is_resolved` TINYINT DEFAULT 0 COMMENT '是否已解决: 0-否, 1-是',
    `resolve_time` DATETIME COMMENT '解决时间',
    `notification_status` VARCHAR(32) COMMENT '通知状态: PENDING/SENT/FAILED',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_alert_id` (`alert_id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_alert_time` (`alert_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警历史表';

-- =============================================
-- 7. 用户和权限相关表（可选）
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
    `role` VARCHAR(32) DEFAULT 'USER' COMMENT '角色: ADMIN/DEVELOPER/USER',
    `status` VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/LOCKED',
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
    `operation_type` VARCHAR(64) NOT NULL COMMENT '操作类型: CREATE_JOB/UPDATE_JOB/DELETE_JOB/START_JOB/STOP_JOB等',
    `resource_type` VARCHAR(32) NOT NULL COMMENT '资源类型: JOB/CONNECTOR/CONFIG',
    `resource_id` VARCHAR(64) COMMENT '资源ID',
    `operation_desc` TEXT COMMENT '操作描述',
    `request_params` JSON COMMENT '请求参数(JSON)',
    `response_result` TEXT COMMENT '响应结果',
    `operation_status` VARCHAR(32) NOT NULL COMMENT '操作状态: SUCCESS/FAILED',
    `error_message` TEXT COMMENT '错误信息',
    `ip_address` VARCHAR(64) COMMENT 'IP地址',
    `user_agent` VARCHAR(256) COMMENT 'User Agent',
    `operation_time` DATETIME NOT NULL COMMENT '操作时间',
    `duration_ms` BIGINT COMMENT '耗时(毫秒)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_log_id` (`log_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_resource_type_id` (`resource_type`, `resource_id`),
    KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- =============================================
-- 初始化数据
-- =============================================

-- 插入内置连接器
INSERT INTO `etl_connector` (`connector_id`, `connector_name`, `connector_type`, `connector_class`, `version`, `description`, `support_source`, `support_sink`, `is_builtin`, `is_enabled`, `creator`) VALUES
('connector-jdbc', 'JDBC Connector', 'JDBC', 'com.framework.etl.connector.jdbc.JdbcConnector', '1.0.0', 'JDBC数据库连接器，支持MySQL、PostgreSQL等', 1, 1, 1, 1, 'system'),
('connector-kafka', 'Kafka Connector', 'KAFKA', 'com.framework.etl.connector.kafka.KafkaConnector', '1.0.0', 'Kafka消息队列连接器', 1, 1, 1, 1, 'system'),
('connector-http', 'HTTP Connector', 'HTTP', 'com.framework.etl.connector.http.HttpConnector', '1.0.0', 'HTTP API连接器', 1, 1, 1, 1, 'system'),
('connector-file', 'File Connector', 'FILE', 'com.framework.etl.connector.file.FileConnector', '1.0.0', '文件系统连接器，支持本地文件、HDFS、S3等', 1, 1, 1, 1, 'system');

-- 插入默认系统配置
INSERT INTO `etl_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`) VALUES
('system.executor.parallelism', '4', 'INT', 'executor', '默认并行度'),
('system.executor.thread.pool.core.size', '10', 'INT', 'executor', '线程池核心大小'),
('system.executor.thread.pool.max.size', '50', 'INT', 'executor', '线程池最大大小'),
('system.checkpoint.enabled', 'true', 'BOOLEAN', 'checkpoint', '是否启用检查点'),
('system.checkpoint.interval.seconds', '60', 'INT', 'checkpoint', '检查点间隔(秒)'),
('system.checkpoint.timeout.seconds', '10', 'INT', 'checkpoint', '检查点超时时间(秒)'),
('system.checkpoint.storage.type', 'filesystem', 'STRING', 'checkpoint', '检查点存储类型'),
('system.checkpoint.storage.path', '/data/checkpoints', 'STRING', 'checkpoint', '检查点存储路径'),
('system.state.backend', 'memory', 'STRING', 'state', '状态后端类型: memory/rocksdb'),
('system.metrics.enabled', 'true', 'BOOLEAN', 'metrics', '是否启用监控'),
('system.scheduler.thread.pool.size', '20', 'INT', 'scheduler', '调度器线程池大小');

-- 插入默认告警规则
INSERT INTO `etl_alert_rule` (`rule_id`, `rule_name`, `rule_type`, `target_type`, `alert_level`, `is_enabled`, `creator`) VALUES
('rule-job-failed', '任务失败告警', 'JOB_FAILED', 'JOB', 'ERROR', 1, 'system'),
('rule-checkpoint-failed', '检查点失败告警', 'CHECKPOINT_FAILED', 'JOB', 'WARNING', 1, 'system'),
('rule-high-backpressure', '高背压告警', 'BACKPRESSURE', 'OPERATOR', 'WARNING', 1, 'system');

-- =============================================
-- 索引优化建议
-- =============================================
-- 根据实际查询情况，可以添加以下组合索引：
-- ALTER TABLE `etl_job_execution` ADD INDEX `idx_job_status_time` (`job_id`, `execution_status`, `start_time`);
-- ALTER TABLE `etl_job_metrics` ADD INDEX `idx_job_exec_time` (`job_id`, `execution_id`, `metric_time`);
-- ALTER TABLE `etl_checkpoint` ADD INDEX `idx_job_status_trigger` (`job_id`, `checkpoint_status`, `trigger_time`);

-- =============================================
-- 表分区建议（大数据量场景）
-- =============================================
-- 对于指标表、日志表等数据量大且按时间查询的表，建议按时间进行分区：
-- ALTER TABLE `etl_job_metrics` PARTITION BY RANGE (TO_DAYS(metric_time)) (
--     PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
--     PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
--     ...
-- );
