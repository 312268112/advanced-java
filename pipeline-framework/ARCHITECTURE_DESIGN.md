# Pipeline Framework 架构设计文档

## 概述

Pipeline Framework 是一个基于响应式编程（Project Reactor）的ETL数据处理框架，支持流式处理、批处理和SQL批处理。

本次重构的核心目标：
1. **分离关注点**：Connector不依赖Reactor，Component依赖Reactor
2. **增强抽象**：多层次的接口继承和泛型约束
3. **应用设计模式**：工厂、适配器、模板方法、策略、建造者
4. **提升扩展性**：插件化的Connector注册机制

---

## 核心架构层次

```
┌─────────────────────────────────────────────────────────┐
│                     Application Layer                    │
│              (Job Definition & Execution)                │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────┐
│                   Component Layer                        │
│            (Reactor-based Data Processing)               │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐           │
│  │ DataSource│   │ Operator │   │ DataSink │           │
│  └──────────┘   └──────────┘   └──────────┘           │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────┐
│                   Adapter Layer                          │
│          (Connector → Component Adaptation)              │
│  ┌────────────────────────────────────────────┐         │
│  │  ReaderToSourceAdapter  WriterToSinkAdapter │         │
│  └────────────────────────────────────────────┘         │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────┐
│                  Connector Layer                         │
│            (Reactor-free I/O Operations)                 │
│  ┌──────────────────┐   ┌──────────────────┐           │
│  │ ConnectorReader  │   │ ConnectorWriter  │           │
│  └──────────────────┘   └──────────────────┘           │
└─────────────────────────────────────────────────────────┘
```

---

## 设计模式应用

### 1. 工厂模式 (Factory Pattern)

**目的**：统一创建Connector实例，解耦对象创建逻辑。

**实现**：
- `ConnectorFactory<T, C>`: 工厂接口，泛型参数T为数据类型，C为配置类型
- `ConnectorFactoryRegistry`: 工厂注册中心，单例模式
- `JdbcConnectorFactory`: JDBC连接器的具体工厂实现

**类图**：
```
┌─────────────────────────────┐
│   ConnectorFactory<T, C>    │
├─────────────────────────────┤
│ + createReader(C): Reader   │
│ + createWriter(C): Writer   │
│ + getSupportedType(): Type  │
└──────────────┬──────────────┘
               △
               │
┌──────────────┴──────────────┐
│   JdbcConnectorFactory      │
└─────────────────────────────┘
```

**使用示例**：
```java
// 注册工厂
ConnectorFactoryRegistry registry = ConnectorFactoryRegistry.getInstance();
registry.register(ConnectorType.JDBC, new JdbcConnectorFactory());

// 创建Reader
JdbcConnectorConfig config = new JdbcConnectorConfig();
config.setUrl("jdbc:mysql://localhost:3306/test");
config.setUsername("root");
config.setPassword("password");
config.setQuerySql("SELECT * FROM users");

ConnectorReader<Map<String, Object>, JdbcConnectorConfig> reader = 
    registry.createReader(ConnectorType.JDBC, config);
```

---

### 2. 适配器模式 (Adapter Pattern)

**目的**：将不依赖Reactor的Connector转换为依赖Reactor的Component。

**实现**：
- `ConnectorAdapter<CONN, COMP, C>`: 适配器接口
- `AbstractConnectorAdapter<CONN, COMP, C>`: 适配器抽象基类
- `DefaultReaderToSourceAdapter<T, C>`: Reader到Source的适配器
- `DefaultWriterToSinkAdapter<T, C>`: Writer到Sink的适配器

