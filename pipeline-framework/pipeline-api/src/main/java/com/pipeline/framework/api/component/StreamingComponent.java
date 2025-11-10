package com.pipeline.framework.api.component;

import reactor.core.publisher.Flux;

/**
 * 流式组件接口。
 * <p>
 * 所有处理数据流的组件的基础接口，提供泛型支持。
 * </p>
 *
 * @param <IN>  输入数据类型
 * @param <OUT> 输出数据类型
 * @param <C>   配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface StreamingComponent<IN, OUT, C> extends Component<C> {

    /**
     * 处理数据流。
     * <p>
     * 核心方法，定义了组件如何处理输入流并产生输出流。
     * </p>
     *
     * @param input 输入数据流
     * @return 输出数据流
     */
    Flux<OUT> process(Flux<IN> input);

    /**
     * 获取输入类型。
     *
     * @return 输入类型的 Class
     */
    default Class<IN> getInputType() {
        return null;
    }

    /**
     * 获取输出类型。
     *
     * @return 输出类型的 Class
     */
    default Class<OUT> getOutputType() {
        return null;
    }
}
