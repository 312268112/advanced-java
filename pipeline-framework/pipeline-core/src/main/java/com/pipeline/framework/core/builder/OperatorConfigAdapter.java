package com.pipeline.framework.core.builder;

import com.pipeline.framework.api.graph.StreamNode;
import com.pipeline.framework.api.operator.OperatorConfig;
import com.pipeline.framework.api.operator.OperatorType;

import java.util.HashMap;
import java.util.Map;

/**
 * Operator 配置适配器。
 * <p>
 * 将 StreamNode 的配置转换为 OperatorConfig。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class OperatorConfigAdapter implements OperatorConfig {
    
    private final OperatorType type;
    private final Map<String, Object> properties;

    private OperatorConfigAdapter(OperatorType type, Map<String, Object> properties) {
        this.type = type;
        this.properties = new HashMap<>(properties);
    }

    public static OperatorConfig from(StreamNode node) {
        String operatorType = node.getOperatorType();
        return new OperatorConfigAdapter(
            OperatorType.valueOf(operatorType.toUpperCase()),
            node.getConfig()
        );
    }

    @Override
    public OperatorType getType() {
        return type;
    }

    @Override
    public <T> T getProperty(String key) {
        return (T) properties.get(key);
    }

    @Override
    public <T> T getProperty(String key, T defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }

    @Override
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    @Override
    public boolean validate() {
        return type != null;
    }

    @Override
    public int getParallelism() {
        return getProperty("parallelism", 1);
    }

    @Override
    public int getBufferSize() {
        return getProperty("bufferSize", 100);
    }
}
