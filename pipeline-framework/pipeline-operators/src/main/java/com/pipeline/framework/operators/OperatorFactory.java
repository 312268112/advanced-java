package com.pipeline.framework.operators;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;
import com.pipeline.framework.api.operator.OperatorType;

/**
 * 算子工厂接口。
 * <p>
 * 根据类型和配置创建算子实例。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface OperatorFactory {

    /**
     * 创建算子。
     *
     * @param type   算子类型
     * @param config 算子配置
     * @param <IN>   输入类型
     * @param <OUT>  输出类型
     * @return 算子实例
     */
    <IN, OUT> Operator<IN, OUT> createOperator(OperatorType type, OperatorConfig config);

    /**
     * 判断是否支持该类型算子。
     *
     * @param type 算子类型
     * @return true如果支持
     */
    boolean supports(OperatorType type);

    /**
     * 注册自定义算子创建器。
     *
     * @param type    算子类型
     * @param creator 算子创建器
     */
    void register(OperatorType type, OperatorCreator<?, ?> creator);
}
