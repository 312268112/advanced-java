package com.pipeline.framework.connector.sdk;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 位置信息，用于断点续传。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> data;

    public Position() {
        this.data = new HashMap<>();
    }

    public Position(Map<String, Object> data) {
        this.data = new HashMap<>(data);
    }

    public Position set(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public Object get(String key) {
        return data.get(key);
    }

    public Long getLong(String key) {
        Object value = data.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    public Integer getInt(String key) {
        Object value = data.get(key);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    public Map<String, Object> toMap() {
        return new HashMap<>(data);
    }

    public static Position of(String key, Object value) {
        return new Position().set(key, value);
    }

    @Override
    public String toString() {
        return "Position" + data;
    }
}
