package com.pipeline.framework.connectors.jdbc;

import com.pipeline.framework.api.connector.ConnectorWriter;
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
 * JDBC数据写入器。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class JdbcConnectorWriter implements ConnectorWriter<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(JdbcConnectorWriter.class);

    private final DataSource dataSource;
    private final String tableName;
    private final String insertSql;

    private Connection connection;
    private PreparedStatement statement;
    private List<String> columns;
    private long writeCount = 0;
    private boolean inTransaction = false;

    public JdbcConnectorWriter(DataSource dataSource, String tableName, String insertSql) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.insertSql = insertSql;
    }

    @Override
    public void open() throws Exception {
        log.info("Opening JDBC writer: table={}", tableName);
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
    }

    @Override
    public void write(Map<String, Object> record) throws Exception {
        if (statement == null) {
            initStatement(record);
        }

        int index = 1;
        for (String column : columns) {
            statement.setObject(index++, record.get(column));
        }
        statement.addBatch();
        writeCount++;
    }

    @Override
    public void writeBatch(List<Map<String, Object>> records) throws Exception {
        if (records == null || records.isEmpty()) {
            return;
        }

        if (statement == null) {
            initStatement(records.get(0));
        }

        for (Map<String, Object> record : records) {
            int index = 1;
            for (String column : columns) {
                statement.setObject(index++, record.get(column));
            }
            statement.addBatch();
        }

        int[] results = statement.executeBatch();
        writeCount += results.length;
        
        log.debug("Batch written: {} records (total: {})", results.length, writeCount);
    }

    @Override
    public void flush() throws Exception {
        if (statement != null) {
            statement.executeBatch();
            if (!inTransaction) {
                connection.commit();
            }
        }
    }

    @Override
    public void close() throws Exception {
        log.info("Closing JDBC writer: {} rows written", writeCount);
        
        try {
            flush();
        } catch (Exception e) {
            log.error("Error flushing on close", e);
        }
        
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                log.warn("Error closing Statement", e);
            }
        }
        
        if (connection != null) {
            try {
                if (!inTransaction) {
                    connection.commit();
                }
                connection.close();
            } catch (SQLException e) {
                log.warn("Error closing Connection", e);
            }
        }
    }

    @Override
    public boolean supportsTransaction() {
        return true;
    }

    @Override
    public void beginTransaction() throws Exception {
        inTransaction = true;
        log.debug("Transaction begun");
    }

    @Override
    public void commit() throws Exception {
        if (connection != null) {
            flush();
            connection.commit();
            inTransaction = false;
            log.debug("Transaction committed");
        }
    }

    @Override
    public void rollback() throws Exception {
        if (connection != null) {
            connection.rollback();
            inTransaction = false;
            log.debug("Transaction rolled back");
        }
    }

    @Override
    public long getWriteCount() {
        return writeCount;
    }

    @Override
    public Object saveCheckpoint() throws Exception {
        Map<String, Object> checkpoint = new java.util.HashMap<>();
        checkpoint.put("writeCount", writeCount);
        checkpoint.put("timestamp", System.currentTimeMillis());
        return checkpoint;
    }

    private void initStatement(Map<String, Object> sampleRecord) throws SQLException {
        if (insertSql != null) {
            statement = connection.prepareStatement(insertSql);
            columns = new ArrayList<>(sampleRecord.keySet());
            log.info("Using provided INSERT SQL");
        } else {
            columns = new ArrayList<>(sampleRecord.keySet());
            String sql = buildInsertSql(tableName, columns);
            statement = connection.prepareStatement(sql);
            log.info("Generated INSERT SQL: {}", sql);
        }
    }

    private String buildInsertSql(String table, List<String> columns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(table).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }
}
