# Pipeline Framework

基于Spring Boot和Project Reactor的响应式ETL数据处理框架。

## 核心特性

- ✅ **插件化Connector** - 独立SDK，不依赖Reactor
- ✅ **能力组合** - 通过接口组合实现灵活的Connector
- ✅ **响应式流** - 基于Reactor的高性能数据处理
- ✅ **简单易用** - Connector开发者无需了解Reactor
- ✅ **多种Job类型** - 支持流式、批处理、SQL批量任务

## 快速开始

### 1. 开发Connector

```java
public class MyReader implements Connector, Readable<Data>, Lifecycle {
    
    @Override
    public void open() throws Exception {
        // 打开连接
    }
    
    @Override
    public List<Data> read(int batchSize) throws Exception {
        // 批量读取数据
        List<Data> batch = new ArrayList<>();
        // ... 读取逻辑
        return batch;
    }
    
    @Override
    public boolean hasMore() {
        return true;
    }
    
    @Override
    public void close() throws Exception {
        // 关闭连接
    }
    
    @Override
    public String name() {
        return "my-reader";
    }
}
```

### 2. 使用Connector

```java
// 创建Connector
JdbcReader reader = new JdbcReader(dataSource, 
    "SELECT * FROM orders WHERE date > ?", 
    List.of(startDate), 
    1000);

// 框架转换为Source
ConnectorSource<Map<String, Object>> source = 
    new ConnectorSource<>(reader, 1000, config);

// 获取响应式流
Flux<Map<String, Object>> stream = source.getDataStream();

// 处理数据
stream.map(this::transform)
      .subscribe();
```

## 项目结构

```
pipeline-framework/
├── pipeline-connector-sdk/    # Connector SDK（不依赖Reactor）
├── pipeline-core/             # 框架核心（Reactor转换）
├── pipeline-connectors/       # 内置Connector实现
├── pipeline-api/              # 核心API定义
├── pipeline-operators/        # 数据处理算子
├── pipeline-scheduler/        # 任务调度
├── pipeline-executor/         # 任务执行
├── pipeline-state/            # 状态管理
├── pipeline-checkpoint/       # 检查点容错
├── pipeline-metrics/          # 监控指标
├── pipeline-web/              # Web API
└── pipeline-starter/          # Spring Boot启动
```

## Job类型

```java
STREAMING    // 流式任务（持续运行）- Kafka消费等
BATCH        // 批处理任务（一次性）- 文件导入等
SQL_BATCH    // SQL批量任务（多表整合）- 复杂查询聚合
```

## Connector能力接口

```java
Connector    // 标记接口
├── Readable     // 数据读取能力
├── Writable     // 数据写入能力
├── Seekable     // 断点续传能力（可选）
└── Lifecycle    // 生命周期管理
```

## 技术栈

- Java 17
- Spring Boot 3.2.0
- Project Reactor 3.6.0
- MySQL 8.0
- Kafka（可选）
- Redis（可选）

## 文档

- [Connector SDK 开发指南](CONNECTOR_SDK_GUIDE.md)
- [架构说明](ARCHITECTURE.md)
- [重构完成总结](REFACTORING_COMPLETE.md)

## 示例：JDBC Connector

查看 `pipeline-connectors/sql/` 目录：
- `JdbcReader.java` - JDBC数据读取
- `JdbcWriter.java` - JDBC数据写入

## 启动应用

```bash
# 编译项目
mvn clean install

# 启动应用
cd pipeline-starter
mvn spring-boot:run
```

## 核心设计理念

**让专注开发connector的人不关注是否使用reactor，只关注connector本身的能力。**

Connector开发者：
- ✅ 只实现简单的读写接口
- ✅ 不需要学习Reactor
- ✅ 专注业务逻辑

框架使用者：
- ✅ 自动获得响应式流
- ✅ 高性能处理
- ✅ 背压管理

---

**简单、专注、高效** 🚀
