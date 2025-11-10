-- =============================================
-- Pipeline Framework - 连接器配置相关表
-- =============================================

-- 连接器注册表
CREATE TABLE `pipeline_connector` (
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

-- 数据源配置表
CREATE TABLE `pipeline_datasource` (
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
