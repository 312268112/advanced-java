# Pipeline Framework 架构说明

## 核心设计理念

### Connector 插件化

Connector采用**插件化设计**，完全独立于框架核心，不依赖Reactor：

```
┌─────────────────────────────────────┐
│         Connector SDK               │  独立SDK，不依赖Reactor
│  ┌──────────┐ ┌────────┐ ┌────────┐│
│  │Readable  │ │Writable│ │Seekable││  能力接口
│  └──────────┘ └────────┘ └────────┘│
└─────────────────────────────────────┘
                 │
                 │ 实现接口
                 ▼
┌─────────────────────────────────────┐
│      Connector实现（插件）            │  开发者实现
│      例如：JdbcReader/Writer         │
└─────────────────────────────────────┘
                 │
                 │ 框架转换
                 ▼
┌─────────────────────────────────────┐
│      ConnectorSource/Sink           │  在需要时转换
│      (core模块)                      │
└─────────────────────────────────────┘
                 │
                 │ 生成Flux/Mono
                 ▼
┌─────────────────────────────────────┐
│      Pipeline Core                  │  响应式处理
│      (Reactor Stream)               │
└─────────────────────────────────────┘
```

## 模块职责

### pipeline-connector-sdk
**职责**：提供Connector开发接口（不依赖Reactor）

**核心接口**：
- `Connector` - 标记接口
- `Readable<T>` - 数据读取能力
- `Writable<T>` - 数据写入能力
- `Seekable` - 断点续传能力（可选）
- `Lifecycle` - 生命周期管理
- `Position` - 位置信息

### pipeline-core
**职责**：框架核心，负责响应式流处理

**关键类**：
- `ConnectorSource` - 将Connector转换为Source（Flux）
- `ConnectorSink` - 将Connector转换为Sink（Mono）

### pipeline-connectors
**职责**：内置Connector实现

**示例**：
- `JdbcReader` - JDBC数据读取
- `JdbcWriter` - JDBC数据写入

## Job类型

```java
public enum JobType {
    STREAMING,    // 流式任务（持续运行）
    BATCH,        // 批处理任务（一次性）
    SQL_BATCH     // SQL批量任务（多表整合）
}
```

## 开发流程

### 1. 开发Connector（插件开发者）

```java
public class MyConnector implements Connector, Readable<Data>, Lifecycle {
    // 只关注数据读写逻辑，不关注Reactor
    
    public List<Data> read(int batchSize) throws Exception {
        // 简单的批量读取
    }
}
```

### 2. 使用Connector（框架使用者）

```java
// 创建connector实例
JdbcReader reader = new JdbcReader(dataSource, sql);

// 框架自动转换为Source
ConnectorSource<Map<String, Object>> source = 
    new ConnectorSource<>(reader, 1000, config);

// 获取响应式流
Flux<Map<String, Object>> stream = source.getDataStream();
```

## 配置管理

配置直接放在各个模块中，不单独抽取autoconfigure模块：

```yaml
# application.yml
pipeline:
  framework:
    executor:
      core-pool-size: 10
      max-pool-size: 50
```

## 核心优势

1. **简单** - Connector开发者无需了解Reactor
2. **专注** - 只关注数据读写逻辑
3. **插件化** - 独立开发和发布
4. **高性能** - 批量处理优化
5. **灵活** - 能力接口可自由组合

---

**设计原则**：让专注开发connector的人不关注是否使用reactor，只关注connector本身的能力。
