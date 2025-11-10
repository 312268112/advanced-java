package com.pipeline.framework.core.pipeline;

import com.pipeline.framework.api.operator.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 算子链默认实现。
 * <p>
 * 核心：依次应用每个算子，形成响应式流的链式转换。
 * </p>
 *
 * @param <IN>  输入类型
 * @param <OUT> 输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class DefaultOperatorChain<IN, OUT> implements OperatorChain<IN, OUT> {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultOperatorChain.class);
    
    private final List<Operator<?, ?>> operators;

    public DefaultOperatorChain(List<Operator<?, ?>> operators) {
        this.operators = new ArrayList<>(operators);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> OperatorChain<IN, T> addOperator(Operator<OUT, T> operator) {
        List<Operator<?, ?>> newOperators = new ArrayList<>(operators);
        newOperators.add(operator);
        return (OperatorChain<IN, T>) new DefaultOperatorChain<>(newOperators);
    }

    @Override
    public List<Operator<?, ?>> getOperators() {
        return Collections.unmodifiableList(operators);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Flux<OUT> execute(Flux<IN> input) {
        if (operators.isEmpty()) {
            // 没有算子，直接返回输入（类型转换）
            return (Flux<OUT>) input;
        }
        
        log.debug("Executing operator chain with {} operators", operators.size());
        
        // 依次应用每个算子
        Flux<?> current = input;
        
        for (int i = 0; i < operators.size(); i++) {
            Operator<Object, Object> operator = (Operator<Object, Object>) operators.get(i);
            final int index = i;
            
            current = operator.apply((Flux<Object>) current)
                .doOnSubscribe(s -> log.trace("Operator {} started: {}", 
                    index, operator.getName()))
                .doOnComplete(() -> log.trace("Operator {} completed: {}", 
                    index, operator.getName()))
                .doOnError(e -> log.error("Operator {} error: {}", 
                    index, operator.getName(), e));
        }
        
        return (Flux<OUT>) current;
    }

    @Override
    public int size() {
        return operators.size();
    }

    @Override
    public boolean isEmpty() {
        return operators.isEmpty();
    }
}
