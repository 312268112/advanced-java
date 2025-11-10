package com.etl.framework.api.operator;

import reactor.core.publisher.Flux;

/**
 * 算子接口，负责对数据流进行转换操作。
 * <p>
 * Operator是数据处理的核心组件，可以实现各种数据转换逻辑。
 * 算子分为无状态算子和有状态算子。
 * </p>
 *
 * @param <IN>  输入数据类型
 * @param <OUT> 输出数据类型
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface Operator<IN, OUT> {

    /**
     * 应用转换操作。
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
     *
     * @return true如果是有状态算子，否则返回false
     */
    boolean isStateful();

    /**
     * 获取算子配置。
     *
     * @return 配置对象
     */
    OperatorConfig getConfig();
}
