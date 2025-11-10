package com.pipeline.framework.connectors.console;

import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import com.pipeline.framework.api.strategy.SourceCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Console Source 创建器。
 * <p>
 * 使用策略模式 + Spring 依赖注入，替代 switch case。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class ConsoleSourceCreator implements SourceCreator {
    
    private final Scheduler ioScheduler;

    public ConsoleSourceCreator(@Qualifier("ioScheduler") Scheduler ioScheduler) {
        this.ioScheduler = ioScheduler;
    }

    @Override
    public Mono<DataSource<?>> create(SourceConfig config) {
        return Mono.fromCallable(() -> {
            String name = config.getProperty("name", "console-source");
            return new ConsoleSource(name, config);
        })
        .subscribeOn(ioScheduler);
    }

    @Override
    public String getType() {
        return "console";
    }

    @Override
    public int getOrder() {
        return 100;  // 较低优先级，用于测试
    }
}
