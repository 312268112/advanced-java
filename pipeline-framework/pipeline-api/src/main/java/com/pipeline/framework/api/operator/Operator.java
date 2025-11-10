package com.pipeline.framework.api.operator;

import reactor.core.publisher.Flux;

/**
 * 数据转换算子接口。
 * <p>
 * 算子负责对数据流进行转换、过滤、聚合等操作。
 * 所有操作都是响应式的，支持背压和非阻塞。
 * </p>
 *
 * @param <IN>  输入类型
 * @param <OUT> 输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Operator<IN, OUT> {

    /**
     * 应用算子转换。
     * <p>
     * 接收输入流，返回转换后的输出流。
     * 必须保证线程安全和无副作用（除非是有状态算子）。
     * </p>
     *
     * @param input 输入数据流
     * @return 输出数据流
     */
    Flux<OUT> apply(Flux<IN> input);

    /**
     * 获取算子名称。
     *
     * @return 算子名称
     */
    String getName();

    /**
     * 获取算子类型。
     *
     * @return 算子类型
     */
    OperatorType getType();

    /**
     * 判断是否为有状态算子。
     * <p>
     * 有状态算子需要特殊处理（如checkpoint）。
     * </p>
     *
     * @return true如果是有状态算子
     */
    boolean isStateful();

    /**
     * 获取算子配置。
     *
     * @return 算子配置
     */
    OperatorConfig getConfig();

    /**
     * 获取算子并行度。
     *
     * @return 并行度，-1表示使用全局配置
     */
    default int getParallelism() {
        return -1;
    }
}
