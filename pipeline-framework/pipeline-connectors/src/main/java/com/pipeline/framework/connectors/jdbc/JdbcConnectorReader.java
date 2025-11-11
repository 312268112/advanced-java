package com.pipeline.framework.connectors.jdbc;

import com.pipeline.framework.api.connector.ConnectorReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * JDBC数据读取器。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class JdbcConnectorReader implements ConnectorReader<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(JdbcConnectorReader.class);

    private final DataSource dataSource;
    private final String sql;
    private final List<Object> parameters;
    private final int fetchSize;

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private long readCount = 0;
    private long totalRows = -1;

    public JdbcConnectorReader(DataSource dataSource, String sql, List<Object> parameters, int fetchSize) {
        this.dataSource = dataSource;
        this.sql = sql;
        this.parameters = parameters != null ? parameters : Collections.emptyList();
        this.fetchSize = fetchSize;
    }

    @Override
    public void open() throws Exception {
        log.info("Opening JDBC reader");
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);

        statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setFetchSize(fetchSize);

        // 设置参数
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }

        resultSet = statement.executeQuery();
        log.info("JDBC query executed");
    }

    @Override
    public List<Map<String, Object>> readBatch(int batchSize) throws Exception {
        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        int columnCount = resultSet.getMetaData().getColumnCount();

        int count = 0;
        while (count < batchSize && resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>(columnCount);
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = resultSet.getMetaData().getColumnLabel(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }
            
            batch.add(row);
            count++;
            readCount++;
        }

        return batch.isEmpty() ? null : batch;
    }

    @Override
    public boolean hasNext() {
        try {
            return !resultSet.isAfterLast();
        } catch (SQLException e) {
            log.warn("Error checking hasNext", e);
            return false;
        }
    }

    @Override
    public void close() throws Exception {
        log.info("Closing JDBC reader: {} rows read", readCount);
        
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.warn("Error closing ResultSet", e);
            }
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
                connection.close();
            } catch (SQLException e) {
                log.warn("Error closing Connection", e);
            }
        }
    }

    @Override
    public Object getCheckpoint() {
        Map<String, Object> checkpoint = new HashMap<>();
        checkpoint.put("readCount", readCount);
        checkpoint.put("timestamp", System.currentTimeMillis());
        return checkpoint;
    }

    @Override
    public boolean supportsCheckpoint() {
        return false; // JDBC ResultSet不支持随机定位
    }

    @Override
    public double getProgress() {
        if (totalRows > 0) {
            return (double) readCount / totalRows;
        }
        return -1.0;
    }

    @Override
    public long getReadCount() {
        return readCount;
    }
}
