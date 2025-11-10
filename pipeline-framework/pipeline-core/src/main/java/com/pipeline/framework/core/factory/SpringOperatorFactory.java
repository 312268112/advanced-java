package com.pipeline.framework.core.factory;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;
import com.pipeline.framework.api.strategy.OperatorCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring 管理的 Operator 工厂。
 * <p>
 * 使用策略模式，通过 Spring 自动注入所有 OperatorCreator 实现。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class SpringOperatorFactory {
    
    private static final Logger log = LoggerFactory.getLogger(SpringOperatorFactory.class);
    
    private final Map<String, OperatorCreator> creatorMap = new ConcurrentHashMap<>();

    public SpringOperatorFactory(List<OperatorCreator> creators) {
        for (OperatorCreator creator : creators) {
            String type = creator.getType().toLowerCase();
            creatorMap.put(type, creator);
            log.info("Registered OperatorCreator: type={}, class={}", type, creator.getClass().getSimpleName());
        }
        log.info("Total {} OperatorCreators registered", creatorMap.size());
    }

    public Mono<Operator<?, ?>> createOperator(OperatorConfig config) {
        String type = config.getType().name().toLowerCase();
        
        log.debug("Creating operator: type={}", type);
        
        OperatorCreator creator = creatorMap.get(type);
        if (creator == null) {
            return Mono.error(new IllegalArgumentException(
                "No OperatorCreator found for type: " + type + ". Available types: " + creatorMap.keySet()));
        }
        
        return creator.create(config)
            .doOnSuccess(operator -> log.info("Operator created: name={}, type={}", operator.getName(), type))
            .doOnError(e -> log.error("Failed to create operator: type={}", type, e));
    }

    public void registerCreator(OperatorCreator creator) {
        String type = creator.getType().toLowerCase();
        creatorMap.put(type, creator);
        log.info("Custom OperatorCreator registered: type={}", type);
    }

    public List<String> getSupportedTypes() {
        return List.copyOf(creatorMap.keySet());
    }
}
