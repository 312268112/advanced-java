package com.pipeline.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipeline.framework.entity.JobInstanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * JobInstance Mapper接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Mapper
public interface JobInstanceMapper extends BaseMapper<JobInstanceEntity> {

    /**
     * 根据实例ID查询。
     *
     * @param instanceId 实例ID
     * @return 实例实体
     */
    @Select("SELECT * FROM pipeline_job_instance WHERE instance_id = #{instanceId}")
    JobInstanceEntity selectByInstanceId(String instanceId);

    /**
     * 查询指定Job的所有实例。
     *
     * @param jobId 任务ID
     * @return 实例列表
     */
    @Select("SELECT * FROM pipeline_job_instance WHERE job_id = #{jobId} ORDER BY start_time DESC")
    List<JobInstanceEntity> selectByJobId(String jobId);

    /**
     * 查询正在运行的实例。
     *
     * @return 实例列表
     */
    @Select("SELECT * FROM pipeline_job_instance WHERE instance_status = 'RUNNING'")
    List<JobInstanceEntity> selectRunningInstances();
}
