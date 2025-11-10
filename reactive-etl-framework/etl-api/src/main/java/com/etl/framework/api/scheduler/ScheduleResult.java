package com.etl.framework.api.scheduler;

/**
 * 调度结果。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface ScheduleResult {

    /**
     * 是否成功。
     *
     * @return true如果成功，否则返回false
     */
    boolean isSuccess();

    /**
     * 获取消息。
     *
     * @return 消息
     */
    String getMessage();

    /**
     * 获取调度ID。
     *
     * @return 调度ID
     */
    String getScheduleId();
}
