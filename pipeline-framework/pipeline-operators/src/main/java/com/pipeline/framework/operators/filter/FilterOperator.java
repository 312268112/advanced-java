package com.pipeline.framework.operators.filter;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;
import com.pipeline.framework.api.operator.OperatorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.function.Predicate;

/**
 * 过滤算子。
 * <p>
 * 根据条件过滤数据，只保留满足条件的记录。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class FilterOperator<T> implements Operator<T, T> {
    
    private static final Logger log = LoggerFactory.getLogger(FilterOperator.class);
    
    private final String name;
    private final OperatorConfig config;
    private final Predicate<T> predicate;

    public FilterOperator(String name, OperatorConfig config, Predicate<T> predicate) {
        this.name = name;
        this.config = config;
        this.predicate = predicate;
    }

    /**
     * 应用过滤逻辑。
     * <p>
     * 使用 Flux.filter() 进行过滤，只传递满足条件的元素。
     * </p>
     */
    @Override
    public Flux<T> apply(Flux<T> input) {
        log.debug("Filter operator starting: {}", name);
        
        return input
            .filter(item -> {
                boolean pass = predicate.test(item);
                if (!pass) {
                    log.trace("Filtered out: {}", item);
                }
                return pass;
            })
            .doOnNext(item -> log.trace("Passed filter: {}", item))
            .doOnComplete(() -> log.debug("Filter operator completed: {}", name))
            .doOnError(e -> log.error("Filter operator error: {}", name, e));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public OperatorType getType() {
        return OperatorType.FILTER;
    }

    @Override
    public OperatorConfig getConfig() {
        return config;
    }
}
