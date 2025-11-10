package com.pipeline.framework.api.component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 组件元数据。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ComponentMetadata {
    
    private final String name;
    private final ComponentType type;
    private final Instant createTime;
    private final Map<String, Object> attributes;

    private ComponentMetadata(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.createTime = builder.createTime;
        this.attributes = new HashMap<>(builder.attributes);
    }

    public String getName() {
        return name;
    }

    public ComponentType getType() {
        return type;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private ComponentType type;
        private Instant createTime = Instant.now();
        private Map<String, Object> attributes = new HashMap<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(ComponentType type) {
            this.type = type;
            return this;
        }

        public Builder createTime(Instant createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }

        public ComponentMetadata build() {
            return new ComponentMetadata(this);
        }
    }
}
