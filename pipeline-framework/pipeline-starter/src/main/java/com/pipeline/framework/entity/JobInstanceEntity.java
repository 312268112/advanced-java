package com.pipeline.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实例实体类。
 * <p>
 * 对应数据库表：pipeline_job_instance
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Data
@TableName("pipeline_job_instance")
public class JobInstanceEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 实例ID
     */
    @TableField("instance_id")
    private String instanceId;

    /**
     * 任务ID
     */
    @TableField("job_id")
    private String jobId;

    /**
     * 任务名称
     */
    @TableField("job_name")
    private String jobName;

    /**
     * 实例状态: RUNNING/COMPLETED/FAILED/CANCELLED
     */
    @TableField("instance_status")
    private String instanceStatus;

    /**
     * 运行主机地址
     */
    @TableField("host_address")
    private String hostAddress;

    /**
     * 进程ID
     */
    @TableField("process_id")
    private String processId;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 执行时长(毫秒)
     */
    @TableField("duration_ms")
    private Long durationMs;

    /**
     * 读取记录数
     */
    @TableField("records_read")
    private Long recordsRead;

    /**
     * 处理记录数
     */
    @TableField("records_processed")
    private Long recordsProcessed;

    /**
     * 写入记录数
     */
    @TableField("records_written")
    private Long recordsWritten;

    /**
     * 过滤记录数
     */
    @TableField("records_filtered")
    private Long recordsFiltered;

    /**
     * 失败记录数
     */
    @TableField("records_failed")
    private Long recordsFailed;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 错误堆栈
     */
    @TableField("error_stack_trace")
    private String errorStackTrace;

    /**
     * 最后检查点ID
     */
    @TableField("last_checkpoint_id")
    private String lastCheckpointId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
