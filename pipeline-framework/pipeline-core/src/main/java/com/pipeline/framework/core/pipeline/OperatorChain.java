package com.pipeline.framework.core.pipeline;

import com.pipeline.framework.api.operator.Operator;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 算子链接口。
 * <p>
 * 将多个算子链接成一个处理链路。
 * 使用响应式流方式处理数据。
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
     * <p>
     * 返回新的算子链，支持链式调用。
     * </p>
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
     * <p>
     * 将输入流依次通过所有算子处理，返回最终输出流。
     * </p>
     *
     * @param input 输入流
     * @return 输出流
     */
    Flux<OUT> execute(Flux<IN> input);

    /**
     * 获取算子链长度。
     *
     * @return 算子数量
     */
    int size();

    /**
     * 判断是否为空链。
     *
     * @return true如果没有算子
     */
    boolean isEmpty();
}
