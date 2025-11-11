# Pipeline Framework 项目结构

## 模块说明

### pipeline-api
核心API定义模块，包含所有接口定义。

```
pipeline-api/src/main/java/com/pipeline/framework/api/
├── connector/              # Connector接口
│   ├── ConnectorReader     # 数据读取器接口
│   └── ConnectorWriter     # 数据写入器接口
├── source/                 # Source接口
├── sink/                   # Sink接口
├── operator/               # Operator接口
├── job/                    # Job接口
├── graph/                  # StreamGraph接口
├── executor/               # Executor接口
└── scheduler/              # Scheduler接口
```

### pipeline-core
框架核心实现模块。

```
pipeline-core/src/main/java/com/pipeline/framework/core/
├── connector/              # Connector适配器
│   ├── ReaderSourceAdapter # Reader → Flux适配
│   └── WriterSinkAdapter   # Writer → Mono适配
├── builder/                # Pipeline构建器
├── factory/                # 组件工厂
├── graph/                  # Graph执行器
├── pipeline/               # Pipeline实现
├── runtime/                # 运行时
├── scheduler/              # 调度器配置
└── service/                # 服务层
```

### pipeline-connectors
Connector实现模块。

```
pipeline-connectors/src/main/java/com/pipeline/framework/connectors/
├── jdbc/                   # JDBC Connector
│   ├── JdbcConnectorReader
│   └── JdbcConnectorWriter
├── kafka/                  # Kafka Connector
├── console/                # Console Connector
└── ...                     # 其他Connector
```

### 其他模块
- **pipeline-operators**: 数据处理算子实现
- **pipeline-scheduler**: 任务调度实现
- **pipeline-executor**: 任务执行器实现
- **pipeline-state**: 状态管理
- **pipeline-checkpoint**: 检查点容错
- **pipeline-metrics**: 监控指标
- **pipeline-web**: Web API
- **pipeline-starter**: Spring Boot启动模块

## Connector开发

### 1. 实现ConnectorReader

```java
package com.pipeline.framework.connectors.custom;

import com.pipeline.framework.api.connector.ConnectorReader;
import java.util.List;

public class MyReader implements ConnectorReader<YourDataType> {
    
    @Override
    public void open() throws Exception {
        // 初始化，打开连接
    }
    
    @Override
    public List<YourDataType> readBatch(int batchSize) throws Exception {
        // 批量读取数据
        List<YourDataType> batch = new ArrayList<>();
        // ... 读取逻辑
        return batch;
    }
    
    @Override
    public boolean hasNext() {
        // 是否还有数据
        return true;
    }
    
    @Override
    public void close() throws Exception {
        // 清理资源，关闭连接
    }
}
```

### 2. 实现ConnectorWriter

```java
package com.pipeline.framework.connectors.custom;

import com.pipeline.framework.api.connector.ConnectorWriter;
import java.util.List;

public class MyWriter implements ConnectorWriter<YourDataType> {
    
    @Override
    public void open() throws Exception {
        // 初始化，打开连接
    }
    
    @Override
    public void write(YourDataType record) throws Exception {
        // 单条写入
    }
    
    @Override
    public void writeBatch(List<YourDataType> records) throws Exception {
        // 批量写入
    }
    
    @Override
    public void flush() throws Exception {
        // 刷新缓冲
    }
    
    @Override
    public void close() throws Exception {
        // 清理资源，关闭连接
    }
}
```

### 3. 在框架中使用

```java
// 创建Reader
MyReader reader = new MyReader();

// 使用适配器转换为Source
ReaderSourceAdapter<YourDataType> source = 
    new ReaderSourceAdapter<>(reader, 1000, config);

// 获取响应式流
Flux<YourDataType> stream = source.getDataStream();
```

## 依赖关系

```
pipeline-starter
    ├── pipeline-web
    ├── pipeline-executor
    ├── pipeline-scheduler
    └── pipeline-core
        ├── pipeline-api
        ├── pipeline-connectors
        │   └── pipeline-api
        ├── pipeline-operators
        │   └── pipeline-api
        ├── pipeline-state
        │   └── pipeline-api
        └── pipeline-checkpoint
            └── pipeline-api
```

## 编译和运行

```bash
# 编译整个项目
mvn clean install

# 只编译某个模块
cd pipeline-connectors
mvn clean install

# 运行应用
cd pipeline-starter
mvn spring-boot:run
```

## 添加新的Connector

1. 在 `pipeline-connectors` 模块创建新包
2. 实现 `ConnectorReader` 和/或 `ConnectorWriter`
3. 添加必要的依赖到 `pipeline-connectors/pom.xml`
4. 使用 `ReaderSourceAdapter` 或 `WriterSinkAdapter` 进行集成

## 注意事项

- Connector接口位于 `pipeline-api` 模块，不依赖Reactor
- 适配器位于 `pipeline-core` 模块，负责转换为响应式流
- Connector实现位于 `pipeline-connectors` 模块
- 外部依赖（如JDBC驱动）标记为 `optional`，按需引入

---

**简洁、清晰、易用** 🚀
