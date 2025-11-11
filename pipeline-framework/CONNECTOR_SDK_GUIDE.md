# Pipeline Framework Connector SDK 开发指南

## 概述

Pipeline Framework Connector SDK 提供了简单、统一的接口来开发数据连接器，**不依赖 Reactor**，降低了开发门槛。

### 核心理念

- **简单接口**：使用标准的 `Iterator`、`List` 等 Java 接口
- **无Reactor依赖**：开发者无需了解响应式编程
- **插件化**：动态注册和加载 Connector
- **框架适配**：框架自动将简单接口转换为 Reactor 流

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.pipeline.framework</groupId>
    <artifactId>pipeline-connector-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**注意**：SDK 不依赖 Reactor，只需要 SLF4J 日志。

### 2. 实现 Reader

#### 方式一：实现 Reader 接口（单条读取）

```java
public class MyReader implements Reader<MyData> {
    
    private Connection connection;
    private ResultSet resultSet;
    
    @Override
    public void open() throws Exception {
        // 初始化资源
        connection = createConnection();
        resultSet = connection.executeQuery("SELECT * FROM my_table");
    }
    
    @Override
    public boolean hasNext() {
        try {
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public MyData next() {
        try {
            // 读取一条数据
            return new MyData(
                resultSet.getString("col1"),
                resultSet.getInt("col2")
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void close() {
        // 关闭资源
        closeQuietly(resultSet);
        closeQuietly(connection);
    }
}
```

#### 方式二：实现 BatchReader 接口（批量读取，推荐）

```java
public class MyBatchReader implements BatchReader<MyData> {
    
    private Connection connection;
    private ResultSet resultSet;
    private boolean hasMore = true;
    
    @Override
    public void open() throws Exception {
        connection = createConnection();
        resultSet = connection.executeQuery("SELECT * FROM my_table");
    }
    
    @Override
    public List<MyData> readBatch(int batchSize) throws Exception {
        if (!hasMore) {
            return null;
        }
        
        List<MyData> batch = new ArrayList<>(batchSize);
        int count = 0;
        
        while (count < batchSize && resultSet.next()) {
            batch.add(new MyData(
                resultSet.getString("col1"),
                resultSet.getInt("col2")
            ));
            count++;
        }
        
        // 如果读取的数据少于批次大小，说明没有更多数据了
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
    public void close() {
        closeQuietly(resultSet);
        closeQuietly(connection);
    }
}
```

### 3. 实现 Writer

```java
public class MyWriter implements Writer<MyData> {
    
    private Connection connection;
    private PreparedStatement statement;
    private List<MyData> buffer = new ArrayList<>();
    private int batchSize;
    
    @Override
    public void open() throws Exception {
        connection = createConnection();
        connection.setAutoCommit(false);
        statement = connection.prepareStatement(
            "INSERT INTO my_table (col1, col2) VALUES (?, ?)"
        );
    }
    
    @Override
    public void write(MyData record) throws Exception {
        buffer.add(record);
        
        // 当缓冲区满时，执行批量写入
        if (buffer.size() >= batchSize) {
            flush();
        }
    }
    
    @Override
    public void writeBatch(List<MyData> records) throws Exception {
        for (MyData record : records) {
            statement.setString(1, record.getCol1());
            statement.setInt(2, record.getCol2());
            statement.addBatch();
        }
        
        statement.executeBatch();
        connection.commit();
    }
    
    @Override
    public void flush() throws Exception {
        if (!buffer.isEmpty()) {
            writeBatch(new ArrayList<>(buffer));
            buffer.clear();
        }
    }
    
    @Override
    public void close() {
        try {
            flush();
        } catch (Exception e) {
            // 记录错误
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }
}
```

### 4. 支持断点续传（可选）

如果你的 Connector 支持断点续传，实现 `Seekable` 接口：

```java
public class MySeekableReader implements BatchReader<MyData>, Seekable {
    
    private long currentOffset = 0;
    
    @Override
    public void seek(Position position) throws Exception {
        // 根据位置信息定位
        Long offset = position.getLong("offset");
        if (offset != null) {
            currentOffset = offset;
            // 执行实际的定位操作
            seekToOffset(offset);
        }
    }
    
    @Override
    public Position getCurrentPosition() {
        return Position.builder()
            .offset(currentOffset)
            .build();
    }
    
    @Override
    public boolean supportsSeek() {
        return true;
    }
    
    // ... 其他方法实现
}
```

## 注册 Connector

### 方式一：使用 Spring 自动装配

