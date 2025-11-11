# Connector 开发指南

本指南帮助开发者创建自定义Connector插件。

---

## 快速开始

### 步骤1：创建配置类

继承`ConnectorConfig`基类，定义Connector特定的配置。

```java
public class MyConnectorConfig extends ConnectorConfig {
    
    private String endpoint;
    private int timeout;
    
    @Override
    public void validate() throws IllegalArgumentException {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("Endpoint is required");
        }
    }
    
    // Getters and Setters
}
```

---

### 步骤2：实现Reader（如果支持读取）

继承抽象基类或直接实现`ReadableConnector`接口。

```java
public class MyConnectorReader extends AbstractMyConnector<MyDataType>
    implements ReadableConnector<MyDataType, MyConnectorConfig> {
    
    public MyConnectorReader(MyConnectorConfig config) {
        super(config);
    }
    
    @Override
    protected void doOpen() throws Exception {
        // 初始化连接、打开资源
    }
    
    @Override
    public List<MyDataType> readBatch(int batchSize) throws Exception {
        // 批量读取数据
        List<MyDataType> batch = new ArrayList<>();
        // ... 读取逻辑
        return batch;
    }
    
    @Override
    public boolean hasNext() {
        // 判断是否还有数据
        return true;
    }
    
    @Override
    protected void doClose() throws Exception {
        // 清理资源
    }
    
    @Override
    public String getName() {
        return config.getName() != null ? config.getName() : "my-reader";
    }
}
```

---

### 步骤3：实现Writer（如果支持写入）

```java
public class MyConnectorWriter extends AbstractMyConnector<MyDataType>
    implements WritableConnector<MyDataType, MyConnectorConfig> {
    
    private long writeCount = 0;
    
    public MyConnectorWriter(MyConnectorConfig config) {
        super(config);
    }
    
    @Override
    protected void doOpen() throws Exception {
        // 初始化连接
    }
    
    @Override
    public void write(MyDataType record) throws Exception {
        // 单条写入
        writeCount++;
    }
    
    @Override
    public void writeBatch(List<MyDataType> records) throws Exception {
        // 批量写入
        for (MyDataType record : records) {
            write(record);
        }
    }
    
    @Override
    public void flush() throws Exception {
        // 刷新缓冲区
    }
    
    @Override
    protected void doClose() throws Exception {
        // 清理资源
    }
    
    @Override
    public long getWriteCount() {
        return writeCount;
    }
    
    @Override
    public String getName() {
        return config.getName() != null ? config.getName() : "my-writer";
    }
}
```

---

### 步骤4：创建工厂类

```java
public class MyConnectorFactory 
    implements ConnectorFactory<MyDataType, MyConnectorConfig> {
    
    @Override
    public ConnectorReader<MyDataType, MyConnectorConfig> createReader(
        MyConnectorConfig config) throws ConnectorException {
        return new MyConnectorReader(config);
    }
    
    @Override
    public ConnectorWriter<MyDataType, MyConnectorConfig> createWriter(
        MyConnectorConfig config) throws ConnectorException {
        return new MyConnectorWriter(config);
    }
    
    @Override
    public ConnectorType getSupportedType() {
        return ConnectorType.CUSTOM; // 或定义新的类型
    }
    
    @Override
    public boolean supports(MyConnectorConfig config) {
        return config != null && config.getEndpoint() != null;
    }
}
```

---

### 步骤5：注册工厂

```java
// 在应用启动时注册
ConnectorFactoryRegistry registry = ConnectorFactoryRegistry.getInstance();
registry.register(ConnectorType.CUSTOM, new MyConnectorFactory());
```

---

## 高级特性

### 1. 实现检查点（断点续传）

```java
@Override
public Object getCheckpoint() {
    // 返回当前位置信息
    return currentOffset;
}

@Override
public void seekToCheckpoint(Object checkpoint) throws Exception {
    // 从检查点恢复
    currentOffset = (Long) checkpoint;
    // 跳转到该位置
}

@Override
public boolean supportsCheckpoint() {
    return true;
}
```

---

### 2. 实现事务

```java
@Override
public boolean supportsTransaction() {
    return true;
}

@Override
public void beginTransaction() throws Exception {
    // 开启事务
    inTransaction = true;
}

@Override
public void commit() throws Exception {
    // 提交事务
    inTransaction = false;
}

@Override
public void rollback() throws Exception {
    // 回滚事务
    inTransaction = false;
}
```

---

