package com.pipeline.framework.connectors.jdbc;

import com.pipeline.framework.api.connector.*;
import com.pipeline.framework.api.connector.factory.ConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * JDBC Connector工厂。
 * <p>
 * 使用工厂模式创建JDBC Reader和Writer。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class JdbcConnectorFactory implements ConnectorFactory<Map<String, Object>, JdbcConnectorConfig> {

    private static final Logger logger = LoggerFactory.getLogger(JdbcConnectorFactory.class);

    @Override
    public ConnectorReader<Map<String, Object>, JdbcConnectorConfig> createReader(JdbcConnectorConfig config)
        throws ConnectorException {
        try {
            logger.info("Creating JDBC reader with config: {}", config.getName());
            
            if (config.getQuerySql() == null || config.getQuerySql().trim().isEmpty()) {
                throw new ConnectorException("Query SQL is required for JDBC reader", ConnectorType.JDBC, config.getName());
            }

            JdbcConnectorReader reader = new JdbcConnectorReader(config);
            
            logger.info("JDBC reader created successfully: {}", config.getName());
            return reader;
        } catch (Exception e) {
            throw new ConnectorException("Failed to create JDBC reader", e, ConnectorType.JDBC, config.getName());
        }
    }

    @Override
    public ConnectorWriter<Map<String, Object>, JdbcConnectorConfig> createWriter(JdbcConnectorConfig config)
        throws ConnectorException {
        try {
            logger.info("Creating JDBC writer with config: {}", config.getName());
            
            if (config.getTableName() == null || config.getTableName().trim().isEmpty()) {
                throw new ConnectorException("Table name is required for JDBC writer", ConnectorType.JDBC, config.getName());
            }

            JdbcConnectorWriter writer = new JdbcConnectorWriter(config);
            
            logger.info("JDBC writer created successfully: {}", config.getName());
            return writer;
        } catch (Exception e) {
            throw new ConnectorException("Failed to create JDBC writer", e, ConnectorType.JDBC, config.getName());
        }
    }

    @Override
    public ConnectorType getSupportedType() {
        return ConnectorType.JDBC;
    }

    @Override
    public boolean supports(JdbcConnectorConfig config) {
        return config != null
            && config.getUrl() != null
            && config.getUsername() != null
            && config.getPassword() != null;
    }
}
