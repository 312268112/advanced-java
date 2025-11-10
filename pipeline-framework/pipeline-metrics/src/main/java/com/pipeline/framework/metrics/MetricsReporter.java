package com.pipeline.framework.metrics;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 指标报告器接口。
 * <p>
 * 将指标发送到外部监控系统。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface MetricsReporter {

    /**
     * 报告指标。
     *
     * @param metrics 指标数据
     * @return 报告结果
     */
    Mono<Void> report(Map<String, Object> metrics);

    /**
     * 初始化报告器。
     *
     * @return 初始化结果
     */
    Mono<Void> initialize();

    /**
     * 关闭报告器。
     *
     * @return 关闭结果
     */
    Mono<Void> close();

    /**
     * 获取报告器类型。
     *
     * @return 报告器类型
     */
    String getType();
}
