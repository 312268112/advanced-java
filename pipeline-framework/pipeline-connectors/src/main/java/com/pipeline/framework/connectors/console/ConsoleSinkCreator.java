package com.pipeline.framework.connectors.console;

import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import com.pipeline.framework.api.strategy.SinkCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Console Sink 创建器。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class ConsoleSinkCreator implements SinkCreator {
    
    private final Scheduler ioScheduler;

    public ConsoleSinkCreator(@Qualifier("ioScheduler") Scheduler ioScheduler) {
        this.ioScheduler = ioScheduler;
    }

    @Override
    public Mono<DataSink<?>> create(SinkConfig config) {
        return Mono.fromCallable(() -> {
            String name = config.getProperty("name", "console-sink");
            return new ConsoleSink<>(name, config);
        })
        .subscribeOn(ioScheduler);
    }

    @Override
    public String getType() {
        return "console";
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
