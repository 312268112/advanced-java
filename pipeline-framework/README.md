# Pipeline Framework

基于 Spring Boot 和 Project Reactor 的响应式 ETL 数据处理框架。

---

## 🎯 核心特性

- ✅ **插件化 Connector 机制** - Connector 不依赖 Reactor，可独立开发和测试
- ✅ **强类型泛型约束** - 多层次泛型参数，提供类型安全保障
- ✅ **丰富的设计模式** - 工厂、适配器、模板方法、策略、建造者等模式应用
- ✅ **灵活的架构分层** - Connector 层、Adapter 层、Component 层清晰分离
- ✅ **响应式数据流** - 基于 Project Reactor，支持背压、异步、非阻塞
- ✅ **多种任务类型** - STREAMING（流式）、BATCH（批处理）、SQL_BATCH（SQL 批处理）

---

## 🏗️ 核心架构

### 分层设计

```
┌─────────────────────────────────────────────────────────┐
│                   Application Layer                      │
│              (Job Definition & Execution)                │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────┐
│                  Component Layer                         │
│         (DataSource, Operator, DataSink)                 │
│              [依赖 Reactor]                              │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────┐
│                   Adapter Layer                          │
│    (Reader→Source, Writer→Sink 适配)                     │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────┐
│                  Connector Layer                         │
│     (ConnectorReader, ConnectorWriter)                   │
│              [不依赖 Reactor]                            │
└──────────────────────┬──────────────────────────────────┘
                       │
                External Systems
          (JDBC, Kafka, Redis, File...)
```

### 关键设计模式

| 模式 | 应用场景 | 类/接口 |
|------|---------|---------|
| 🏭 工厂模式 | Connector 创建 | `ConnectorFactory`, `ConnectorFactoryRegistry` |
| 🔌 适配器模式 | Connector → Component | `DefaultReaderToSourceAdapter`, `DefaultWriterToSinkAdapter` |
| 📋 模板方法模式 | 通用流程骨架 | `AbstractJdbcConnector`, `AbstractConnectorAdapter` |
| 🎯 策略模式 | 可替换的算法 | `ConnectorType` 枚举 + 多种 Connector 实现 |
| 🔧 建造者模式 | 复杂对象构建 | `ConnectorMetadata.Builder`, `ComponentMetadata.Builder` |
| 📝 注册表模式 | 动态注册 | `ConnectorFactoryRegistry` |

---

## 📦 模块结构

```
pipeline-framework/
├── pipeline-api/              # 核心接口定义
│   ├── connector/            # Connector 接口
│   │   ├── adapter/         # 适配器接口
│   │   └── factory/         # 工厂接口
│   ├── component/            # Component 基础接口
│   ├── source/               # DataSource 接口
│   ├── sink/                 # DataSink 接口
│   └── operator/             # Operator 接口
│
├── pipeline-core/             # 核心实现
│   ├── connector/            # Adapter 实现
│   ├── builder/              # Pipeline 构建器
│   └── runtime/              # 运行时
│
├── pipeline-connectors/       # Connector 实现
│   ├── jdbc/                 # JDBC Connector
│   ├── kafka/                # Kafka Connector
│   └── console/              # Console Connector
│
├── pipeline-operators/        # Operator 实现
├── pipeline-executor/         # 执行器
├── pipeline-scheduler/        # 调度器
├── pipeline-state/            # 状态管理
├── pipeline-checkpoint/       # 检查点
├── pipeline-metrics/          # 监控指标
└── pipeline-starter/          # Spring Boot 启动器
```

---

## 🚀 快速开始

### 1. 创建 Connector（不依赖 Reactor）

```java
// 配置
JdbcConnectorConfig config = new JdbcConnectorConfig();
config.setUrl("jdbc:mysql://localhost:3306/test");
config.setUsername("root");
config.setPassword("password");
config.setQuerySql("SELECT * FROM users");

// 创建 Reader
JdbcConnectorReader reader = new JdbcConnectorReader(config);
reader.open();

// 读取数据
List<Map<String, Object>> batch = reader.readBatch(1000);
System.out.println("Read: " + batch.size() + " records");

reader.close();
```

### 2. 使用工厂模式

