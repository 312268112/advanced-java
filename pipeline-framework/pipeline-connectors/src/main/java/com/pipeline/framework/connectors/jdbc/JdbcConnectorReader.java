package com.pipeline.framework.connectors.jdbc;

import com.pipeline.framework.api.connector.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC Connector Reader实现。
 * <p>
 * 实现ReadableConnector接口，提供JDBC数据库读取能力。
 * 不依赖Reactor，可以独立使用。
 * 支持断点续传、进度跟踪、并行读取。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class JdbcConnectorReader extends AbstractJdbcConnector<Map<String, Object>>
    implements ReadableConnector<Map<String, Object>, JdbcConnectorConfig> {

    private PreparedStatement statement;
    private ResultSet resultSet;
    private ResultSetMetaData metaData;
    private long readCount = 0;
    private long totalRows = -1;
    private Object checkpoint;

    public JdbcConnectorReader(JdbcConnectorConfig config) {
        super(config);
    }

    @Override
    protected void doOpen() throws Exception {
        // 创建查询语句
        statement = connection.prepareStatement(
            config.getQuerySql(),
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY
        );
        statement.setFetchSize(config.getFetchSize());
        statement.setQueryTimeout(config.getQueryTimeout());

        // 执行查询
        resultSet = statement.executeQuery();
        metaData = resultSet.getMetaData();

        // 计算总行数（用于进度跟踪）
        calculateTotalRows();
    }

    @Override
    protected void doClose() throws Exception {
        if (resultSet != null && !resultSet.isClosed()) {
            resultSet.close();
        }
        if (statement != null && !statement.isClosed()) {
            statement.close();
        }
    }

    @Override
    public List<Map<String, Object>> readBatch(int batchSize) throws Exception {
        reconnectIfNecessary();

        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        
        int count = 0;
        while (count < batchSize && resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }

            batch.add(row);
            count++;
            readCount++;
        }

        // 更新检查点
        checkpoint = readCount;

        return batch;
    }

    @Override
    public boolean hasNext() {
        try {
            return resultSet != null && !resultSet.isAfterLast();
        } catch (SQLException e) {
            logger.error("Error checking hasNext", e);
            return false;
        }
    }

    @Override
    public double getProgress() {
        if (totalRows <= 0) {
            return -1.0;
        }
        return (double) readCount / totalRows;
    }

    @Override
    public long getReadCount() {
        return readCount;
    }

    @Override
    public Object getCheckpoint() {
        return checkpoint;
    }

    @Override
    public void seekToCheckpoint(Object checkpoint) throws Exception {
        this.checkpoint = checkpoint;
        if (checkpoint instanceof Long) {
            // 跳过已读取的行
            long skipRows = (Long) checkpoint;
            for (long i = 0; i < skipRows && resultSet.next(); i++) {
                // 跳过
            }
            readCount = skipRows;
            logger.info("Seeked to checkpoint: {}", skipRows);
        }
    }

    @Override
    public boolean supportsCheckpoint() {
        return true;
    }

    @Override
    public boolean supportsParallelRead() {
        return false; // JDBC ResultSet不支持并行读取
    }

    @Override
    public ConnectorReader<Map<String, Object>, JdbcConnectorConfig> duplicate() throws ConnectorException {
        try {
            JdbcConnectorReader newReader = new JdbcConnectorReader(config);
            newReader.open();
            return newReader;
        } catch (Exception e) {
            throw new ConnectorException("Failed to duplicate reader", e, getType(), getName());
        }
    }

    @Override
    public String getName() {
        return config.getName() != null ? config.getName() : "jdbc-reader";
    }

    /**
     * 计算总行数（用于进度跟踪）。
     */
    private void calculateTotalRows() {
        try (Statement countStmt = connection.createStatement()) {
            // 从SQL中提取表名（简单实现）
            String countSql = "SELECT COUNT(*) FROM (" + config.getQuerySql() + ") AS temp";
            try (ResultSet rs = countStmt.executeQuery(countSql)) {
                if (rs.next()) {
                    totalRows = rs.getLong(1);
                    logger.info("Total rows: {}", totalRows);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to calculate total rows: {}", e.getMessage());
            totalRows = -1;
        }
    }
}
