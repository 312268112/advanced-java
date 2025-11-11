package com.pipeline.framework.connectors.sql;

import com.pipeline.framework.connector.sdk.Writer;
import com.pipeline.framework.connector.sdk.WriterMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL批量数据写入器（简单实现，不依赖Reactor）。
 * <p>
 * 实现标准的 Writer 接口，
 * 框架会在需要时将其转换为 Reactor 流消费者。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class SqlBatchSinkWriter implements Writer<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(SqlBatchSinkWriter.class);

    private final SqlBatchSinkConfig config;
    private final DataSource dataSource;

    private Connection connection;
    private PreparedStatement statement;
    private String insertSql;
    private long rowCount = 0;
    private List<Map<String, Object>> buffer;

    public SqlBatchSinkWriter(DataSource dataSource, SqlBatchSinkConfig config) {
        this.dataSource = dataSource;
        this.config = config;
        this.buffer = new ArrayList<>();
    }

    @Override
    public void open() throws Exception {
        log.info("Opening SQL batch writer: table={}", config.getTableName());

        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
    }

    @Override
    public void write(Map<String, Object> record) throws Exception {
        buffer.add(record);

        // 当缓冲区达到批次大小时，执行批量写入
        if (buffer.size() >= config.getBatchSize()) {
            flush();
        }
    }

    @Override
    public void writeBatch(List<Map<String, Object>> records) throws Exception {
        if (records == null || records.isEmpty()) {
            return;
        }

        // 如果没有SQL，使用第一条记录构建
        if (insertSql == null) {
            insertSql = buildInsertSql(records.get(0));
            statement = connection.prepareStatement(insertSql);
        }

        for (Map<String, Object> record : records) {
            int index = 1;
            List<String> columns = getColumns(record);

            for (String column : columns) {
                statement.setObject(index++, record.get(column));
            }
            statement.addBatch();
        }

        int[] results = statement.executeBatch();
        connection.commit();

        rowCount += results.length;
        log.debug("SQL batch writer: {} rows written (total: {})", results.length, rowCount);
    }

    @Override
    public void flush() throws Exception {
        if (buffer.isEmpty()) {
            return;
        }

        writeBatch(new ArrayList<>(buffer));
        buffer.clear();
    }

    @Override
    public void close() {
        try {
            // 写入剩余的缓冲数据
            flush();
            log.info("SQL batch writer completed: {} total rows written", rowCount);

        } catch (Exception e) {
            log.error("Error flushing remaining data", e);
        } finally {
            closeStatement();
            closeConnection();
        }
    }

    @Override
    public WriterMetadata getMetadata() {
        return WriterMetadata.builder()
                .writerName("SqlBatchSinkWriter")
                .supportsBatchWrite(true)
                .supportsTransaction(true)
                .recommendedBatchSize(config.getBatchSize())
                .build();
    }

    private String buildInsertSql(Map<String, Object> sampleRecord) {
        // 如果配置中指定了SQL，直接使用
        if (config.getInsertSql() != null && !config.getInsertSql().isEmpty()) {
            return config.getInsertSql();
        }

        // 否则根据列名自动构建
        List<String> columns = getColumns(sampleRecord);

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(config.getTableName());
        sql.append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append("?, ".repeat(columns.size()));
        sql.setLength(sql.length() - 2); // 移除最后的", "
        sql.append(")");

        log.info("Generated INSERT SQL: {}", sql);
        return sql.toString();
    }

    private List<String> getColumns(Map<String, Object> record) {
        // 如果配置中指定了列，使用配置的列
        if (config.getColumns() != null && !config.getColumns().isEmpty()) {
            return config.getColumns();
        }

        // 否则使用记录中的所有键
        return new ArrayList<>(record.keySet());
    }

    private void closeStatement() {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                log.warn("Error closing PreparedStatement", e);
            }
        }
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.commit();  // 最后提交一次
                connection.close();
            } catch (SQLException e) {
                log.warn("Error closing Connection", e);
            }
        }
    }
}
