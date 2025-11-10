package com.pipeline.framework.core.pipeline;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import reactor.core.publisher.Mono;

/**
 * Pipeline接口，表示完整的数据处理管道。
 * <p>
 * Pipeline = Source → Operators → Sink
 * 所有操作都是响应式的。
 * </p>
 *
 * @param <IN>  输入类型
 * @param <OUT> 输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Pipeline<IN, OUT> {

    /**
     * 获取数据源。
     *
     * @return 数据源
     */
    DataSource<IN> getSource();

    /**
     * 获取算子链。
     *
     * @return 算子链
     */
    OperatorChain<IN, OUT> getOperatorChain();

    /**
     * 获取数据输出。
     *
     * @return 数据输出
     */
    DataSink<OUT> getSink();

    /**
     * 执行Pipeline。
     * <p>
     * 启动整个数据处理流程，返回执行结果的Mono。
     * </p>
     *
     * @return 执行结果
     */
    Mono<PipelineResult> execute();

    /**
     * 停止Pipeline。
     * <p>
     * 优雅地停止Pipeline，等待当前处理中的数据完成。
     * </p>
     *
     * @return 停止完成信号
     */
    Mono<Void> stop();

    /**
     * 强制停止Pipeline。
     * <p>
     * 立即停止Pipeline，可能会丢失部分数据。
     * </p>
     *
     * @return 停止完成信号
     */
    Mono<Void> forceStop();

    /**
     * 判断Pipeline是否正在运行。
     *
     * @return true如果正在运行
     */
    boolean isRunning();

    /**
     * 获取Pipeline名称。
     *
     * @return Pipeline名称
     */
    String getName();
}
