package com.pipeline.framework.api.operator;

import com.pipeline.framework.api.component.ComponentType;
import com.pipeline.framework.api.component.StreamingComponent;
import reactor.core.publisher.Flux;

/**
 * 操作算子接口。
 * <p>
 * 增强的算子接口，继承自 StreamingComponent，提供统一的抽象。
 * </p>
 *
 * @param <IN>  输入数据类型
 * @param <OUT> 输出数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Operator<IN, OUT> extends StreamingComponent<IN, OUT, OperatorConfig> {

    /**
     * 应用算子转换。
     * <p>
     * 接受输入流，返回转换后的输出流。
     * </p>
     *
     * @param input 输入数据流
     * @return 输出数据流
     */
    Flux<OUT> apply(Flux<IN> input);

    /**
     * 默认实现：将 apply 委托给 process。
     */
    @Override
    default Flux<OUT> process(Flux<IN> input) {
        return apply(input);
    }

    /**
     * 获取算子类型。
     *
     * @return 算子类型
     */
    OperatorType getType();

    @Override
    default ComponentType getComponentType() {
        return ComponentType.OPERATOR;
    }
}