```java
// 注册工厂
ConnectorFactoryRegistry registry = ConnectorFactoryRegistry.getInstance();
registry.register(ConnectorType.JDBC, new JdbcConnectorFactory());

// 创建 Connector
ConnectorReader<Map<String, Object>, JdbcConnectorConfig> reader = 
    registry.createReader(ConnectorType.JDBC, config);
```

### 3. 转换为 Component（集成 Reactor）

```java
// 创建适配器
DefaultReaderToSourceAdapter<Map<String, Object>, JdbcConnectorConfig> adapter =
    new DefaultReaderToSourceAdapter<>(reader, 1000);

// 获取 DataSource
DataSource<Map<String, Object>> source = adapter.adapt(reader);

// 使用响应式流
Flux<Map<String, Object>> stream = source.read();
stream.subscribe(data -> System.out.println(data));
```

### 4. 完整 ETL 流程

```java
// 源和目标
ConnectorReader reader = registry.createReader(ConnectorType.JDBC, sourceConfig);
ConnectorWriter writer = registry.createWriter(ConnectorType.JDBC, sinkConfig);

// 适配为 Component
DataSource source = new DefaultReaderToSourceAdapter(reader, 1000).adapt(reader);
DataSink sink = new DefaultWriterToSinkAdapter(writer, 1000).adapt(writer);

// 执行 ETL
source.read()
    .map(data -> {
        // 数据转换
        data.put("migrated_at", System.currentTimeMillis());
        return data;
    })
    .filter(data -> data.get("email") != null) // 过滤
    .transform(dataStream -> sink.write(dataStream))
    .block();
```

---

## 💡 核心接口

### Connector 层（不依赖 Reactor）

```java
// 顶层接口
public interface Connector<C extends ConnectorConfig> {
    String getName();
    ConnectorType getType();
    C getConfig();
    ConnectorMetadata getMetadata();
}

// Reader 接口
public interface ConnectorReader<T, C extends ConnectorConfig> extends Connector<C> {
    void open() throws Exception;
    List<T> readBatch(int batchSize) throws Exception;
    boolean hasNext();
    void close() throws Exception;
    
    // 可选能力
    Object getCheckpoint();
    void seekToCheckpoint(Object checkpoint) throws Exception;
    boolean supportsCheckpoint();
    double getProgress();
    long getReadCount();
}

// Writer 接口
public interface ConnectorWriter<T, C extends ConnectorConfig> extends Connector<C> {
    void open() throws Exception;
    void write(T record) throws Exception;
    void writeBatch(List<T> records) throws Exception;
    void flush() throws Exception;
    void close() throws Exception;
    
    // 事务能力
    boolean supportsTransaction();
    void beginTransaction() throws Exception;
    void commit() throws Exception;
    void rollback() throws Exception;
}
```

### Component 层（依赖 Reactor）

```java
// Component 基础接口
public interface Component<C> {
    String getName();
    ComponentType getComponentType();
    C getConfig();
}

// DataSource 接口
public interface DataSource<OUT> extends Component<SourceConfig>, LifecycleAware {
    Flux<OUT> read();
    SourceType getType();
}

// DataSink 接口
public interface DataSink<IN> extends Component<SinkConfig>, LifecycleAware {
    Mono<Void> write(Flux<IN> data);
    Mono<Void> flush();
}

// Operator 接口
public interface Operator<IN, OUT> extends StreamingComponent<IN, OUT, OperatorConfig> {
    Flux<OUT> apply(Flux<IN> input);
    OperatorType getType();
}
```

---

## 📚 开发指南

### 创建自定义 Connector

1. **定义配置类**

```java
public class MyConnectorConfig extends ConnectorConfig {
    private String endpoint;
    
    @Override
    public void validate() {
        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint required");
        }
    }
}
```

2. **实现 Reader**

```java
public class MyConnectorReader extends AbstractMyConnector<MyDataType>
    implements ReadableConnector<MyDataType, MyConnectorConfig> {
    
    @Override
    protected void doOpen() throws Exception {
        // 初始化连接
    }
    
    @Override
    public List<MyDataType> readBatch(int batchSize) throws Exception {
        // 读取数据
    }
    
    @Override
    protected void doClose() throws Exception {
        // 清理资源
    }
}
```

