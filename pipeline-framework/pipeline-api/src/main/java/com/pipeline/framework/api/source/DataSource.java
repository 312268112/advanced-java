package com.pipeline.framework.api.source;

import com.pipeline.framework.api.component.Component;
import com.pipeline.framework.api.component.ComponentType;
import com.pipeline.framework.api.component.LifecycleAware;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 数据源接口。
 * <p>
 * 增强的数据源接口，继承自 Component，提供统一的抽象。
 * </p>
 *
 * @param <OUT> 输出数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface DataSource<OUT> extends Component<SourceConfig>, LifecycleAware {

    /**
     * 读取数据流。
     * <p>
     * 返回一个 Flux 流，持续产生数据。
     * </p>
     *
     * @return 数据流
     */
    Flux<OUT> read();

    /**
     * 获取数据源类型。
     *
     * @return 数据源类型
     */
    SourceType getType();

    @Override
    default ComponentType getComponentType() {
        return ComponentType.SOURCE;
    }

    @Override
    default Mono<Void> start() {
        return Mono.empty();
    }

    @Override
    default Mono<Void> stop() {
        return Mono.empty();
    }

    /**
     * 获取输出数据类型。
     *
     * @return 输出类型的 Class
     */
    default Class<OUT> getOutputType() {
        return null;
    }
}
