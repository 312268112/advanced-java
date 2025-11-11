package com.pipeline.framework.connectors.sql;

import com.pipeline.framework.connector.sdk.Connector;
import com.pipeline.framework.connector.sdk.Lifecycle;
import com.pipeline.framework.connector.sdk.Writable;
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
 * <p>
 * 简单实现，不依赖Reactor，只关注JDBC写入逻辑。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class JdbcWriter implements Connector, Writable<Map<String, Object>>, Lifecycle {

    private static final Logger log = LoggerFactory.getLogger(JdbcWriter.class);

    private final DataSource dataSource;
    private final String tableName;
    private final String insertSql;

    private Connection connection;
    private PreparedStatement statement;
    private List<String> columns;
    private long rowCount = 0;

    public JdbcWriter(DataSource dataSource, String tableName) {
        this(dataSource, tableName, null);
    }

    public JdbcWriter(DataSource dataSource, String tableName, String insertSql) {
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
    public void write(List<Map<String, Object>> records) throws Exception {
        if (records == null || records.isEmpty()) {
            return;
        }

        // 第一次写入时初始化
        if (statement == null) {
            initStatement(records.get(0));
        }

        // 批量添加
        for (Map<String, Object> record : records) {
            int index = 1;
            for (String column : columns) {
                statement.setObject(index++, record.get(column));
            }
            statement.addBatch();
        }

        // 执行并提交
        statement.executeBatch();
        connection.commit();
        
        rowCount += records.size();
        log.debug("Written {} records (total: {})", records.size(), rowCount);
    }

    @Override
    public void flush() throws Exception {
        if (connection != null) {
            connection.commit();
        }
    }

    @Override
    public void close() throws Exception {
        log.info("Closing JDBC writer: {} rows written", rowCount);
        
        if (statement != null) statement.close();
        if (connection != null) {
            connection.commit();
            connection.close();
        }
    }

    @Override
    public String name() {
        return "jdbc-writer";
    }

    private void initStatement(Map<String, Object> sampleRecord) throws SQLException {
        if (insertSql != null) {
            statement = connection.prepareStatement(insertSql);
            columns = new ArrayList<>(sampleRecord.keySet());
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
        sql.append("?, ".repeat(columns.size()));
        sql.setLength(sql.length() - 2);
        sql.append(")");
        return sql.toString();
    }
}