```java
@Configuration
public class MyConnectorAutoConfiguration {
    
    @Bean
    public ConnectorDescriptor myConnectorDescriptor() {
        return ConnectorDescriptor.builder()
            .name("my-connector")
            .version("1.0.0")
            .description("My custom connector")
            .type(ConnectorDescriptor.ConnectorType.DATABASE)
            .readerClass(MyBatchReader.class)
            .writerClass(MyWriter.class)
            .supportsBatchRead(true)
            .supportsBatchWrite(true)
            .supportsSeek(false)
            .build();
    }
    
    @Bean
    public void registerMyConnector(ConnectorRegistry registry, 
                                     DataSource dataSource) {
        // 注册描述符
        registry.registerConnector(myConnectorDescriptor());
        
        // 注册 Reader 工厂
        registry.registerReaderFactory("my-connector", config -> {
            MyConfig myConfig = (MyConfig) config;
            return new MyBatchReader(dataSource, myConfig);
        });
        
        // 注册 Writer 工厂
        registry.registerWriterFactory("my-connector", config -> {
            MyConfig myConfig = (MyConfig) config;
            return new MyWriter(dataSource, myConfig);
        });
    }
}
```

### 方式二：程序化注册

```java
public class MyConnectorPlugin {
    
    public void register(ConnectorRegistry registry) {
        // 注册描述符
        ConnectorDescriptor descriptor = ConnectorDescriptor.builder()
            .name("my-connector")
            .version("1.0.0")
            .build();
        registry.registerConnector(descriptor);
        
        // 注册工厂
        registry.registerReaderFactory("my-connector", 
            config -> new MyBatchReader(config));
        registry.registerWriterFactory("my-connector", 
            config -> new MyWriter(config));
    }
}
```

## 使用 Connector

框架会自动将你的 Reader/Writer 转换为 Reactor 流：

```java
@Service
public class MyService {
    
    @Autowired
    private ConnectorRegistry registry;
    
    public void runJob() throws Exception {
        // 创建 Reader
        BatchReader<MyData> reader = registry.createBatchReader(
            "my-connector", 
            myConfig
        );
        
        // 框架自动转换为 Flux
        Flux<MyData> dataStream = ReaderAdapter.toFlux(reader, 1000);
        
        // 创建 Writer
        Writer<MyData> writer = registry.createWriter(
            "my-connector", 
            myConfig
        );
        
        // 框架自动处理写入
        WriterAdapter.write(dataStream, writer, 1000)
            .subscribe();
    }
}
```

## 完整示例：MySQL Connector

```java
/**
 * MySQL 批量读取器
 */
public class MySQLBatchReader implements BatchReader<Map<String, Object>>, Seekable {
    
    private final DataSource dataSource;
    private final String sql;
    private final int fetchSize;
    
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private boolean hasMore = true;
    private long rowCount = 0;
    
    public MySQLBatchReader(DataSource dataSource, String sql, int fetchSize) {
        this.dataSource = dataSource;
        this.sql = sql;
        this.fetchSize = fetchSize;
    }
    
    @Override
    public void open() throws Exception {
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        
        statement = connection.prepareStatement(sql);
        statement.setFetchSize(fetchSize);
        
        resultSet = statement.executeQuery();
    }
    
    @Override
    public List<Map<String, Object>> readBatch(int batchSize) throws Exception {
        if (!hasMore) {
            return null;
        }
        
        List<Map<String, Object>> batch = new ArrayList<>(batchSize);
        int columnCount = resultSet.getMetaData().getColumnCount();
        int count = 0;
        
        while (count < batchSize && resultSet.next()) {
            Map<String, Object> row = new HashMap<>(columnCount);
            
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
    public void close() {
        closeQuietly(resultSet);
        closeQuietly(statement);
        closeQuietly(connection);
    }
    
    @Override
    public void seek(Position position) throws Exception {
        // MySQL 不支持随机定位
        throw new UnsupportedOperationException("MySQL ResultSet does not support seek");
    }
    
    @Override
    public Position getCurrentPosition() {
        return Position.builder().offset(rowCount).build();
    }
    
    @Override
    public boolean supportsSeek() {
        return false;
    }
}

/**
 * MySQL 批量写入器
 */
public class MySQLBatchWriter implements Writer<Map<String, Object>> {
    
    private final DataSource dataSource;
    private final String tableName;
    private final int batchSize;
    
    private Connection connection;
    private PreparedStatement statement;
    private String insertSql;
    private List<Map<String, Object>> buffer;
    
    public MySQLBatchWriter(DataSource dataSource, String tableName, int batchSize) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.batchSize = batchSize;
        this.buffer = new ArrayList<>();
    }
    
    @Override
    public void open() throws Exception {
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
    }
    
    @Override
    public void write(Map<String, Object> record) throws Exception {
        buffer.add(record);
        if (buffer.size() >= batchSize) {
            flush();
        }
    }
    
    @Override
    public void writeBatch(List<Map<String, Object>> records) throws Exception {
        if (records.isEmpty()) {
            return;
        }
        
        // 第一次写入时构建 SQL
        if (insertSql == null) {
            List<String> columns = new ArrayList<>(records.get(0).keySet());
            insertSql = buildInsertSql(tableName, columns);
            statement = connection.prepareStatement(insertSql);
        }
        
        // 批量添加
        for (Map<String, Object> record : records) {
            int index = 1;
            for (Object value : record.values()) {
                statement.setObject(index++, value);
            }
            statement.addBatch();
        }
        
        // 执行并提交
        statement.executeBatch();
        connection.commit();
    }
    
    @Override
    public void flush() throws Exception {
        if (!buffer.isEmpty()) {
            writeBatch(new ArrayList<>(buffer));
            buffer.clear();
        }
    }
    
    @Override
    public void close() {
        try {
            flush();
        } catch (Exception e) {
            // 记录错误
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
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
```

