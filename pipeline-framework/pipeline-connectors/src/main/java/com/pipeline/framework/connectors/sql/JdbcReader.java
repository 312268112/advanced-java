package com.pipeline.framework.connectors.sql;

import com.pipeline.framework.connector.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * JDBC数据读取器。
 * <p>
 * 简单实现，不依赖Reactor，只关注JDBC读取逻辑。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class JdbcReader implements Connector, Readable<Map<String, Object>>, Seekable, Lifecycle {

    private static final Logger log = LoggerFactory.getLogger(JdbcReader.class);

    private final DataSource dataSource;
    private final String sql;
    private final List<Object> parameters;
    private final int fetchSize;

    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private boolean hasMore = true;
    private long rowCount = 0;

    public JdbcReader(DataSource dataSource, String sql) {
        this(dataSource, sql, Collections.emptyList(), 500);
    }

    public JdbcReader(DataSource dataSource, String sql, List<Object> parameters, int fetchSize) {
        this.dataSource = dataSource;
        this.sql = sql;
        this.parameters = parameters;
        this.fetchSize = fetchSize;
    }

    @Override
    public void open() throws Exception {
        log.info("Opening JDBC reader: {}", sql);
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);

        statement = connection.prepareStatement(sql);
        statement.setFetchSize(fetchSize);

        // 设置参数
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }

        resultSet = statement.executeQuery();
    }

    @Override
    public List<Map<String, Object>> read(int batchSize) throws Exception {
        if (!hasMore) {
            return null;
        }

        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        int columnCount = resultSet.getMetaData().getColumnCount();

        int count = 0;
        while (count < batchSize && resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>(columnCount);
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = resultSet.getMetaData().getColumnLabel(i);
                row.put(columnName, resultSet.getObject(i));
            }
            
            batch.add(row);
            count++;
            rowCount++;
        }

        if (count < batchSize) {
            hasMore = false;
        }

        return batch.isEmpty() ? null : batch;
    }

    @Override
    public boolean hasMore() {
        return hasMore;
    }

    @Override
    public void seek(Position position) throws Exception {
        // JDBC ResultSet不支持随机定位
        throw new UnsupportedOperationException("JDBC ResultSet does not support seek");
    }

    @Override
    public Position currentPosition() {
        return Position.of("rowCount", rowCount);
    }

    @Override
    public void close() throws Exception {
        log.info("Closing JDBC reader: {} rows processed", rowCount);
        
        if (resultSet != null) resultSet.close();
        if (statement != null) statement.close();
        if (connection != null) connection.close();
    }

    @Override
    public String name() {
        return "jdbc-reader";
    }
}
