package com.pipeline.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实体类。
 * <p>
 * 对应数据库表：pipeline_job
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Data
@TableName("pipeline_job")
public class JobEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务唯一标识
     */
    @TableField("job_id")
    private String jobId;

    /**
     * 任务名称
     */
    @TableField("job_name")
    private String jobName;

    /**
     * 任务类型: STREAMING/BATCH
     */
    @TableField("job_type")
    private String jobType;

    /**
     * 任务状态
     */
    @TableField("job_status")
    private String jobStatus;

    /**
     * 任务描述
     */
    @TableField("description")
    private String description;

    /**
     * StreamGraph ID
     */
    @TableField("stream_graph_id")
    private String streamGraphId;

    /**
     * 重启策略
     */
    @TableField("restart_strategy")
    private String restartStrategy;

    /**
     * 最大重启次数
     */
    @TableField("restart_attempts")
    private Integer restartAttempts;

    /**
     * 重启延迟(秒)
     */
    @TableField("restart_delay_seconds")
    private Integer restartDelaySeconds;

    /**
     * 是否启用检查点
     */
    @TableField("checkpoint_enabled")
    private Boolean checkpointEnabled;

    /**
     * 检查点间隔(秒)
     */
    @TableField("checkpoint_interval_seconds")
    private Integer checkpointIntervalSeconds;

    /**
     * Source配置(JSON)
     */
    @TableField("source_config")
    private String sourceConfig;

    /**
     * Operators配置列表(JSON)
     */
    @TableField("operators_config")
    private String operatorsConfig;

    /**
     * Sink配置(JSON)
     */
    @TableField("sink_config")
    private String sinkConfig;

    /**
     * 任务全局配置(JSON)
     */
    @TableField("job_config")
    private String jobConfig;

    /**
     * 创建人
     */
    @TableField("creator")
    private String creator;

    /**
     * 更新人
     */
    @TableField("updater")
    private String updater;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除: 0-否, 1-是
     */
    @TableField("is_deleted")
    @TableLogic
    private Boolean isDeleted;
}
