package com.pipeline.framework.core.factory;

import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import com.pipeline.framework.api.strategy.SourceCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source 工厂。
 * <p>
 * 使用策略模式，通过 Spring 自动注入所有 SourceCreator 实现。
 * 不再使用 switch case，每个类型的 Source 都有自己的 Creator。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class SourceFactory {
    
    private static final Logger log = LoggerFactory.getLogger(SourceFactory.class);
    
    private final Map<String, SourceCreator> creatorMap = new ConcurrentHashMap<>();

    /**
     * 构造函数注入所有 SourceCreator。
     * <p>
     * Spring 会自动注入所有实现了 SourceCreator 接口的 Bean。
     * </p>
     *
     * @param creators 所有 SourceCreator 实现
     */
    public SourceFactory(List<SourceCreator> creators) {
        for (SourceCreator creator : creators) {
            String type = creator.getType().toLowerCase();
            creatorMap.put(type, creator);
            log.info("Registered SourceCreator: type={}, class={}", type, creator.getClass().getSimpleName());
        }
        log.info("Total {} SourceCreators registered", creatorMap.size());
    }

    /**
     * 创建 Source 实例。
     *
     * @param config Source 配置
     * @return Source 实例的 Mono
     */
    public Mono<DataSource<?>> createSource(SourceConfig config) {
        String type = config.getType().name().toLowerCase();
        
        log.debug("Creating source: type={}", type);
        
        SourceCreator creator = creatorMap.get(type);
        if (creator == null) {
            return Mono.error(new IllegalArgumentException(
                "No SourceCreator found for type: " + type + ". Available types: " + creatorMap.keySet()));
        }
        
        return creator.create(config)
            .doOnSuccess(source -> log.info("Source created: name={}, type={}", source.getName(), type))
            .doOnError(e -> log.error("Failed to create source: type={}", type, e));
    }

    /**
     * 注册自定义 SourceCreator。
     *
     * @param creator 创建器
     */
    public void registerCreator(SourceCreator creator) {
        String type = creator.getType().toLowerCase();
        creatorMap.put(type, creator);
        log.info("Custom SourceCreator registered: type={}", type);
    }

    /**
     * 获取所有支持的类型。
     *
     * @return 类型列表
     */
    public List<String> getSupportedTypes() {
        return List.copyOf(creatorMap.keySet());
    }
}
