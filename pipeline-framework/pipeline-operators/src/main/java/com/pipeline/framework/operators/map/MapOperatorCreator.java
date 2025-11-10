package com.pipeline.framework.operators.map;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;
import com.pipeline.framework.api.strategy.OperatorCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.function.Function;

/**
 * Map Operator 创建器。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class MapOperatorCreator implements OperatorCreator {
    
    private final Scheduler computeScheduler;

    public MapOperatorCreator(@Qualifier("computeScheduler") Scheduler computeScheduler) {
        this.computeScheduler = computeScheduler;
    }

    @Override
    public Mono<Operator<?, ?>> create(OperatorConfig config) {
        return Mono.fromCallable(() -> {
            String name = config.getProperty("name", "map-operator");
            String expression = config.getProperty("expression", "");
            
            // 根据表达式创建 Function
            Function<Object, Object> mapper = buildMapper(expression);
            
            return new MapOperator<>(name, config, mapper);
        })
        .subscribeOn(computeScheduler);
    }

    @Override
    public String getType() {
        return "map";
    }

    @Override
    public int getOrder() {
        return 20;
    }

    /**
     * 根据表达式构建 Function。
     * <p>
     * 这里简化处理，实际应该支持 SpEL 或其他表达式语言。
     * </p>
     */
    private Function<Object, Object> buildMapper(String expression) {
        if (expression.isEmpty() || expression.equalsIgnoreCase("toUpperCase")) {
            // 默认：转换为大写
            return item -> {
                if (item instanceof String) {
                    return ((String) item).toUpperCase();
                }
                return item;
            };
        }
        
        // TODO: 实现表达式解析（SpEL、MVEL 等）
        return item -> item;
    }
}
