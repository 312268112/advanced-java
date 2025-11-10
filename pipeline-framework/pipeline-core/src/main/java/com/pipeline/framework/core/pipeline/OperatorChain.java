package com.pipeline.framework.core.pipeline;

import com.pipeline.framework.api.operator.Operator;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 算子链接口。
 * <p>
 * 将多个算子链接成一个处理链路。
 * </p>
 *
 * @param <IN>  输入类型
 * @param <OUT> 输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface OperatorChain<IN, OUT> {

    /**
     * 添加算子到链中。
     *
     * @param operator 算子
     * @param <T>      算子输出类型
     * @return 新的算子链
     */
    <T> OperatorChain<IN, T> addOperator(Operator<OUT, T> operator);

    /**
     * 获取所有算子。
     *
     * @return 算子列表
     */
    List<Operator<?, ?>> getOperators();

    /**
     * 执行算子链。
     *
     * @param input 输入流
     * @return 输出流
     */
    Flux<OUT> execute(Flux<IN> input);
}
