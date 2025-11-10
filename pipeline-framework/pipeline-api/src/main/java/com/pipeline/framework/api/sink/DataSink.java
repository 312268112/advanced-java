package com.pipeline.framework.api.sink;

import com.pipeline.framework.api.component.Component;
import com.pipeline.framework.api.component.ComponentType;
import com.pipeline.framework.api.component.LifecycleAware;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 数据接收器接口。
 * <p>
 * 增强的数据接收器接口，继承自 Component，提供统一的抽象。
 * </p>
 *
 * @param <IN> 输入数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface DataSink<IN> extends Component<SinkConfig>, LifecycleAware {

    /**
     * 写入数据流。
     * <p>
     * 消费输入的数据流，写入到目标系统。
     * </p>
     *
     * @param data 输入数据流
     * @return 写入完成的 Mono
     */
    Mono<Void> write(Flux<IN> data);

    /**
     * 批量写入数据流。
     *
     * @param data      输入数据流
     * @param batchSize 批次大小
     * @return 写入完成的 Mono
     */
    default Mono<Void> writeBatch(Flux<IN> data, int batchSize) {
        return write(data.buffer(batchSize).flatMap(Flux::fromIterable));
    }

    /**
     * 获取接收器类型。
     *
     * @return 接收器类型
     */
    SinkType getType();

    @Override
    default ComponentType getComponentType() {
        return ComponentType.SINK;
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
     * 刷新缓冲区。
     *
     * @return 刷新完成的 Mono
     */
    default Mono<Void> flush() {
        return Mono.empty();
    }

    /**
     * 获取输入数据类型。
     *
     * @return 输入类型的 Class
     */
    default Class<IN> getInputType() {
        return null;
    }
}
