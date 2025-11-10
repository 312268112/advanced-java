package com.pipeline.framework.connectors.kafka;

import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import com.pipeline.framework.api.strategy.SourceCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Kafka Source 创建器。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class KafkaSourceCreator implements SourceCreator {
    
    private final Scheduler ioScheduler;

    public KafkaSourceCreator(@Qualifier("ioScheduler") Scheduler ioScheduler) {
        this.ioScheduler = ioScheduler;
    }

    @Override
    public Mono<DataSource<?>> create(SourceConfig config) {
        return Mono.fromCallable(() -> {
            String name = config.getProperty("name", "kafka-source");
            return new KafkaSource<>(name, config);
        })
        .subscribeOn(ioScheduler);
    }

    @Override
    public String getType() {
        return "kafka";
    }

    @Override
    public int getOrder() {
        return 10;  // 高优先级
    }
}
