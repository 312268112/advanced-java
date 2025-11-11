# Connector SDK 开发指南

## 简介

Pipeline Framework Connector SDK 提供简洁的接口来开发数据连接器，**完全不依赖Reactor**。

## 核心设计

### 能力接口

Connector通过实现不同的能力接口来组合功能：

```java
Connector          // 标记接口，所有connector都实现
├── Readable       // 数据读取能力
├── Writable       // 数据写入能力
├── Seekable       // 断点续传能力（可选）
└── Lifecycle      // 生命周期管理
```

## 快速开始

### 1. 实现读取Connector

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
        for (int i = 0; i < batchSize && hasData(); i++) {
            batch.add(readOne());
        }
        return batch;
    }
    
    @Override
    public boolean hasMore() {
        // 是否还有数据
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

### 2. 实现写入Connector

```java
public class MyWriter implements Connector, Writable<Data>, Lifecycle {
    
    @Override
    public void open() throws Exception {
        // 打开连接
    }
    
    @Override
    public void write(List<Data> records) throws Exception {
        // 批量写入
        for (Data record : records) {
            writeOne(record);
        }
    }
    
    @Override
    public void flush() throws Exception {
        // 刷新缓冲
    }
    
    @Override
    public void close() throws Exception {
        // 关闭连接
    }
    
    @Override
    public String name() {
        return "my-writer";
    }
}
```

### 3. 支持断点续传（可选）

```java
public class SeekableReader implements Connector, Readable<Data>, Seekable, Lifecycle {
    
    @Override
    public void seek(Position position) throws Exception {
        long offset = position.getLong("offset");
        // 定位到指定位置
    }
    
    @Override
    public Position currentPosition() {
        return Position.of("offset", currentOffset);
    }
    
    // ... 其他方法
}
```

## 框架集成

Connector在框架中自动转换为响应式流：

```java
// Connector实现（简单，不依赖Reactor）
JdbcReader reader = new JdbcReader(dataSource, sql);

// 框架转换为Source（在core中完成）
ConnectorSource<Map<String, Object>> source = 
    new ConnectorSource<>(reader, 1000, config);

// 自动获得Reactor流
Flux<Map<String, Object>> stream = source.getDataStream();
```

## 完整示例：JDBC Connector

参见：
- `JdbcReader.java`
- `JdbcWriter.java`

## 最佳实践

1. **批量处理** - 实现批量读写以提高性能
2. **资源管理** - 在close()中确保资源释放
3. **异常处理** - 抛出明确的异常信息
4. **日志记录** - 记录关键操作和进度

---

**简单、专注、高效** - 开发者只需关注连接器逻辑，框架处理响应式转换。
