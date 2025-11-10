-- =============================================
-- Pipeline Framework - 系统配置和告警相关表
-- =============================================

-- 系统配置表
CREATE TABLE `pipeline_system_config` (
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

-- 告警规则表
CREATE TABLE `pipeline_alert_rule` (
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

-- 告警记录表
CREATE TABLE `pipeline_alert_record` (
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