**类图**：
```
┌────────────────────────────────────┐
│  ConnectorAdapter<CONN, COMP, C>   │
├────────────────────────────────────┤
│ + adapt(CONN): COMP                │
│ + getConnector(): CONN             │
│ + supports(CONN): boolean          │
└────────────────┬───────────────────┘
                 △
                 │
┌────────────────┴───────────────────┐
│  AbstractConnectorAdapter          │
├────────────────────────────────────┤
│ # preAdapt(CONN): void             │
│ # doAdapt(CONN): COMP              │
│ # postAdapt(CONN, COMP): void      │
└────────────────┬───────────────────┘
                 △
        ┌────────┴────────┐
        │                 │
┌───────┴──────────┐  ┌──┴──────────────┐
│ ReaderToSource   │  │ WriterToSink    │
│ Adapter          │  │ Adapter         │
└──────────────────┘  └─────────────────┘
```

**使用示例**：
```java
// 创建Connector
JdbcConnectorReader reader = new JdbcConnectorReader(config);

// 使用适配器转换为DataSource
DefaultReaderToSourceAdapter<Map<String, Object>, JdbcConnectorConfig> adapter =
    new DefaultReaderToSourceAdapter<>(reader, 1000);

DataSource<Map<String, Object>> source = adapter.adapt(reader);

// 使用响应式流
Flux<Map<String, Object>> dataStream = source.read();
dataStream.subscribe(data -> System.out.println(data));
```

---

### 3. 模板方法模式 (Template Method Pattern)

**目的**：定义算法骨架，让子类实现具体步骤。

**实现**：
- `AbstractJdbcConnector<T>`: JDBC连接器的抽象基类
- `AbstractConnectorAdapter<CONN, COMP, C>`: 适配器的抽象基类

**模板方法流程**：

```java
// AbstractJdbcConnector 的 open() 方法
public void open() throws Exception {
    // 1. 加载驱动（公共步骤）
    loadDriver();
    
    // 2. 建立连接（公共步骤）
    establishConnection();
    
    // 3. 配置连接（公共步骤）
    configureConnection();
    
    // 4. 子类初始化（钩子方法）
    doOpen();
}
```

**子类实现**：
```java
public class JdbcConnectorReader extends AbstractJdbcConnector<Map<String, Object>> {
    
    @Override
    protected void doOpen() throws Exception {
        // 子类特定的初始化逻辑
        statement = connection.prepareStatement(config.getQuerySql());
        resultSet = statement.executeQuery();
    }
}
```

---

### 4. 策略模式 (Strategy Pattern)

**目的**：定义一系列算法，让它们可以相互替换。

**实现**：
- 不同类型的`Connector`作为不同的策略
- `ConnectorType`枚举定义策略类型
- `ConnectorFactoryRegistry`作为策略选择器

**使用示例**：
```java
// 策略1：JDBC Connector
ConnectorReader jdbcReader = registry.createReader(ConnectorType.JDBC, jdbcConfig);

// 策略2：Kafka Connector（未来扩展）
ConnectorReader kafkaReader = registry.createReader(ConnectorType.KAFKA, kafkaConfig);

// 策略3：File Connector（未来扩展）
ConnectorReader fileReader = registry.createReader(ConnectorType.FILE, fileConfig);
```

---

### 5. 建造者模式 (Builder Pattern)

**目的**：分步骤构建复杂对象。

**实现**：
- `ConnectorMetadata.Builder`: 构建Connector元数据
- `ComponentMetadata.Builder`: 构建Component元数据

**使用示例**：
```java
ConnectorMetadata metadata = ConnectorMetadata.builder()
    .name("my-jdbc-reader")
    .type(ConnectorType.JDBC)
    .version("1.0.0")
    .description("MySQL数据库读取器")
    .attribute("database", "test")
    .attribute("table", "users")
    .build();
```

---

## 核心接口设计

### 1. Connector层次

