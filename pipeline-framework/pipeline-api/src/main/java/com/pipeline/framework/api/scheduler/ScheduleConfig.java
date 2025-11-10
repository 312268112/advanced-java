package com.pipeline.framework.api.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 调度配置接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ScheduleConfig {

    /**
     * 获取调度类型。
     *
     * @return 调度类型
     */
    ScheduleType getType();

    /**
     * 获取Cron表达式（针对CRON类型）。
     *
     * @return Cron表达式
     */
    String getCronExpression();

    /**
     * 获取固定间隔（针对FIXED_RATE类型）。
     *
     * @return 固定间隔
     */
    Duration getFixedRate();

    /**
     * 获取固定延迟（针对FIXED_DELAY类型）。
     *
     * @return 固定延迟
     */
    Duration getFixedDelay();

    /**
     * 获取初始延迟。
     *
     * @return 初始延迟
     */
    Duration getInitialDelay();

    /**
     * 获取时区。
     *
     * @return 时区
     */
    ZoneId getTimeZone();

    /**
     * 获取开始时间。
     *
     * @return 开始时间
     */
    Instant getStartTime();

    /**
     * 获取结束时间。
     *
     * @return 结束时间
     */
    Instant getEndTime();

    /**
     * 是否启用调度。
     *
     * @return true如果启用
     */
    boolean isEnabled();

    /**
     * 获取最大执行次数（-1表示无限制）。
     *
     * @return 最大执行次数
     */
    int getMaxExecutions();
}
