-- =============================================
-- Pipeline Framework - 视图定义
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
FROM pipeline_job j
LEFT JOIN pipeline_job_instance i ON j.job_id = i.job_id
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
FROM pipeline_job_instance i
WHERE i.instance_status = 'RUNNING'
ORDER BY i.start_time DESC;
