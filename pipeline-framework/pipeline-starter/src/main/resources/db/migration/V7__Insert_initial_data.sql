-- =============================================
-- Pipeline Framework - 初始化数据
-- =============================================

-- 插入内置连接器
INSERT INTO `pipeline_connector` (`connector_id`, `connector_name`, `connector_type`, `connector_class`, `version`, `description`, `support_source`, `support_sink`, `is_builtin`, `is_enabled`, `creator`) VALUES
('jdbc-connector', 'JDBC Connector', 'JDBC', 'com.pipeline.framework.connectors.jdbc.JdbcConnector', '1.0.0', 'JDBC数据库连接器，支持MySQL、PostgreSQL、Oracle等', 1, 1, 1, 1, 'system'),
('kafka-connector', 'Kafka Connector', 'KAFKA', 'com.pipeline.framework.connectors.kafka.KafkaConnector', '1.0.0', 'Apache Kafka消息队列连接器', 1, 1, 1, 1, 'system'),
('http-connector', 'HTTP Connector', 'HTTP', 'com.pipeline.framework.connectors.http.HttpConnector', '1.0.0', 'HTTP/HTTPS API连接器', 1, 1, 1, 1, 'system'),
('file-connector', 'File Connector', 'FILE', 'com.pipeline.framework.connectors.file.FileConnector', '1.0.0', '文件系统连接器，支持CSV、JSON、Parquet等格式', 1, 1, 1, 1, 'system'),
('redis-connector', 'Redis Connector', 'REDIS', 'com.pipeline.framework.connectors.redis.RedisConnector', '1.0.0', 'Redis缓存连接器', 1, 1, 1, 1, 'system'),
('elasticsearch-connector', 'Elasticsearch Connector', 'ELASTICSEARCH', 'com.pipeline.framework.connectors.elasticsearch.ElasticsearchConnector', '1.0.0', 'Elasticsearch搜索引擎连接器', 1, 1, 1, 1, 'system');

-- 插入系统配置
INSERT INTO `pipeline_system_config` (`config_key`, `config_value`, `config_type`, `config_group`, `description`) VALUES
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
INSERT INTO `pipeline_alert_rule` (`rule_id`, `rule_name`, `rule_type`, `alert_level`, `condition_expression`, `is_enabled`, `creator`) VALUES
('alert-job-failed', '任务失败告警', 'JOB_FAILED', 'ERROR', 'instance_status == FAILED', 1, 'system'),
('alert-job-timeout', '任务超时告警', 'JOB_TIMEOUT', 'WARNING', 'duration_ms > 3600000', 1, 'system'),
('alert-high-error-rate', '高错误率告警', 'HIGH_ERROR_RATE', 'WARNING', 'error_count / records_read_total > 0.01', 1, 'system'),
('alert-checkpoint-failed', '检查点失败告警', 'CHECKPOINT_FAILED', 'WARNING', 'checkpoint_status == FAILED', 1, 'system');
