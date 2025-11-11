package com.pipeline.framework.api.connector;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Connector元数据。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConnectorMetadata {
    
    private final String name;
    private final ConnectorType type;
    private final String version;
    private final String description;
    private final Instant createTime;
    private final Map<String, Object> attributes;

    private ConnectorMetadata(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.version = builder.version;
        this.description = builder.description;
        this.createTime = builder.createTime;
        this.attributes = new HashMap<>(builder.attributes);
    }

    public String getName() {
        return name;
    }

    public ConnectorType getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
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
        private ConnectorType type;
        private String version = "1.0.0";
        private String description;
        private Instant createTime = Instant.now();
        private Map<String, Object> attributes = new HashMap<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(ConnectorType type) {
            this.type = type;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
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

        public ConnectorMetadata build() {
            return new ConnectorMetadata(this);
        }
    }
}
