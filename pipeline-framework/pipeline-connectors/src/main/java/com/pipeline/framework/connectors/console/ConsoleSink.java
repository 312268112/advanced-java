package com.pipeline.framework.connectors.console;

import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import com.pipeline.framework.api.sink.SinkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 控制台数据接收器。
 * <p>
 * 将数据输出到控制台，用于测试和调试。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConsoleSink<T> implements DataSink<T> {
    
    private static final Logger log = LoggerFactory.getLogger(ConsoleSink.class);
    
    private final String name;
    private final SinkConfig config;
    private final AtomicLong counter = new AtomicLong(0);

    public ConsoleSink(String name, SinkConfig config) {
        this.name = name;
        this.config = config;
    }

    /**
     * 写入数据到控制台。
     * <p>
     * 简单地打印每条数据，并统计总数。
     * </p>
     */
    @Override
    public Mono<Void> write(Flux<T> data) {
        log.info("Console sink starting: {}", name);
        
        return data
            .doOnNext(item -> {
                long count = counter.incrementAndGet();
                System.out.println("[" + name + "] [" + count + "] " + item);
                log.debug("Written to console: {}", item);
            })
            .then()
            .doOnSuccess(v -> log.info("Console sink completed: {} records written", counter.get()))
            .doOnError(e -> log.error("Console sink error", e));
    }

    @Override
    public Mono<Void> writeBatch(Flux<T> data, int batchSize) {
        // Console sink 不需要批处理，直接调用 write
        return write(data);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SinkType getType() {
        return SinkType.CONSOLE;
    }

    @Override
    public SinkConfig getConfig() {
        return config;
    }
}
