package com.etl.framework.api.scheduler;

/**
 * 调度策略接口。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface SchedulePolicy {

    /**
     * 获取调度类型。
     *
     * @return 调度类型
     */
    ScheduleType getScheduleType();

    /**
     * 获取Cron表达式（仅Cron调度适用）。
     *
     * @return Cron表达式
     */
    String getCronExpression();
}
