# Pipeline Framework 快速开始

## 项目概览

Pipeline Framework 是一个基于 **Project Reactor** 的响应式流处理框架，提供完整的 ETL 数据处理能力。

### 核心特性

- ✅ **完全响应式**: 基于 Project Reactor，支持背压和非阻塞
- ✅ **插件化架构**: 可扩展的连接器和算子系统
- ✅ **状态管理**: 支持有状态算子和检查点
- ✅ **调度执行**: 灵活的任务调度和执行引擎
- ✅ **可观测性**: 完整的指标收集和监控

## 项目结构

```
pipeline-framework/
├── pipeline-api          # 核心API接口（33个接口）
├── pipeline-core         # 核心实现
├── pipeline-connectors   # 连接器实现
├── pipeline-operators    # 算子实现
├── pipeline-scheduler    # 任务调度器
├── pipeline-executor     # 任务执行器
├── pipeline-state        # 状态管理
├── pipeline-checkpoint   # 检查点管理
├── pipeline-metrics      # 指标收集
├── pipeline-web          # Web API
└── pipeline-starter      # Spring Boot启动器
```

## 技术栈

- **Java**: 17
- **Framework**: Spring Boot 3.2.0
- **Reactive**: Project Reactor 3.6.0
- **Database**: MySQL 8.0 + R2DBC
- **Message Queue**: Kafka
- **Cache**: Redis
- **Monitoring**: Micrometer + Prometheus + Grafana

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 2. 启动基础服务

```bash
cd /workspace/pipeline-framework
docker-compose up -d
```

这将启动：
- MySQL (端口 3306)
- Kafka (端口 9092)
- Redis (端口 6379)
- Prometheus (端口 9090)
- Grafana (端口 3000)

### 3. 构建项目

```bash
mvn clean install
```

### 4. 运行应用

```bash
mvn spring-boot:run -pl pipeline-starter
```

应用将在 http://localhost:8080 启动

## 核心概念

### 1. DataSource - 数据源

```java
// 创建数据源
DataSource<String> source = kafkaConnector
    .createSource(sourceConfig)
    .block();

// 读取数据流
Flux<String> dataStream = source.read();
```

### 2. Operator - 数据转换

```java
// 创建算子
Operator<String, Integer> mapOperator = operatorFactory
    .createOperator(OperatorType.MAP, config)
    .block();

// 应用转换
Flux<Integer> transformed = mapOperator.apply(dataStream);
```

### 3. DataSink - 数据输出

```java
// 创建输出
DataSink<Integer> sink = jdbcConnector
    .createSink(sinkConfig)
    .block();

// 写入数据
sink.write(transformed).block();
```

### 4. Pipeline - 完整流程

```java
// 构建Pipeline
Pipeline<String, Integer> pipeline = Pipeline.builder()
    .source(source)
    .addOperator(mapOperator)
    .addOperator(filterOperator)
    .sink(sink)
    .build();

// 执行Pipeline
pipeline.execute()
    .doOnSuccess(result -> log.info("Pipeline completed"))
    .doOnError(error -> log.error("Pipeline failed", error))
    .subscribe();
```

## 响应式编程示例

### 异步数据处理

```java
// 从Kafka读取，转换，写入MySQL
kafkaSource.read()
    .map(data -> transform(data))
    .filter(data -> validate(data))
    .buffer(100)  // 批量处理
    .flatMap(batch -> mysqlSink.writeBatch(Flux.fromIterable(batch), 100))
    .subscribe();
```

### 背压控制

```java
// 自动处理背压
source.read()
    .onBackpressureBuffer(1000)  // 缓冲区
    .transform(operator::apply)
    .as(sink::write)
    .subscribe();
```

### 错误处理

```java
source.read()
    .transform(operator::apply)
    .onErrorResume(error -> {
        log.error("Error occurred", error);
        return Flux.empty();  // 继续处理
    })
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
    .as(sink::write)
    .subscribe();
```

## API接口

### Source接口（3个）
- `DataSource<T>` - 数据源
- `SourceConfig` - 配置
- `SourceType` - 类型

### Operator接口（3个）
- `Operator<IN, OUT>` - 算子
- `OperatorConfig` - 配置
- `OperatorType` - 类型

### Sink接口（3个）
- `DataSink<T>` - 输出
- `SinkConfig` - 配置
- `SinkType` - 类型

### Job接口（5个）
- `Job` - 任务
- `JobConfig` - 配置
- `JobType` - 类型
- `JobStatus` - 状态
- `RestartStrategy` - 重启策略

### Scheduler接口（5个）
- `JobScheduler` - 调度器
- `ScheduleConfig` - 配置
- `ScheduleType` - 类型
- `ScheduleStatus` - 状态
- `ScheduleResult` - 结果

