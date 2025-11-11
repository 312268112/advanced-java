# Pipeline Framework

基于Spring Boot和Project Reactor的响应式ETL数据处理框架。

## 核心特性

- ✅ **简单的Connector接口** - 不依赖Reactor，只需实现简单的读写方法
- ✅ **增强的能力** - 支持断点续传、事务、进度追踪
- ✅ **响应式流** - 框架自动将Connector转换为Reactor流
- ✅ **批量优化** - 批量读写提升性能
- ✅ **多种Job类型** - 支持流式、批处理、SQL批量任务

## 项目结构

```
pipeline-framework/
├── pipeline-api/              # 核心API定义
│   └── connector/             # Connector接口
│       ├── ConnectorReader    # 读取器接口
│       └── ConnectorWriter    # 写入器接口
├── pipeline-core/             # 框架核心
│   └── connector/             # Reactor适配器
│       ├── ReaderSourceAdapter
│       └── WriterSinkAdapter
├── pipeline-connectors/       # Connector实现
│   └── jdbc/                  # JDBC实现
│       ├── JdbcConnectorReader
│       └── JdbcConnectorWriter
└── ...
```

## 快速开始

### 1. 实现Reader

```java
public class MyReader implements ConnectorReader<Data> {
    
    @Override
    public void open() throws Exception {
        // 打开连接
    }
    
    @Override
    public List<Data> readBatch(int batchSize) throws Exception {
        // 批量读取
        List<Data> batch = new ArrayList<>();
        // ... 读取逻辑
        return batch;
    }
    
    @Override
    public boolean hasNext() {
        return true;
    }
    
    @Override
    public void close() throws Exception {
        // 关闭连接
    }
    
    // 可选：支持断点续传
    @Override
    public boolean supportsCheckpoint() {
        return true;
    }
    
    @Override
    public Object getCheckpoint() {
        return currentOffset;
    }
}
```

### 2. 实现Writer

```java
public class MyWriter implements ConnectorWriter<Data> {
    
    @Override
    public void open() throws Exception {
        // 打开连接
    }
    
    @Override
    public void writeBatch(List<Data> records) throws Exception {
        // 批量写入
    }
    
    @Override
    public void flush() throws Exception {
        // 刷新缓冲
    }
    
    @Override
    public void close() throws Exception {
        // 关闭连接
    }
    
    // 可选：支持事务
    @Override
    public boolean supportsTransaction() {
        return true;
    }
    
    @Override
    public void commit() throws Exception {
        // 提交事务
    }
}
```

### 3. 使用Connector

```java
// 创建Reader
JdbcConnectorReader reader = new JdbcConnectorReader(
    dataSource, 
    "SELECT * FROM orders WHERE date > ?",
    List.of(startDate),
    1000
);

// 框架转换为Source
ReaderSourceAdapter<Map<String,Object>> source = 
    new ReaderSourceAdapter<>(reader, 1000, config);

// 获取响应式流
Flux<Map<String,Object>> stream = source.getDataStream();
```

## Connector能力

### ConnectorReader

- ✅ 批量读取数据
- ✅ 检查是否还有数据
- ✅ 支持断点续传（可选）
- ✅ 获取读取进度
- ✅ 统计已读记录数

### ConnectorWriter

- ✅ 单条/批量写入
- ✅ 刷新缓冲区
- ✅ 支持事务（可选）
- ✅ 检查点保存/恢复
- ✅ 统计已写记录数

## Job类型

```java
STREAMING    // 流式任务（持续运行）
BATCH        // 批处理任务（一次性）
SQL_BATCH    // SQL批量任务（多表整合）
```

## 示例：JDBC

参见 `pipeline-connectors/jdbc/` 目录：
- `JdbcConnectorReader.java` - JDBC读取器
- `JdbcConnectorWriter.java` - JDBC写入器

## 编译运行

```bash
# 编译
mvn clean install

# 启动
cd pipeline-starter
mvn spring-boot:run
```

---

**简洁、高效、易用** 🚀
