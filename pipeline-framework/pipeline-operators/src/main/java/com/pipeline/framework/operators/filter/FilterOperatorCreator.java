package com.pipeline.framework.operators.filter;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;
import com.pipeline.framework.api.strategy.OperatorCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.function.Predicate;

/**
 * Filter Operator 创建器。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class FilterOperatorCreator implements OperatorCreator {
    
    private final Scheduler computeScheduler;

    public FilterOperatorCreator(@Qualifier("computeScheduler") Scheduler computeScheduler) {
        this.computeScheduler = computeScheduler;
    }

    @Override
    public Mono<Operator<?, ?>> create(OperatorConfig config) {
        return Mono.fromCallable(() -> {
            String name = config.getProperty("name", "filter-operator");
            String expression = config.getProperty("expression", "");
            
            // 根据表达式创建 Predicate
            Predicate<Object> predicate = buildPredicate(expression);
            
            return new FilterOperator<>(name, config, predicate);
        })
        .subscribeOn(computeScheduler);
    }

    @Override
    public String getType() {
        return "filter";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    /**
     * 根据表达式构建 Predicate。
     * <p>
     * 这里简化处理，实际应该支持 SpEL 或其他表达式语言。
     * </p>
     */
    private Predicate<Object> buildPredicate(String expression) {
        if (expression.isEmpty()) {
            // 默认：过滤 null 和空字符串
            return item -> {
                if (item == null) return false;
                if (item instanceof String) {
                    return !((String) item).isEmpty();
                }
                return true;
            };
        }
        
        // TODO: 实现表达式解析（SpEL、MVEL 等）
        return item -> true;
    }
}