3. **实现工厂**

```java
public class MyConnectorFactory 
    implements ConnectorFactory<MyDataType, MyConnectorConfig> {
    
    @Override
    public ConnectorReader<MyDataType, MyConnectorConfig> createReader(
        MyConnectorConfig config) throws ConnectorException {
        return new MyConnectorReader(config);
    }
    
    @Override
    public ConnectorType getSupportedType() {
        return ConnectorType.CUSTOM;
    }
}
```

4. **注册使用**

```java
ConnectorFactoryRegistry.getInstance()
    .register(ConnectorType.CUSTOM, new MyConnectorFactory());
```

详细开发指南请参考：[CONNECTOR_DEVELOPMENT_GUIDE.md](CONNECTOR_DEVELOPMENT_GUIDE.md)

---

## 📖 文档

- [架构设计文档](ARCHITECTURE_DESIGN.md) - 详细的架构说明和设计模式应用
- [Connector 开发指南](CONNECTOR_DEVELOPMENT_GUIDE.md) - 如何开发自定义 Connector
- [项目结构说明](STRUCTURE.md) - 模块结构和目录说明
- [快速开始](QUICK_START.md) - 从零开始构建第一个 Pipeline

---

## 🎨 设计亮点

### 1. 职责分离

- **Connector 层**：专注 I/O 操作，不依赖 Reactor，易于测试
- **Adapter 层**：负责转换，将 Connector 适配为 Component
- **Component 层**：响应式数据处理，充分利用 Reactor 的能力

### 2. 泛型约束

```java
// 多层次泛型参数
Connector<C extends ConnectorConfig>
ConnectorReader<T, C extends ConnectorConfig>
StreamingComponent<IN, OUT, C>
ConnectorAdapter<CONN extends Connector<C>, COMP extends Component<?>, C extends ConnectorConfig>
```

### 3. 设计模式组合

- 工厂模式 + 注册表模式 = 动态扩展
- 适配器模式 + 模板方法模式 = 灵活转换
- 策略模式 + 泛型约束 = 类型安全

### 4. 易于扩展

- 新增 Connector：实现接口 + 注册工厂
- 新增 Operator：继承 StreamingComponent
- 新增 Job 类型：扩展 JobType 枚举

---

## 🔧 技术栈

- **Spring Boot 3.x** - 应用框架
- **Project Reactor** - 响应式编程
- **Java 17+** - 编程语言
- **Maven** - 构建工具
- **SLF4J + Logback** - 日志
- **JUnit 5** - 单元测试

---

## 📊 示例场景

### 场景 1：MySQL 到 MySQL 的数据迁移

```java
// 源数据库
JdbcConnectorConfig source = new JdbcConnectorConfig();
source.setUrl("jdbc:mysql://source:3306/db");
source.setQuerySql("SELECT * FROM users WHERE active = 1");

// 目标数据库
JdbcConnectorConfig sink = new JdbcConnectorConfig();
sink.setUrl("jdbc:mysql://target:3306/db");
sink.setTableName("users_backup");

// 执行迁移
registry.createReader(ConnectorType.JDBC, source)
    .adapt()
    .read()
    .transform(data -> transform(data))
    .writeTo(registry.createWriter(ConnectorType.JDBC, sink));
```

### 场景 2：实时日志处理

```java
// Kafka 读取日志
kafkaSource.read()
    .filter(log -> log.getLevel() == Level.ERROR)
    .map(log -> enrichLog(log))
    .writeTo(elasticsearchSink);
```

### 场景 3：批量数据聚合

```java
// 读取订单数据
jdbcSource.read()
    .buffer(Duration.ofSeconds(10))
    .map(orders -> aggregateOrders(orders))
    .writeTo(redisSink);
```

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

开发指南：
1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/my-feature`
3. 提交更改：`git commit -am 'Add my feature'`
4. 推送分支：`git push origin feature/my-feature`
5. 提交 Pull Request

---

## 📄 许可证

MIT License

---

## 👥 团队

Pipeline Framework Team

---

**版本**：1.0.0  
**最后更新**：2025-11-10

🚀 快速开始，立即体验强大的 ETL 框架！
