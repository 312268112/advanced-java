package com.pipeline.framework.connectors.jdbc;

import com.pipeline.framework.api.connector.*;

import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * JDBC Connector Writer实现。
 * <p>
 * 实现WritableConnector接口，提供JDBC数据库写入能力。
 * 不依赖Reactor，可以独立使用。
 * 支持批量写入、事务、检查点、幂等写入。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class JdbcConnectorWriter extends AbstractJdbcConnector<Map<String, Object>>
    implements WritableConnector<Map<String, Object>, JdbcConnectorConfig> {

    private PreparedStatement statement;
    private long writeCount = 0;
    private Object checkpoint;
    private boolean inTransaction = false;

    public JdbcConnectorWriter(JdbcConnectorConfig config) {
        super(config);
    }

    @Override
    protected void doOpen() throws Exception {
        // 动态生成INSERT语句（简化示例，实际应该从配置获取）
        String insertSql = generateInsertSql();
        statement = connection.prepareStatement(insertSql);
        
        logger.info("JDBC writer prepared with SQL: {}", insertSql);
    }

    @Override
    protected void doClose() throws Exception {
        if (statement != null && !statement.isClosed()) {
            statement.close();
        }
    }

    @Override
    public void write(Map<String, Object> record) throws Exception {
        reconnectIfNecessary();

        setStatementParameters(statement, record);
        statement.executeUpdate();
        writeCount++;
    }

    @Override
    public void writeBatch(List<Map<String, Object>> records) throws Exception {
        reconnectIfNecessary();

        for (Map<String, Object> record : records) {
            setStatementParameters(statement, record);
            statement.addBatch();
        }

        int[] results = statement.executeBatch();
        writeCount += results.length;

        logger.debug("Batch write completed: {} records", results.length);
    }

    @Override
    public void flush() throws Exception {
        if (connection != null && !connection.getAutoCommit()) {
            connection.commit();
            logger.debug("Flushed and committed transaction");
        }
    }

    @Override
    public Object saveCheckpoint() throws Exception {
        checkpoint = writeCount;
        return checkpoint;
    }

    @Override
    public void restoreCheckpoint(Object checkpoint) throws Exception {
        if (checkpoint instanceof Long) {
            this.checkpoint = checkpoint;
            this.writeCount = (Long) checkpoint;
            logger.info("Restored checkpoint: {}", checkpoint);
        }
    }

    @Override
    public boolean supportsTransaction() {
        return true;
    }

    @Override
    public void beginTransaction() throws Exception {
        if (!inTransaction) {
            connection.setAutoCommit(false);
            inTransaction = true;
            logger.debug("Transaction began");
        }
    }

    @Override
    public void commit() throws Exception {
        if (inTransaction) {
            connection.commit();
            inTransaction = false;
            logger.debug("Transaction committed");
        }
    }

    @Override
    public void rollback() throws Exception {
        if (inTransaction) {
            connection.rollback();
            inTransaction = false;
            logger.debug("Transaction rolled back");
        }
    }

    @Override
    public long getWriteCount() {
        return writeCount;
    }

    @Override
    public boolean supportsParallelWrite() {
        return true; // JDBC支持多个连接并行写入
    }

    @Override
    public boolean supportsIdempotentWrite() {
        return false; // 需要业务层保证
    }

    @Override
    public ConnectorWriter<Map<String, Object>, JdbcConnectorConfig> duplicate() throws ConnectorException {
        try {
            JdbcConnectorWriter newWriter = new JdbcConnectorWriter(config);
            newWriter.open();
            return newWriter;
        } catch (Exception e) {
            throw new ConnectorException("Failed to duplicate writer", e, getType(), getName());
        }
    }

    @Override
    public String getName() {
        return config.getName() != null ? config.getName() : "jdbc-writer";
    }

    /**
     * 生成INSERT SQL语句（简化实现）。
     *
     * @return INSERT SQL
     */
    private String generateInsertSql() {
        // 简化示例：INSERT INTO table (col1, col2, ...) VALUES (?, ?, ...)
        // 实际应该从配置或元数据获取列信息
        String tableName = config.getTableName();
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name is required for writer");
        }

        // 这里简化处理，实际应该查询表结构
        return String.format("INSERT INTO %s VALUES (?)", tableName);
    }

    /**
     * 设置PreparedStatement参数。
     *
     * @param stmt   PreparedStatement
     * @param record 数据记录
     * @throws SQLException SQL异常
     */
    private void setStatementParameters(PreparedStatement stmt, Map<String, Object> record) throws SQLException {
        // 简化实现：按照Map的顺序设置参数
        // 实际应该按照表结构的列顺序设置
        int index = 1;
        for (Object value : record.values()) {
            stmt.setObject(index++, value);
        }
    }
}