## 最佳实践

### 1. 使用批量接口

批量接口（BatchReader/writeBatch）性能更好：

```java
// ✅ 推荐：批量读取
public class MyBatchReader implements BatchReader<Data> {
    @Override
    public List<Data> readBatch(int batchSize) {
        // 一次读取多条
    }
}

// ❌ 不推荐：单条读取（除非数据源不支持批量）
public class MyReader implements Reader<Data> {
    @Override
    public Data next() {
        // 每次读取一条
    }
}
```

### 2. 合理设置批次大小

```java
// 小数据量
int batchSize = 100;

// 中等数据量
int batchSize = 1000;

// 大数据量
int batchSize = 5000;
```

### 3. 正确处理资源

```java
@Override
public void close() {
    try {
        // 先刷新缓冲
        flush();
    } catch (Exception e) {
        log.error("Error flushing", e);
    } finally {
        // 确保资源被关闭
        closeQuietly(statement);
        closeQuietly(connection);
    }
}
```

### 4. 异常处理

```java
@Override
public List<Data> readBatch(int batchSize) throws Exception {
    try {
        // 读取逻辑
        return batch;
    } catch (SQLException e) {
        // 记录详细的错误信息
        log.error("Error reading batch at offset {}", currentOffset, e);
        throw new ConnectorException("Failed to read batch", e);
    }
}
```

### 5. 日志记录

```java
@Override
public void open() throws Exception {
    log.info("Opening reader: sql={}, fetchSize={}", sql, fetchSize);
    // ...
}

@Override
public List<Data> readBatch(int batchSize) throws Exception {
    // ...
    if (rowCount % 10000 == 0) {
        log.debug("Progress: {} rows processed", rowCount);
    }
    // ...
}

@Override
public void close() {
    log.info("Reader closed: {} total rows processed", rowCount);
    // ...
}
```

## SDK API 参考

### 核心接口

| 接口 | 说明 | 使用场景 |
|------|------|---------|
| `Reader<T>` | 单条读取接口 | 简单数据源 |
| `BatchReader<T>` | 批量读取接口 | 大数据量（推荐） |
| `Writer<T>` | 写入接口 | 所有数据输出 |
| `Seekable` | 可定位接口 | 需要断点续传 |

### 工具类

| 类 | 说明 |
|------|------|
| `Position` | 位置信息容器 |
| `ReaderMetadata` | Reader 元数据 |
| `WriterMetadata` | Writer 元数据 |
| `ConnectorDescriptor` | Connector 描述符 |

### 框架适配器（Core模块）

| 类 | 说明 |
|------|------|
| `ReaderAdapter` | Reader → Flux 适配器 |
| `WriterAdapter` | Writer → Mono 适配器 |
| `ConnectorRegistry` | Connector 注册中心 |

## 常见问题

### Q1: 如何支持参数化查询？

```java
public class ParameterizedReader implements BatchReader<Data> {
    private final List<Object> parameters;
    
    @Override
    public void open() throws Exception {
        statement = connection.prepareStatement(sql);
        int index = 1;
        for (Object param : parameters) {
            statement.setObject(index++, param);
        }
        resultSet = statement.executeQuery();
    }
}
```

### Q2: 如何实现分页读取？

```java
public class PaginatedReader implements BatchReader<Data> {
    private int pageSize = 1000;
    private int currentPage = 0;
    
    @Override
    public List<Data> readBatch(int batchSize) throws Exception {
        String paginatedSql = sql + " LIMIT ? OFFSET ?";
        statement.setInt(1, pageSize);
        statement.setInt(2, currentPage * pageSize);
        currentPage++;
        // ...
    }
}
```

### Q3: 如何处理大对象（BLOB/CLOB）？

```java
// 流式读取大对象
InputStream stream = resultSet.getBinaryStream("large_column");
// 分块处理
byte[] buffer = new byte[4096];
while (stream.read(buffer) != -1) {
    // 处理
}
```

## 总结

使用 Pipeline Connector SDK 开发 Connector 的优势：

1. **简单**：无需了解 Reactor，使用熟悉的 Java 接口
2. **专注**：只关注数据读写逻辑，不关心响应式细节
3. **独立**：作为独立 JAR 发布，无需依赖整个框架
4. **灵活**：支持单条/批量、同步/异步等多种模式
5. **可扩展**：框架提供强大的适配和扩展能力

---

**开始开发你的第一个 Connector 吧！** 🚀
