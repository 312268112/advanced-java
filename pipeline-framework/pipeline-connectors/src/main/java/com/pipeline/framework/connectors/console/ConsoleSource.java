package com.pipeline.framework.connectors.console;

import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import com.pipeline.framework.api.source.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 控制台数据源（用于测试）。
 * <p>
 * 生成测试数据流，可配置生成频率和数量。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConsoleSource implements DataSource<String> {
    
    private static final Logger log = LoggerFactory.getLogger(ConsoleSource.class);
    
    private final String name;
    private final SourceConfig config;
    private final AtomicLong counter = new AtomicLong(0);

    public ConsoleSource(String name, SourceConfig config) {
        this.name = name;
        this.config = config;
    }

    /**
     * 生成测试数据流。
     * <p>
     * 每隔指定时间生成一条数据，格式为："message-{序号}"
     * </p>
     */
    @Override
    public Flux<String> read() {
        int count = config.getProperty("count", 100);
        long intervalMs = config.getProperty("intervalMs", 100L);
        
        log.info("Console source starting: count={}, intervalMs={}", count, intervalMs);
        
        return Flux.interval(Duration.ofMillis(intervalMs))
            .take(count)
            .map(i -> {
                long seq = counter.incrementAndGet();
                String message = String.format("message-%d", seq);
                log.debug("Generated: {}", message);
                return message;
            })
            .doOnComplete(() -> log.info("Console source completed: {} messages", counter.get()))
            .doOnError(e -> log.error("Console source error", e));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SourceType getType() {
        return SourceType.CUSTOM;
    }

    @Override
    public SourceConfig getConfig() {
        return config;
    }
}
