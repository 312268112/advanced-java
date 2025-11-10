-- =============================================
-- Pipeline Framework - 图结构相关表
-- =============================================

-- StreamGraph定义表
CREATE TABLE `pipeline_stream_graph` (
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
