package com.pipeline.framework.connectors.sql;

import com.pipeline.framework.api.source.SourceConfig;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * SQL批量数据源配置。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class SqlBatchSourceConfig implements SourceConfig {

    private String componentId;
    private String sql;
    private List<Object> parameters;
    private int fetchSize = 500;
    private int queryTimeoutSeconds = 300;
    private Map<String, Object> properties;

    public SqlBatchSourceConfig() {
    }

    public SqlBatchSourceConfig(String componentId, String sql) {
        this.componentId = componentId;
        this.sql = sql;
    }

    @Override
    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<Object> getParameters() {
        return parameters != null ? parameters : Collections.emptyList();
    }

    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public int getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties != null ? properties : Collections.emptyMap();
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SqlBatchSourceConfig config = new SqlBatchSourceConfig();

        public Builder componentId(String componentId) {
            config.componentId = componentId;
            return this;
        }

        public Builder sql(String sql) {
            config.sql = sql;
            return this;
        }

        public Builder parameters(List<Object> parameters) {
            config.parameters = parameters;
            return this;
        }

        public Builder fetchSize(int fetchSize) {
            config.fetchSize = fetchSize;
            return this;
        }

        public Builder queryTimeoutSeconds(int queryTimeoutSeconds) {
            config.queryTimeoutSeconds = queryTimeoutSeconds;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            config.properties = properties;
            return this;
        }

        public SqlBatchSourceConfig build() {
            if (config.componentId == null || config.componentId.isEmpty()) {
                throw new IllegalArgumentException("componentId is required");
            }
            if (config.sql == null || config.sql.isEmpty()) {
                throw new IllegalArgumentException("sql is required");
            }
            return config;
        }
    }
}
