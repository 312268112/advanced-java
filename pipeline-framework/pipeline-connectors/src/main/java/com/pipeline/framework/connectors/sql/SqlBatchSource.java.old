package com.pipeline.framework.connectors.sql;

import com.pipeline.framework.api.component.ComponentMetadata;
import com.pipeline.framework.api.component.ComponentType;
import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource as JavaxDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * SQL批量数据源。
 * <p>
 * 用于执行大SQL查询，支持多表关联和复杂聚合。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class SqlBatchSource implements DataSource<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(SqlBatchSource.class);

    private final ComponentMetadata metadata;
    private final SqlBatchSourceConfig config;
    private final JavaxDataSource dataSource;
    
    private volatile boolean running = false;

    public SqlBatchSource(SqlBatchSourceConfig config, JavaxDataSource dataSource) {
        this.config = config;
        this.dataSource = dataSource;
        this.metadata = ComponentMetadata.builder()
                .componentId(config.getComponentId())
                .componentName("SqlBatchSource")
                .componentType(ComponentType.SOURCE)
                .build();
    }

    @Override
    public Flux<Map<String, Object>> getDataStream() {
        return Flux.defer(() -> {
            running = true;
            log.info("Starting SQL Batch Source: {}", config.getSql());
            
            return Flux.<Map<String, Object>>create(sink -> {
                Connection conn = null;
                PreparedStatement stmt = null;
                ResultSet rs = null;
                
                try {
                    conn = dataSource.getConnection();
                    conn.setAutoCommit(false);
                    
                    // 设置fetch size优化大结果集查询
                    stmt = conn.prepareStatement(config.getSql());
                    stmt.setFetchSize(config.getFetchSize());
                    
                    if (config.getQueryTimeoutSeconds() > 0) {
                        stmt.setQueryTimeout(config.getQueryTimeoutSeconds());
                    }
                    
                    // 设置查询参数
                    if (config.getParameters() != null && !config.getParameters().isEmpty()) {
                        int index = 1;
                        for (Object param : config.getParameters()) {
                            stmt.setObject(index++, param);
                        }
                    }
                    
                    rs = stmt.executeQuery();
                    int columnCount = rs.getMetaData().getColumnCount();
                    long rowCount = 0;
                    
                    while (rs.next() && running) {
                        Map<String, Object> row = new HashMap<>(columnCount);
                        
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = rs.getMetaData().getColumnLabel(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        
                        sink.next(row);
                        rowCount++;
                        
                        // 日志输出进度
                        if (rowCount % 10000 == 0) {
                            log.debug("SQL Batch Source processed {} rows", rowCount);
                        }
                    }
                    
                    log.info("SQL Batch Source completed: {} rows processed", rowCount);
                    sink.complete();
                    
                } catch (SQLException e) {
                    log.error("SQL Batch Source error", e);
                    sink.error(new RuntimeException("SQL Batch Source execution failed", e));
                } finally {
                    closeResources(rs, stmt, conn);
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    @Override
    public void start() {
        running = true;
        log.info("SQL Batch Source started");
    }

    @Override
    public void stop() {
        running = false;
        log.info("SQL Batch Source stopped");
    }

    @Override
    public ComponentMetadata getMetadata() {
        return metadata;
    }

    @Override
    public SourceConfig getConfig() {
        return config;
    }

    private void closeResources(ResultSet rs, PreparedStatement stmt, Connection conn) {
        try {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing ResultSet", e);
        }
        
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
