package com.pipeline.framework.connectors.sql;

import com.pipeline.framework.api.component.ComponentMetadata;
import com.pipeline.framework.api.component.ComponentType;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL批量数据输出。
 * <p>
 * 用于批量写入数据到数据库。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class SqlBatchSink implements DataSink<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(SqlBatchSink.class);

    private final ComponentMetadata metadata;
    private final SqlBatchSinkConfig config;
    private final DataSource dataSource;
    
    private volatile boolean running = false;

    public SqlBatchSink(SqlBatchSinkConfig config, DataSource dataSource) {
        this.config = config;
        this.dataSource = dataSource;
        this.metadata = ComponentMetadata.builder()
                .componentId(config.getComponentId())
                .componentName("SqlBatchSink")
                .componentType(ComponentType.SINK)
                .build();
    }

    @Override
    public Mono<Void> sink(Flux<Map<String, Object>> dataStream) {
        return dataStream
                .buffer(config.getBatchSize())
                .flatMap(this::batchInsert)
                .then()
                .doOnSubscribe(s -> {
                    running = true;
                    log.info("SQL Batch Sink started: table={}, batchSize={}", 
                            config.getTableName(), config.getBatchSize());
                })
                .doOnTerminate(() -> {
                    running = false;
                    log.info("SQL Batch Sink completed");
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> batchInsert(List<Map<String, Object>> batch) {
        return Mono.fromRunnable(() -> {
            if (batch.isEmpty()) {
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            
            try {
                conn = dataSource.getConnection();
                conn.setAutoCommit(false);
                
                // 构建INSERT SQL
                String sql = buildInsertSql(batch.get(0));
                stmt = conn.prepareStatement(sql);
                
                for (Map<String, Object> row : batch) {
                    int index = 1;
                    for (String column : config.getColumns()) {
                        stmt.setObject(index++, row.get(column));
                    }
                    stmt.addBatch();
                }
                
                int[] results = stmt.executeBatch();
                conn.commit();
                
                log.debug("SQL Batch Sink inserted {} rows", results.length);
                
            } catch (SQLException e) {
                log.error("SQL Batch Sink error", e);
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        log.error("Rollback failed", ex);
                    }
                }
                throw new RuntimeException("SQL Batch Sink execution failed", e);
            } finally {
                closeResources(stmt, conn);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String buildInsertSql(Map<String, Object> sampleRow) {
        if (config.getInsertSql() != null && !config.getInsertSql().isEmpty()) {
            return config.getInsertSql();
        }
        
        List<String> columns = config.getColumns();
        if (columns == null || columns.isEmpty()) {
            columns = new ArrayList<>(sampleRow.keySet());
        }
        
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(config.getTableName());
        sql.append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append("?, ".repeat(columns.size()));
        sql.setLength(sql.length() - 2); // 移除最后的", "
        sql.append(")");
        
        return sql.toString();
    }

    @Override
    public void start() {
        running = true;
        log.info("SQL Batch Sink started");
    }

    @Override
    public void stop() {
        running = false;
        log.info("SQL Batch Sink stopped");
    }

    @Override
    public ComponentMetadata getMetadata() {
        return metadata;
    }

    @Override
    public SinkConfig getConfig() {
        return config;
    }

    private void closeResources(PreparedStatement stmt, Connection conn) {
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing PreparedStatement", e);
        }
        
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing Connection", e);
        }
    }
}