```java
// 顶层接口
public interface Connector<C extends ConnectorConfig> {
    String getName();
    ConnectorType getType();
    C getConfig();
    ConnectorMetadata getMetadata();
    boolean validate();
}

// Reader接口（增加泛型约束）
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

// Writer接口（增加泛型约束）
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
    
    // 检查点能力
    Object saveCheckpoint() throws Exception;
    void restoreCheckpoint(Object checkpoint) throws Exception;
    long getWriteCount();
}

// 可读连接器（增强接口）
public interface ReadableConnector<T, C extends ConnectorConfig> 
    extends ConnectorReader<T, C> {
    ConnectorReader<T, C> duplicate() throws ConnectorException;
    boolean supportsParallelRead();
}

// 可写连接器（增强接口）
public interface WritableConnector<T, C extends ConnectorConfig> 
    extends ConnectorWriter<T, C> {
    ConnectorWriter<T, C> duplicate() throws ConnectorException;
    boolean supportsParallelWrite();
    boolean supportsIdempotentWrite();
}
```

---

### 2. Component层次

```java
// 顶层接口
public interface Component<C> {
    String getName();
    ComponentType getComponentType();
    C getConfig();
    Mono<Boolean> healthCheck();
    ComponentMetadata getMetadata();
}

// 生命周期接口
public interface LifecycleAware {
    Mono<Void> start();
    Mono<Void> stop();
    boolean isRunning();
}

// 流式组件接口（三个泛型参数）
public interface StreamingComponent<IN, OUT, C> extends Component<C> {
    Flux<OUT> process(Flux<IN> input);
    Class<IN> getInputType();
    Class<OUT> getOutputType();
}

// DataSource接口
public interface DataSource<OUT> extends Component<SourceConfig>, LifecycleAware {
    Flux<OUT> read();
    SourceType getType();
    Class<OUT> getOutputType();
}

// DataSink接口
public interface DataSink<IN> extends Component<SinkConfig>, LifecycleAware {
    Mono<Void> write(Flux<IN> data);
    Mono<Void> writeBatch(Flux<IN> data, int batchSize);
    SinkType getType();
    Mono<Void> flush();
    Class<IN> getInputType();
}

// Operator接口
public interface Operator<IN, OUT> 
    extends StreamingComponent<IN, OUT, OperatorConfig> {
    Flux<OUT> apply(Flux<IN> input);
    OperatorType getType();
}
```

---

## 泛型约束体系

### 层次1：基础泛型
```java
// Connector层：<T数据类型, C配置类型>
Connector<C extends ConnectorConfig>
ConnectorReader<T, C extends ConnectorConfig>
ConnectorWriter<T, C extends ConnectorConfig>
```

### 层次2：组件泛型
```java
// Component层：<C配置类型>
Component<C>

// StreamingComponent层：<IN输入, OUT输出, C配置>
StreamingComponent<IN, OUT, C>
Operator<IN, OUT> extends StreamingComponent<IN, OUT, OperatorConfig>
```

### 层次3：适配器泛型
```java
// Adapter层：<CONN连接器, COMP组件, C配置>
ConnectorAdapter<CONN extends Connector<C>, COMP extends Component<?>, C extends ConnectorConfig>

// 具体适配器
ReaderToSourceAdapter<T, C extends ConnectorConfig>
    extends ConnectorAdapter<ConnectorReader<T, C>, DataSource<T>, C>

WriterToSinkAdapter<T, C extends ConnectorConfig>
    extends ConnectorAdapter<ConnectorWriter<T, C>, DataSink<T>, C>
```

---

## 使用场景示例

### 场景1：创建一个JDBC到MySQL的ETL任务

