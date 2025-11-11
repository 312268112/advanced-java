package com.pipeline.framework.connectors.sql;

import com.pipeline.framework.connector.sdk.BatchReader;
import com.pipeline.framework.connector.sdk.Position;
import com.pipeline.framework.connector.sdk.ReaderMetadata;
import com.pipeline.framework.connector.sdk.Seekable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL批量数据读取器（简单实现，不依赖Reactor）。
 * <p>
 * 实现标准的 BatchReader 和 Seekable 接口，
 * 框架会在需要时将其转换为 Reactor 流。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class SqlBatchSourceReader implements BatchReader<Map<String, Object>>, Seekable {

    private static final Logger log = LoggerFactory.getLogger(SqlBatchSourceReader.class);

    private final SqlBatchSourceConfig config;
    private final DataSource dataSource;

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private boolean hasMore = true;
    private long rowCount = 0;
    private Position currentPosition;

    public SqlBatchSourceReader(DataSource dataSource, SqlBatchSourceConfig config) {
        this.dataSource = dataSource;
        this.config = config;
        this.currentPosition = Position.builder().offset(0).build();
    }

    @Override
    public void open() throws Exception {
        log.info("Opening SQL batch reader: {}", config.getSql());

        connection = dataSource.getConnection();
        connection.setAutoCommit(false);

        statement = connection.prepareStatement(config.getSql());
        statement.setFetchSize(config.getFetchSize());

        if (config.getQueryTimeoutSeconds() > 0) {
            statement.setQueryTimeout(config.getQueryTimeoutSeconds());
        }

        // 设置查询参数
        if (config.getParameters() != null && !config.getParameters().isEmpty()) {
            int index = 1;
            for (Object param : config.getParameters()) {
                statement.setObject(index++, param);
            }
        }

        resultSet = statement.executeQuery();
        log.info("SQL query executed successfully");
    }

    @Override
    public List<Map<String, Object>> readBatch(int batchSize) throws Exception {
        if (!hasMore || resultSet == null) {
            return null;
        }

        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        int columnCount = resultSet.getMetaData().getColumnCount();

        int count = 0;
        while (count < batchSize && resultSet.next()) {
            Map<String, Object> row = new HashMap<>(columnCount);

            for (int i = 1; i <= columnCount; i++) {
                String columnName = resultSet.getMetaData().getColumnLabel(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }

            batch.add(row);
            count++;
            rowCount++;
        }

        // 检查是否还有更多数据
        if (count < batchSize) {
            hasMore = false;
            log.info("SQL batch reader completed: {} total rows processed", rowCount);
        } else if (rowCount % 10000 == 0) {
            log.debug("SQL batch reader progress: {} rows processed", rowCount);
        }

        // 更新位置
        currentPosition = Position.builder().offset(rowCount).build();

        return batch.isEmpty() ? null : batch;
    }

    @Override
    public boolean hasMore() {
        return hasMore;
    }

    @Override
    public void close() {
        log.info("Closing SQL batch reader");

        closeResultSet();
        closeStatement();
        closeConnection();
    }

    @Override
    public void seek(Position position) throws Exception {
        // SQL ResultSet 通常不支持任意位置的 seek
        // 这里可以通过 WHERE 条件或 OFFSET 实现
        // 具体实现取决于数据库类型和查询需求
        log.warn("Seek operation not fully supported for SQL batch reader. Position: {}", position);
    }

    @Override
    public Position getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public boolean supportsSeek() {
        return false;  // SQL ResultSet 一般不支持随机定位
    }

    @Override
    public ReaderMetadata getMetadata() {
        return ReaderMetadata.builder()
                .readerName("SqlBatchSourceReader")
                .supportsBatchRead(true)
                .supportsSeek(false)
                .recommendedBatchSize(config.getFetchSize())
                .build();
    }

    private void closeResultSet() {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.warn("Error closing ResultSet", e);
            }
        }
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
                connection.close();
            } catch (SQLException e) {
                log.warn("Error closing Connection", e);
            }
        }
    }
}