### 3. 实现进度跟踪

```java
@Override
public double getProgress() {
    if (totalRecords <= 0) {
        return -1.0;
    }
    return (double) readCount / totalRecords;
}

@Override
public long getReadCount() {
    return readCount;
}
```

---

### 4. 支持并行读/写

```java
@Override
public boolean supportsParallelRead() {
    return true;
}

@Override
public ConnectorReader<MyDataType, MyConnectorConfig> duplicate() 
    throws ConnectorException {
    MyConnectorReader newReader = new MyConnectorReader(config);
    newReader.open();
    return newReader;
}
```

---

## 使用抽象基类（推荐）

创建抽象基类来封装通用逻辑：

```java
public abstract class AbstractMyConnector<T> implements Connector<MyConnectorConfig> {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final MyConnectorConfig config;
    protected Connection connection;
    protected volatile boolean opened = false;
    
    protected AbstractMyConnector(MyConnectorConfig config) {
        this.config = config;
        this.config.validate();
    }
    
    public void open() throws Exception {
        if (opened) {
            return;
        }
        
        logger.info("Opening connector: {}", getName());
        
        // 建立连接（模板方法）
        establishConnection();
        
        // 子类初始化（钩子方法）
        doOpen();
        
        opened = true;
    }
    
    protected abstract void doOpen() throws Exception;
    
    protected void establishConnection() throws Exception {
        // 通用连接逻辑
    }
    
    public void close() throws Exception {
        if (!opened) {
            return;
        }
        
        logger.info("Closing connector: {}", getName());
        
        // 子类清理（钩子方法）
        doClose();
        
        // 关闭连接
        if (connection != null) {
            connection.close();
        }
        
        opened = false;
    }
    
    protected abstract void doClose() throws Exception;
    
    @Override
    public ConnectorType getType() {
        return ConnectorType.CUSTOM;
    }
    
    @Override
    public MyConnectorConfig getConfig() {
        return config;
    }
}
```

---

## 最佳实践

### 1. 资源管理
- ✅ 在`open()`中初始化资源
- ✅ 在`close()`中释放资源
- ✅ 使用try-catch-finally确保资源释放
- ✅ 实现幂等的open和close方法

### 2. 错误处理
- ✅ 抛出有意义的异常信息
- ✅ 使用ConnectorException包装底层异常
- ✅ 记录详细的日志
- ✅ 支持重试机制

### 3. 性能优化
- ✅ 使用批量操作（readBatch/writeBatch）
- ✅ 合理设置批次大小
- ✅ 使用连接池
- ✅ 避免阻塞操作

### 4. 类型安全
- ✅ 使用泛型约束数据类型
- ✅ 在配置类中验证参数
- ✅ 提供类型转换方法

### 5. 可测试性
- ✅ Connector不依赖Reactor，易于单元测试
- ✅ 提供Mock实现
- ✅ 使用依赖注入

---

## 测试示例

```java
public class MyConnectorReaderTest {
    
    @Test
    public void testReadBatch() throws Exception {
        // 准备配置
        MyConnectorConfig config = new MyConnectorConfig();
        config.setEndpoint("test://localhost");
        
        // 创建Reader
        MyConnectorReader reader = new MyConnectorReader(config);
        reader.open();
        
        // 读取数据
        List<MyDataType> batch = reader.readBatch(10);
        
        // 验证
        assertNotNull(batch);
        assertTrue(batch.size() <= 10);
        
        // 清理
        reader.close();
    }
}
```

---

## 完整示例

参考 `pipeline-connectors/jdbc` 包下的JDBC Connector实现：
- `JdbcConnectorConfig`
- `AbstractJdbcConnector`
- `JdbcConnectorReader`
- `JdbcConnectorWriter`
- `JdbcConnectorFactory`

---

## 常见问题

### Q: Connector和Component的区别是什么？
A: Connector是底层I/O操作，不依赖Reactor，专注读写；Component是响应式数据处理，依赖Reactor，处理数据流。

### Q: 如何在Component中使用Connector？
A: 使用Adapter层的`DefaultReaderToSourceAdapter`和`DefaultWriterToSinkAdapter`进行转换。

### Q: 是否必须同时实现Reader和Writer？
A: 不是，可以只实现其中一个，取决于Connector的功能。

### Q: 如何支持多种数据类型？
A: 使用泛型参数`<T>`，在具体实现时指定数据类型。

---

**文档版本**：1.0.0  
**最后更新**：2025-11-10