```java
// 1. 创建Reader配置
JdbcConnectorConfig sourceConfig = new JdbcConnectorConfig();
sourceConfig.setName("mysql-source");
sourceConfig.setUrl("jdbc:mysql://source:3306/db");
sourceConfig.setUsername("root");
sourceConfig.setPassword("password");
sourceConfig.setQuerySql("SELECT * FROM users WHERE active = 1");
sourceConfig.setBatchSize(1000);

// 2. 创建Writer配置
JdbcConnectorConfig sinkConfig = new JdbcConnectorConfig();
sinkConfig.setName("mysql-sink");
sinkConfig.setUrl("jdbc:mysql://target:3306/db");
sinkConfig.setUsername("root");
sinkConfig.setPassword("password");
sinkConfig.setTableName("users_backup");

// 3. 使用工厂创建Connector
ConnectorFactoryRegistry registry = ConnectorFactoryRegistry.getInstance();
registry.register(ConnectorType.JDBC, new JdbcConnectorFactory());

ConnectorReader<Map<String, Object>, JdbcConnectorConfig> reader = 
    registry.createReader(ConnectorType.JDBC, sourceConfig);

ConnectorWriter<Map<String, Object>, JdbcConnectorConfig> writer = 
    registry.createWriter(ConnectorType.JDBC, sinkConfig);

// 4. 使用适配器转换为Component
DefaultReaderToSourceAdapter<Map<String, Object>, JdbcConnectorConfig> sourceAdapter =
    new DefaultReaderToSourceAdapter<>(reader, 1000);
DataSource<Map<String, Object>> source = sourceAdapter.adapt(reader);

DefaultWriterToSinkAdapter<Map<String, Object>, JdbcConnectorConfig> sinkAdapter =
    new DefaultWriterToSinkAdapter<>(writer, 1000);
DataSink<Map<String, Object>> sink = sinkAdapter.adapt(writer);

// 5. 构建Pipeline执行
source.read()
    .map(data -> {
        // 数据转换逻辑
        data.put("migrated_at", System.currentTimeMillis());
        return data;
    })
    .transform(dataStream -> sink.write(dataStream))
    .subscribe();
```

### 场景2：扩展新的Connector类型

```java
// 1. 定义配置类
public class KafkaConnectorConfig extends ConnectorConfig {
    private String bootstrapServers;
    private String topic;
    // ... getters and setters
}

// 2. 实现Reader
public class KafkaConnectorReader 
    extends AbstractKafkaConnector<String>
    implements ReadableConnector<String, KafkaConnectorConfig> {
    
    @Override
    protected void doOpen() throws Exception {
        // Kafka consumer初始化
    }
    
    @Override
    public List<String> readBatch(int batchSize) throws Exception {
        // 读取消息
    }
    
    // ... 其他方法实现
}

// 3. 实现工厂
public class KafkaConnectorFactory 
    implements ConnectorFactory<String, KafkaConnectorConfig> {
    
    @Override
    public ConnectorReader<String, KafkaConnectorConfig> createReader(
        KafkaConnectorConfig config) throws ConnectorException {
        return new KafkaConnectorReader(config);
    }
    
    // ... 其他方法实现
}

// 4. 注册工厂
ConnectorFactoryRegistry.getInstance()
    .register(ConnectorType.KAFKA, new KafkaConnectorFactory());
```

---

## 总结

本架构通过以下设计原则实现了高度的灵活性和可扩展性：

1. **单一职责原则**：Connector专注I/O，Component专注数据处理
2. **开闭原则**：通过接口和抽象类，对扩展开放，对修改封闭
3. **里氏替换原则**：子类可以替换父类，不影响程序正确性
4. **接口隔离原则**：多个专用接口，而非单一大接口
5. **依赖倒置原则**：依赖抽象，而非具体实现

**关键优势**：
- ✅ 插件化的Connector注册机制
- ✅ 类型安全的泛型约束
- ✅ 职责清晰的分层架构
- ✅ 灵活的设计模式组合
- ✅ 易于测试和扩展

**设计模式应用总结**：
- 🏭 工厂模式：统一创建Connector
- 🔌 适配器模式：Connector → Component转换
- 📋 模板方法模式：定义算法骨架
- 🎯 策略模式：可替换的Connector实现
- 🔧 建造者模式：复杂对象构建
- 📝 注册表模式：动态注册Connector工厂
- 🔒 单例模式：ConnectorFactoryRegistry

---

**文档版本**：1.0.0  
**最后更新**：2025-11-10  
**作者**：Pipeline Framework Team