### Executor接口（4个）
- `JobExecutor` - 执行器
- `JobResult` - 结果
- `ExecutionStatus` - 状态
- `ExecutionMetrics` - 指标

## 配置说明

### 开发环境配置 (application-dev.yml)

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://localhost:3306/pipeline_framework
    username: root
    password: root123456
  
  flyway:
    enabled: true
    url: jdbc:mysql://localhost:3306/pipeline_framework
```

### 生产环境配置 (application-prod.yml)

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## 监控和指标

### Actuator端点

- `/actuator/health` - 健康检查
- `/actuator/metrics` - 指标
- `/actuator/prometheus` - Prometheus格式指标

### Grafana Dashboard

访问 http://localhost:3000 查看可视化监控

默认账号:
- Username: admin
- Password: admin

## 数据库Migration

项目使用 Flyway 进行数据库版本管理：

```
pipeline-starter/src/main/resources/db/migration/
├── V1__Create_job_tables.sql
├── V2__Create_graph_tables.sql
├── V3__Create_connector_tables.sql
├── V4__Create_checkpoint_tables.sql
├── V5__Create_metrics_tables.sql
├── V6__Create_config_alert_tables.sql
├── V7__Insert_initial_data.sql
└── V8__Create_views.sql
```

应用启动时自动执行迁移。

## 开发指南

### 1. 创建自定义Connector

```java
@Component
public class CustomConnector implements Connector {
    @Override
    public String getType() {
        return "custom";
    }
    
    @Override
    public <T> Mono<DataSource<T>> createSource(SourceConfig config) {
        return Mono.fromSupplier(() -> new CustomSource<>(config));
    }
    
    @Override
    public <T> Mono<DataSink<T>> createSink(SinkConfig config) {
        return Mono.fromSupplier(() -> new CustomSink<>(config));
    }
}
```

### 2. 创建自定义Operator

```java
@Component
public class CustomOperator<IN, OUT> implements Operator<IN, OUT> {
    @Override
    public Flux<OUT> apply(Flux<IN> input) {
        return input
            .map(this::transform)
            .filter(this::validate);
    }
    
    private OUT transform(IN data) {
        // 转换逻辑
    }
}
```

### 3. 使用Builder模式

```java
Job job = Job.builder()
    .jobId("job-001")
    .jobName("ETL Job")
    .type(JobType.STREAMING)
    .streamGraph(graph)
    .config(config)
    .build();
```

## 常见问题

### Q: 如何处理大数据量？

A: 使用批处理和背压控制：

```java
source.read()
    .buffer(1000)  // 每1000条批处理
    .onBackpressureBuffer(10000)  // 缓冲区大小
    .flatMap(batch -> sink.writeBatch(Flux.fromIterable(batch), 1000))
    .subscribe();
```

### Q: 如何实现有状态处理？

A: 使用StateManager：

```java
stateManager.createState("counter", 0L)
    .flatMap(state -> 
        dataStream.flatMap(data -> 
            state.get()
                .flatMap(count -> state.update(count + 1))
                .thenReturn(data)
        )
    )
    .subscribe();
```

### Q: 如何配置检查点？

A: 在JobConfig中配置：

```java
JobConfig config = JobConfig.builder()
    .checkpointEnabled(true)
    .checkpointInterval(Duration.ofMinutes(1))
    .build();
```

## 性能优化建议

1. **使用适当的并行度**
   ```java
   .parallel(Runtime.getRuntime().availableProcessors())
   ```

2. **批量处理**
   ```java
   .buffer(1000)
   ```

3. **使用合适的Scheduler**
   ```java
   .subscribeOn(Schedulers.boundedElastic())
   ```

4. **避免阻塞操作**
   ```java
   // ❌ 错误
   .map(data -> blockingCall())
   
   // ✅ 正确
   .flatMap(data -> Mono.fromCallable(() -> blockingCall())
       .subscribeOn(Schedulers.boundedElastic()))
   ```

## 测试

### 单元测试

```bash
mvn test
```

### 集成测试

```bash
mvn verify
```

## 文档

- [包结构重构总结](./PACKAGE_REFACTORING_SUMMARY.md)
- [项目结构说明](./PROJECT_STRUCTURE.md)
- [构建和运行指南](./BUILD_AND_RUN.md)
- [贡献指南](./CONTRIBUTING.md)

## License

Apache License 2.0

## 联系方式

- Issues: [GitHub Issues](https://github.com/yourorg/pipeline-framework/issues)
- Documentation: [Wiki](https://github.com/yourorg/pipeline-framework/wiki)
