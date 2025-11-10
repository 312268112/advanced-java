package com.pipeline.framework.operators.map;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;
import com.pipeline.framework.api.operator.OperatorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * 映射算子。
 * <p>
 * 将输入数据转换为输出数据，类似于 Stream.map()。
 * </p>
 *
 * @param <IN>  输入类型
 * @param <OUT> 输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class MapOperator<IN, OUT> implements Operator<IN, OUT> {
    
    private static final Logger log = LoggerFactory.getLogger(MapOperator.class);
    
    private final String name;
    private final OperatorConfig config;
    private final Function<IN, OUT> mapper;

    public MapOperator(String name, OperatorConfig config, Function<IN, OUT> mapper) {
        this.name = name;
        this.config = config;
        this.mapper = mapper;
    }

    /**
     * 应用映射逻辑。
     * <p>
     * 使用 Flux.map() 对每个元素进行转换。
     * </p>
     */
    @Override
    public Flux<OUT> apply(Flux<IN> input) {
        log.debug("Map operator starting: {}", name);
        
        return input
            .map(item -> {
                OUT result = mapper.apply(item);
                log.trace("Mapped: {} -> {}", item, result);
                return result;
            })
            .doOnComplete(() -> log.debug("Map operator completed: {}", name))
            .doOnError(e -> log.error("Map operator error: {}", name, e));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public OperatorType getType() {
        return OperatorType.MAP;
    }

    @Override
    public OperatorConfig getConfig() {
        return config;
    }
}
