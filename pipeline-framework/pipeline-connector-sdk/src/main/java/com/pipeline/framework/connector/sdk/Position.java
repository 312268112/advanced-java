package com.pipeline.framework.connector.sdk;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 位置信息，用于断点续传。
 * <p>
 * 通用的位置信息容器，可以存储任意键值对。
 * 不同的 Connector 可以存储不同类型的位置信息。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> properties;

    public Position() {
        this.properties = new HashMap<>();
    }

    public Position(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }

    /**
     * 设置属性。
     *
     * @param key   键
     * @param value 值
     * @return this
     */
    public Position set(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    /**
     * 获取属性。
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return properties.get(key);
    }

    /**
     * 获取属性（带默认值）。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 值
     */
    public Object get(String key, Object defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    /**
     * 获取字符串属性。
     *
     * @param key 键
     * @return 值
     */
    public String getString(String key) {
        Object value = properties.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取Long属性。
     *
     * @param key 键
     * @return 值
     */
    public Long getLong(String key) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 获取Integer属性。
     *
     * @param key 键
     * @return 值
     */
    public Integer getInteger(String key) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * 获取所有属性。
     *
     * @return 属性映射
     */
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    /**
     * 检查是否包含某个键。
     *
     * @param key 键
     * @return true 如果包含，false 否则
     */
    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    /**
     * 检查位置是否为空。
     *
     * @return true 如果为空，false 否则
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /**
     * 创建一个新的 Position Builder。
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return Objects.equals(properties, position.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }

    @Override
    public String toString() {
        return "Position{" +
                "properties=" + properties +
                '}';
    }

    /**
     * Position Builder
     */
    public static class Builder {
        private final Map<String, Object> properties = new HashMap<>();

        public Builder set(String key, Object value) {
            properties.put(key, value);
            return this;
        }

        public Builder offset(long offset) {
            return set("offset", offset);
        }

        public Builder partition(int partition) {
            return set("partition", partition);
        }

        public Builder timestamp(long timestamp) {
            return set("timestamp", timestamp);
        }

        public Position build() {
            return new Position(properties);
        }
    }
}
