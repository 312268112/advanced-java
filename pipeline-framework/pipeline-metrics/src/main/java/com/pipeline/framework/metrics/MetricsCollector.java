package com.pipeline.framework.metrics;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

/**
 * 指标收集器接口。
 * <p>
 * 收集和报告各种运行时指标。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface MetricsCollector {

    /**
     * 记录计数器指标。
     *
     * @param name  指标名称
     * @param value 指标值
     * @param tags  标签
     */
    void recordCounter(String name, long value, Map<String, String> tags);

    /**
     * 记录计时器指标。
     *
     * @param name     指标名称
     * @param duration 时长
     * @param tags     标签
     */
    void recordTimer(String name, Duration duration, Map<String, String> tags);

    /**
     * 记录仪表盘指标。
     *
     * @param name  指标名称
     * @param value 指标值
     * @param tags  标签
     */
    void recordGauge(String name, double value, Map<String, String> tags);

    /**
     * 记录直方图指标。
     *
     * @param name  指标名称
     * @param value 指标值
     * @param tags  标签
     */
    void recordHistogram(String name, double value, Map<String, String> tags);

    /**
     * 获取所有指标快照。
     *
     * @return 指标快照
     */
    Map<String, Object> snapshot();

    /**
     * 定期发送指标。
     *
     * @param interval 发送间隔
     * @return 指标流
     */
    Flux<Map<String, Object>> publishMetrics(Duration interval);
}
