package com.pipeline.framework.starter.example;

import com.pipeline.framework.api.connector.*;
import com.pipeline.framework.api.connector.factory.ConnectorFactoryRegistry;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.connectors.jdbc.JdbcConnectorConfig;
import com.pipeline.framework.connectors.jdbc.JdbcConnectorFactory;
import com.pipeline.framework.core.connector.DefaultReaderToSourceAdapter;
import com.pipeline.framework.core.connector.DefaultWriterToSinkAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Connector使用示例。
 * <p>
 * 展示如何使用新架构进行数据ETL处理。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConnectorUsageExample {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorUsageExample.class);

    public static void main(String[] args) {
        try {
            // 示例1：基础用法
            basicUsage();

            // 示例2：工厂模式用法
            factoryUsage();

            // 示例3：适配器用法
            adapterUsage();

            // 示例4：完整ETL流程
            fullEtlPipeline();

        } catch (Exception e) {
            logger.error("Example execution failed", e);
        }
    }

    /**
     * 示例1：基础用法。
     */
    private static void basicUsage() throws Exception {
        logger.info("=== 示例1：基础用法 ===");

        // 创建配置
        JdbcConnectorConfig config = new JdbcConnectorConfig();
        config.setName("my-jdbc-reader");
        config.setUrl("jdbc:mysql://localhost:3306/test");
        config.setUsername("root");
        config.setPassword("password");
        config.setQuerySql("SELECT * FROM users LIMIT 10");
        config.setBatchSize(5);

        // 创建Reader
        ConnectorReader<Map<String, Object>, JdbcConnectorConfig> reader = 
            new com.pipeline.framework.connectors.jdbc.JdbcConnectorReader(config);

        // 使用Reader
        reader.open();
        
        while (reader.hasNext()) {
            var batch = reader.readBatch(5);
            logger.info("Read batch: {} records", batch.size());
            
            for (Map<String, Object> record : batch) {
                logger.info("Record: {}", record);
            }
        }
        
        reader.close();
        logger.info("Total read: {} records", reader.getReadCount());
    }

    /**
     * 示例2：工厂模式用法。
     */
    private static void factoryUsage() throws Exception {
        logger.info("=== 示例2：工厂模式用法 ===");

        // 注册工厂
        ConnectorFactoryRegistry registry = ConnectorFactoryRegistry.getInstance();
        registry.register(ConnectorType.JDBC, new JdbcConnectorFactory());

        // 创建Reader配置
        JdbcConnectorConfig sourceConfig = new JdbcConnectorConfig();
        sourceConfig.setName("source-reader");
        sourceConfig.setUrl("jdbc:mysql://localhost:3306/source_db");
        sourceConfig.setUsername("root");
        sourceConfig.setPassword("password");
        sourceConfig.setQuerySql("SELECT * FROM products WHERE price > 100");

        // 使用工厂创建Reader
        ConnectorReader<Map<String, Object>, JdbcConnectorConfig> reader = 
            registry.createReader(ConnectorType.JDBC, sourceConfig);

        // 创建Writer配置
        JdbcConnectorConfig sinkConfig = new JdbcConnectorConfig();
        sinkConfig.setName("sink-writer");
        sinkConfig.setUrl("jdbc:mysql://localhost:3306/target_db");
        sinkConfig.setUsername("root");
        sinkConfig.setPassword("password");
        sinkConfig.setTableName("high_price_products");

        // 使用工厂创建Writer
        ConnectorWriter<Map<String, Object>, JdbcConnectorConfig> writer = 
            registry.createWriter(ConnectorType.JDBC, sinkConfig);

        // 获取元数据
        logger.info("Reader metadata: {}", reader.getMetadata());
        logger.info("Writer metadata: {}", writer.getMetadata());

        // 清理
        registry.clear();
    }

    /**
     * 示例3：适配器用法。
     */
    private static void adapterUsage() throws Exception {
        logger.info("=== 示例3：适配器用法 ===");

        // 创建Connector
        JdbcConnectorConfig config = new JdbcConnectorConfig();
        config.setName("adapted-reader");
        config.setUrl("jdbc:mysql://localhost:3306/test");
        config.setUsername("root");
        config.setPassword("password");
        config.setQuerySql("SELECT * FROM orders LIMIT 100");

        ConnectorReader<Map<String, Object>, JdbcConnectorConfig> reader = 
            new com.pipeline.framework.connectors.jdbc.JdbcConnectorReader(config);

        // 创建适配器
        DefaultReaderToSourceAdapter<Map<String, Object>, JdbcConnectorConfig> adapter =
            new DefaultReaderToSourceAdapter<>(reader, 20);

        // 获取DataSource
        DataSource<Map<String, Object>> source = adapter.adapt(reader);

        // 启动
        source.start().block();

        // 使用响应式流
        source.read()
            .take(50)
            .doOnNext(data -> logger.info("Received: {}", data))
            .doOnComplete(() -> logger.info("Stream completed"))
            .blockLast();

        // 停止
        source.stop().block();
    }

    /**
     * 示例4：完整ETL流程。
     */
    private static void fullEtlPipeline() throws Exception {
        logger.info("=== 示例4：完整ETL流程 ===");

        // 注册工厂
        ConnectorFactoryRegistry registry = ConnectorFactoryRegistry.getInstance();
        registry.register(ConnectorType.JDBC, new JdbcConnectorFactory());

        // 源配置
        JdbcConnectorConfig sourceConfig = new JdbcConnectorConfig();
        sourceConfig.setName("etl-source");
        sourceConfig.setUrl("jdbc:mysql://localhost:3306/source");
        sourceConfig.setUsername("root");
        sourceConfig.setPassword("password");
        sourceConfig.setQuerySql("SELECT id, name, email, created_at FROM users WHERE status = 'active'");
        sourceConfig.setBatchSize(100);

        // 目标配置
        JdbcConnectorConfig sinkConfig = new JdbcConnectorConfig();
        sinkConfig.setName("etl-sink");
        sinkConfig.setUrl("jdbc:mysql://localhost:3306/target");
        sinkConfig.setUsername("root");
        sinkConfig.setPassword("password");
        sinkConfig.setTableName("migrated_users");

        // 创建Connector
        ConnectorReader<Map<String, Object>, JdbcConnectorConfig> reader = 
            registry.createReader(ConnectorType.JDBC, sourceConfig);
        ConnectorWriter<Map<String, Object>, JdbcConnectorConfig> writer = 
            registry.createWriter(ConnectorType.JDBC, sinkConfig);

        // 创建适配器
        DefaultReaderToSourceAdapter<Map<String, Object>, JdbcConnectorConfig> sourceAdapter =
            new DefaultReaderToSourceAdapter<>(reader, 100);
        DefaultWriterToSinkAdapter<Map<String, Object>, JdbcConnectorConfig> sinkAdapter =
            new DefaultWriterToSinkAdapter<>(writer, 100);

        // 获取Component
        DataSource<Map<String, Object>> source = sourceAdapter.adapt(reader);
        DataSink<Map<String, Object>> sink = sinkAdapter.adapt(writer);

        // 启动
        source.start().block();
        sink.start().block();

        logger.info("Starting ETL pipeline...");

        // 执行ETL
        source.read()
            .doOnNext(data -> logger.debug("Processing: {}", data.get("id")))
            .map(data -> {
                // 数据转换逻辑
                data.put("migrated_at", System.currentTimeMillis());
                data.put("source", "legacy_system");
                return data;
            })
            .filter(data -> data.get("email") != null) // 过滤
            .transform(dataStream -> sink.write(dataStream))
            .doOnError(error -> logger.error("ETL failed", error))
            .doOnSuccess(v -> logger.info("ETL completed successfully"))
            .block();

        // 停止
        sink.stop().block();
        source.stop().block();

        logger.info("ETL pipeline finished");
    }
}
