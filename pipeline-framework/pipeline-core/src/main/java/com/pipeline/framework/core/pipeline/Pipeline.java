package com.pipeline.framework.core.pipeline;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Pipeline 接口。
 * <p>
 * 表示一个完整的数据处理管道：Source → Operators → Sink。
 * 使用泛型提供类型安全。
 * </p>
 *
 * @param <IN>  输入类型
 * @param <OUT> 输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Pipeline<IN, OUT> {

    /**
     * 执行 Pipeline。
     *
     * @return 执行结果的 Mono
     */
    Mono<PipelineResult> execute();

    /**
     * 停止 Pipeline。
     *
     * @return 停止完成的 Mono
     */
    Mono<Void> stop();

    /**
     * 强制停止 Pipeline。
     *
     * @return 强制停止完成的 Mono
     */
    Mono<Void> forceStop();

    /**
     * 是否正在运行。
     *
     * @return 是否运行中
     */
    boolean isRunning();

    /**
     * 获取 Pipeline 名称。
     *
     * @return 名称
     */
    String getName();

    /**
     * 获取 Source。
     *
     * @return Source 实例
     */
    DataSource<IN> getSource();

    /**
     * 获取 Sink。
     *
     * @return Sink 实例
     */
    DataSink<OUT> getSink();

    /**
     * 获取所有 Operators。
     *
     * @return Operators 列表
     */
    List<Operator<?, ?>> getOperators();

    /**
     * 获取已处理的记录数。
     *
     * @return 记录数
     */
    default long getRecordsProcessed() {
        return 0;
    }
}
