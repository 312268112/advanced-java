package com.pipeline.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.framework.entity.JobEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Job Mapper接口。
 * <p>
 * 基于MyBatis Plus的BaseMapper，提供标准CRUD操作。
 * 注意：这里是同步API，用于配置和元数据查询。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Mapper
public interface JobMapper extends BaseMapper<JobEntity> {

    /**
     * 根据任务ID查询。
     *
     * @param jobId 任务ID
     * @return 任务实体
     */
    @Select("SELECT * FROM pipeline_job WHERE job_id = #{jobId} AND is_deleted = 0")
    JobEntity selectByJobId(String jobId);

    /**
     * 查询指定状态的任务。
     *
     * @param status 任务状态
     * @return 任务列表
     */
    @Select("SELECT * FROM pipeline_job WHERE job_status = #{status} AND is_deleted = 0")
    List<JobEntity> selectByStatus(String status);

    /**
     * 查询所有运行中的任务。
     *
     * @return 任务列表
     */
    @Select("SELECT * FROM pipeline_job WHERE job_status = 'RUNNING' AND is_deleted = 0")
    List<JobEntity> selectRunningJobs();
}
