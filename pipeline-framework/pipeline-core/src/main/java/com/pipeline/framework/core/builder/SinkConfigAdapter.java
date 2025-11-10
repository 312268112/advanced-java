package com.pipeline.framework.core.builder;

import com.pipeline.framework.api.graph.StreamNode;
import com.pipeline.framework.api.sink.SinkConfig;
import com.pipeline.framework.api.sink.SinkType;

import java.util.HashMap;
import java.util.Map;

/**
 * Sink 配置适配器。
 * <p>
 * 将 StreamNode 的配置转换为 SinkConfig。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class SinkConfigAdapter implements SinkConfig {
    
    private final Map<String, Object> properties;

    private SinkConfigAdapter(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }

    public static SinkConfig from(StreamNode node) {
        return new SinkConfigAdapter(node.getConfig());
    }

    @Override
    public SinkType getType() {
        String type = (String) properties.get("type");
        return SinkType.valueOf(type.toUpperCase());
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
    public long getFlushInterval() {
        return getProperty("flushInterval", 1000L);
    }

    @Override
    public boolean isRetryEnabled() {
        return getProperty("retryEnabled", true);
    }

    @Override
    public int getMaxRetries() {
        return getProperty("maxRetries", 3);
    }
}
