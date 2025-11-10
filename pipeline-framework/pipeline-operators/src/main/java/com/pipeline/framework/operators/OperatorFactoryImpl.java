package com.pipeline.framework.operators;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;
import com.pipeline.framework.api.operator.OperatorType;
import com.pipeline.framework.operators.filter.FilterOperator;
import com.pipeline.framework.operators.map.MapOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Operator工厂实现。
 * <p>
 * 负责根据配置创建各种类型的Operator。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class OperatorFactoryImpl implements OperatorFactory {
    
    private static final Logger log = LoggerFactory.getLogger(OperatorFactoryImpl.class);
    
    // 存储自定义的Operator创建函数
    private final Map<OperatorType, Function<OperatorConfig, Operator<?, ?>>> creators = new HashMap<>();

    public OperatorFactoryImpl() {
        // 注册默认的Operator创建器
        registerDefaultCreators();
    }

    /**
     * 注册默认的Operator创建器。
     */
    private void registerDefaultCreators() {
        // FILTER: 根据配置的条件过滤
        creators.put(OperatorType.FILTER, config -> {
            String name = config.getProperty("name", "filter-operator");
            // 这里简化处理，实际应该根据配置解析具体的过滤条件
            return new FilterOperator<>(name, config, item -> {
                // 示例：过滤掉null或空字符串
                if (item == null) return false;
                if (item instanceof String) {
                    return !((String) item).isEmpty();
                }
                return true;
            });
        });
        
        // MAP: 根据配置的映射函数转换
        creators.put(OperatorType.MAP, config -> {
            String name = config.getProperty("name", "map-operator");
            String expression = config.getProperty("expression", "");
            
            // 这里简化处理，实际应该支持SpEL或其他表达式语言
            return new MapOperator<>(name, config, item -> {
                // 示例：转换为大写
                if (item instanceof String) {
                    return ((String) item).toUpperCase();
                }
                return item;
            });
        });
        
        log.info("Default operator creators registered: {}", creators.keySet());
    }

    @Override
    public Mono<Operator<?, ?>> createOperator(OperatorType type, OperatorConfig config) {
        log.debug("Creating operator: type={}", type);
        
        return Mono.defer(() -> {
            Function<OperatorConfig, Operator<?, ?>> creator = creators.get(type);
            
            if (creator == null) {
                return Mono.error(new IllegalArgumentException(
                    "Unsupported operator type: " + type));
            }
            
            try {
                Operator<?, ?> operator = creator.apply(config);
                log.info("Operator created: {} (type: {})", operator.getName(), type);
                return Mono.just(operator);
            } catch (Exception e) {
                log.error("Failed to create operator: type={}", type, e);
                return Mono.error(e);
            }
        });
    }

    /**
     * 注册自定义Operator创建器。
     *
     * @param type    Operator类型
     * @param creator 创建函数
     */
    public void registerCreator(OperatorType type, 
                                Function<OperatorConfig, Operator<?, ?>> creator) {
        creators.put(type, creator);
        log.info("Custom operator creator registered: {}", type);
    }
}
