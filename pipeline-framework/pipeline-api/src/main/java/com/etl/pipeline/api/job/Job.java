package com.pipeline.framework.api.job;

import com.pipeline.framework.api.graph.StreamGraph;

import java.time.Instant;

/**
 * ETL任务。
 * <p>
 * Job是ETL任务的最小执行单元，封装了完整的数据处理逻辑。
 * 一个Job在单个实例上完整执行，不会分散到多个节点。
 * </p>
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface Job {

    /**
     * 获取任务ID。
     *
     * @return 任务ID
     */
    String getJobId();

    /**
     * 获取任务名称。
     *
     * @return 任务名称
     */
    String getJobName();

    /**
     * 获取任务类型。
     *
     * @return 任务类型
     */
    JobType getJobType();

    /**
     * 获取任务状态。
     *
     * @return 任务状态
     */
    JobStatus getStatus();

    /**
     * 获取StreamGraph。
     *
     * @return StreamGraph对象
     */
    StreamGraph getStreamGraph();

    /**
     * 获取任务配置。
     *
     * @return 配置对象
     */
    JobConfig getConfig();

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    Instant getCreateTime();

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    Instant getUpdateTime();
}
