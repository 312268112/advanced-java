package com.pipeline.framework.core.factory;

import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import com.pipeline.framework.api.strategy.SinkCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sink 工厂。
 * <p>
 * 使用策略模式，自动注入所有 SinkCreator 实现。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class SinkFactory {
    
    private static final Logger log = LoggerFactory.getLogger(SinkFactory.class);
    
    private final Map<String, SinkCreator> creatorMap = new ConcurrentHashMap<>();

    public SinkFactory(List<SinkCreator> creators) {
        for (SinkCreator creator : creators) {
            String type = creator.getType().toLowerCase();
            creatorMap.put(type, creator);
            log.info("Registered SinkCreator: type={}, class={}", type, creator.getClass().getSimpleName());
        }
        log.info("Total {} SinkCreators registered", creatorMap.size());
    }

    public Mono<DataSink<?>> createSink(SinkConfig config) {
        String type = config.getType().name().toLowerCase();
        
        log.debug("Creating sink: type={}", type);
        
        SinkCreator creator = creatorMap.get(type);
        if (creator == null) {
            return Mono.error(new IllegalArgumentException(
                "No SinkCreator found for type: " + type + ". Available types: " + creatorMap.keySet()));
        }
        
        return creator.create(config)
            .doOnSuccess(sink -> log.info("Sink created: name={}, type={}", sink.getName(), type))
            .doOnError(e -> log.error("Failed to create sink: type={}", type, e));
    }

    public void registerCreator(SinkCreator creator) {
        String type = creator.getType().toLowerCase();
        creatorMap.put(type, creator);
        log.info("Custom SinkCreator registered: type={}", type);
    }

    public List<String> getSupportedTypes() {
        return List.copyOf(creatorMap.keySet());
    }
}
