package com.pipeline.framework.core.builder;

import com.pipeline.framework.api.graph.StreamNode;
import com.pipeline.framework.api.source.SourceConfig;
import com.pipeline.framework.api.source.SourceType;

import java.util.HashMap;
import java.util.Map;

/**
 * Source 配置适配器。
 * <p>
 * 将 StreamNode 的配置转换为 SourceConfig。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class SourceConfigAdapter implements SourceConfig {
    
    private final Map<String, Object> properties;

    private SourceConfigAdapter(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }

    public static SourceConfig from(StreamNode node) {
        return new SourceConfigAdapter(node.getConfig());
    }

    @Override
    public SourceType getType() {
        String type = (String) properties.get("type");
        return SourceType.valueOf(type.toUpperCase());
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
        return properties.containsKey("type");
    }

    @Override
    public int getBatchSize() {
        return getProperty("batchSize", 100);
    }

    @Override
    public int getParallelism() {
        return getProperty("parallelism", 1);
    }
}
