package com.pipeline.framework.connectors.sql;

import com.pipeline.framework.api.sink.SinkConfig;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * SQL批量数据输出配置。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class SqlBatchSinkConfig implements SinkConfig {

    private String componentId;
    private String tableName;
    private List<String> columns;
    private String insertSql;
    private int batchSize = 1000;
    private Map<String, Object> properties;

    public SqlBatchSinkConfig() {
    }

    public SqlBatchSinkConfig(String componentId, String tableName) {
        this.componentId = componentId;
        this.tableName = tableName;
    }

    @Override
    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public String getInsertSql() {
        return insertSql;
    }

    public void setInsertSql(String insertSql) {
        this.insertSql = insertSql;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
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
        private final SqlBatchSinkConfig config = new SqlBatchSinkConfig();

        public Builder componentId(String componentId) {
            config.componentId = componentId;
            return this;
        }

        public Builder tableName(String tableName) {
            config.tableName = tableName;
            return this;
        }

        public Builder columns(List<String> columns) {
            config.columns = columns;
            return this;
        }

        public Builder insertSql(String insertSql) {
            config.insertSql = insertSql;
            return this;
        }

        public Builder batchSize(int batchSize) {
            config.batchSize = batchSize;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            config.properties = properties;
            return this;
        }

        public SqlBatchSinkConfig build() {
            if (config.componentId == null || config.componentId.isEmpty()) {
                throw new IllegalArgumentException("componentId is required");
            }
            if (config.tableName == null || config.tableName.isEmpty()) {
                throw new IllegalArgumentException("tableName is required");
            }
            return config;
        }
    }
}
