package com.pipeline.framework.operators;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;
import reactor.core.publisher.Mono;

/**
 * 算子创建器接口。
 * <p>
 * 用于创建自定义算子。
 * 支持响应式API。
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
     * <p>
     * 异步创建算子。
     * </p>
     *
     * @param config 算子配置
     * @return 算子实例的Mono
     */
    Mono<Operator<IN, OUT>> create(OperatorConfig config);
}
