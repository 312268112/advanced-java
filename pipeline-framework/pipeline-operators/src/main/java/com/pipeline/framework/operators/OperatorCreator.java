package com.pipeline.framework.operators;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;

/**
 * 算子创建器接口。
 * <p>
 * 用于创建自定义算子。
 * </p>
 *
 * @param <IN>  输入类型
 * @param <OUT> 输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@FunctionalInterface
public interface OperatorCreator<IN, OUT> {

    /**
     * 创建算子实例。
     *
     * @param config 算子配置
     * @return 算子实例
     */
    Operator<IN, OUT> create(OperatorConfig config);
}
