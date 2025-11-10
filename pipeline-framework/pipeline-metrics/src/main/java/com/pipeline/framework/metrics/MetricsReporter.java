package com.pipeline.framework.metrics;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 指标报告器接口。
 * <p>
 * 将指标发送到外部监控系统。
 * 支持响应式API。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface MetricsReporter {

    /**
     * 报告指标。
     * <p>
     * 异步发送指标到监控系统。
     * </p>
     *
     * @param metrics 指标数据
     * @return 报告完成信号
     */
    Mono<Void> report(Map<String, Object> metrics);

    /**
     * 初始化报告器。
     *
     * @return 初始化完成信号
     */
    Mono<Void> initialize();

    /**
     * 关闭报告器。
     * <p>
     * 优雅地关闭报告器，刷新所有缓冲的指标。
     * </p>
     *
     * @return 关闭完成信号
     */
    Mono<Void> close();

    /**
     * 获取报告器类型。
     *
     * @return 报告器类型
     */
    String getType();

    /**
     * 健康检查。
     * <p>
     * 检查报告器是否正常工作。
     * </p>
     *
     * @return 健康状态
     */
    Mono<Boolean> healthCheck();

    /**
     * 刷新缓冲区。
     * <p>
     * 强制刷新所有缓冲的指标。
     * </p>
     *
     * @return 刷新完成信号
     */
    Mono<Void> flush();
}
