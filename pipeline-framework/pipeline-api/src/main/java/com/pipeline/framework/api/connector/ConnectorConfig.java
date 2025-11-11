package com.pipeline.framework.api.connector;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Connector配置基类。
 * <p>
 * 所有连接器配置的基类，提供通用配置能力。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public abstract class ConnectorConfig implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 连接器名称
     */
    private String name;

    /**
     * 扩展属性
     */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeout = 30000;

    /**
     * 是否启用重试
     */
    private boolean retryEnabled = true;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    public <T> T getProperty(String key, Class<T> type) {
        Object value = this.properties.get(key);
        return type.cast(value);
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * 验证配置是否有效。
     *
     * @throws IllegalArgumentException 配置无效时抛出
     */
    public abstract void validate() throws IllegalArgumentException;
}
