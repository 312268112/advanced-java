package com.pipeline.framework.metrics;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * 指标收集器接口。
 * <p>
 * 收集和报告各种运行时指标。
 * 支持响应式API。
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
     * @return 记录完成信号
     */
    Mono<Void> recordCounter(String name, long value, Map<String, String> tags);

    /**
     * 记录计时器指标。
     *
     * @param name     指标名称
     * @param duration 时长
     * @param tags     标签
     * @return 记录完成信号
     */
    Mono<Void> recordTimer(String name, Duration duration, Map<String, String> tags);

    /**
     * 记录仪表盘指标。
     *
     * @param name  指标名称
     * @param value 指标值
     * @param tags  标签
     * @return 记录完成信号
     */
    Mono<Void> recordGauge(String name, double value, Map<String, String> tags);

    /**
     * 记录直方图指标。
     *
     * @param name  指标名称
     * @param value 指标值
     * @param tags  标签
     * @return 记录完成信号
     */
    Mono<Void> recordHistogram(String name, double value, Map<String, String> tags);

    /**
     * 获取所有指标快照。
     *
     * @return 指标快照的Mono
     */
    Mono<Map<String, Object>> snapshot();

    /**
     * 定期发送指标。
     * <p>
     * 按指定间隔发送指标数据流。
     * </p>
     *
     * @param interval 发送间隔
     * @return 指标流
     */
    Flux<Map<String, Object>> publishMetrics(Duration interval);

    /**
     * 清空指标。
     *
     * @return 清空完成信号
     */
    Mono<Void> clear();

    /**
     * 获取指标名称列表。
     *
     * @return 指标名称流
     */
    Flux<String> getMetricNames();
}
