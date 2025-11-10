# Spring + Reactor 集成指南

## 📚 概述

本文档详细说明如何在 Pipeline Framework 中使用 Spring 和 Reactor，包括线程池配置、依赖注入和最佳实践。

## 🔧 Reactor 线程池配置

### 1. 配置文件（application.yml）

```yaml
reactor:
  scheduler:
    # IO 密集型操作线程池
    io:
      pool-size: 100
      queue-size: 1000
      thread-name-prefix: reactor-io-
    
    # CPU 密集型操作线程池
    compute:
      pool-size: 0  # 0 表示使用 CPU 核心数
      thread-name-prefix: reactor-compute-
    
    # 有界弹性线程池（阻塞操作）
    bounded-elastic:
      pool-size: 200
      queue-size: 10000
      ttl-seconds: 60
      thread-name-prefix: reactor-bounded-
    
    # Pipeline 执行专用线程池
    pipeline:
      pool-size: 50
      queue-size: 500
      thread-name-prefix: pipeline-exec-
```

### 2. Scheduler Bean 配置

```java
@Configuration
public class ReactorSchedulerConfig {
    
    @Bean(name = "ioScheduler", destroyMethod = "dispose")
    public Scheduler ioScheduler(ReactorSchedulerProperties properties) {
        ReactorSchedulerProperties.SchedulerConfig config = properties.getIo();
        
        return Schedulers.newBoundedElastic(
            config.getPoolSize(),
            config.getQueueSize(),
            config.getThreadNamePrefix(),
            60,
            true
        );
    }
    
    @Bean(name = "computeScheduler", destroyMethod = "dispose")
    public Scheduler computeScheduler(ReactorSchedulerProperties properties) {
        ReactorSchedulerProperties.SchedulerConfig config = properties.getCompute();
        
        int poolSize = config.getPoolSize();
        if (poolSize <= 0) {
            poolSize = Runtime.getRuntime().availableProcessors();
        }
        
        return Schedulers.newParallel(
            config.getThreadNamePrefix(),
            poolSize,
            true
        );
    }
    
    @Bean(name = "boundedElasticScheduler", destroyMethod = "dispose")
    public Scheduler boundedElasticScheduler(ReactorSchedulerProperties properties) {
        ReactorSchedulerProperties.BoundedElasticConfig config = properties.getBoundedElastic();
        
        return Schedulers.newBoundedElastic(
            config.getPoolSize(),
            config.getQueueSize(),
            config.getThreadNamePrefix(),
            config.getTtlSeconds(),
            true
        );
    }
    
    @Bean(name = "pipelineScheduler", destroyMethod = "dispose")
    public Scheduler pipelineScheduler(ReactorSchedulerProperties properties) {
        ReactorSchedulerProperties.SchedulerConfig config = properties.getPipeline();
        
        return Schedulers.newBoundedElastic(
            config.getPoolSize(),
            config.getQueueSize(),
            config.getThreadNamePrefix(),
            60,
            true
        );
    }
}
```

### 3. Scheduler 使用场景

#### IO Scheduler
**适用场景**：
- 数据库查询（SELECT 操作）
- HTTP/REST API 调用
- 消息队列操作（Kafka、RabbitMQ）
- 文件读写
- 网络 IO

**示例**：
```java
@Component
public class KafkaSourceCreator implements SourceCreator {
    
    private final Scheduler ioScheduler;

    public KafkaSourceCreator(@Qualifier("ioScheduler") Scheduler ioScheduler) {
        this.ioScheduler = ioScheduler;
    }

    @Override
    public Mono<DataSource<?>> create(SourceConfig config) {
        return Mono.fromCallable(() -> {
            // 创建 Kafka Source（可能涉及网络连接）
            return new KafkaSource<>(config);
        })
        .subscribeOn(ioScheduler);
    }
}
```

#### Compute Scheduler
**适用场景**：
- 数据转换
- 计算密集型任务
- 数据聚合
- 编解码

**示例**：
```java
@Component
public class MapOperatorCreator implements OperatorCreator {
    
    private final Scheduler computeScheduler;

    public MapOperatorCreator(@Qualifier("computeScheduler") Scheduler computeScheduler) {
        this.computeScheduler = computeScheduler;
    }

    @Override
    public Mono<Operator<?, ?>> create(OperatorConfig config) {
        return Mono.fromCallable(() -> {
            // 创建计算密集型 Operator
            return new MapOperator<>(config);
        })
        .subscribeOn(computeScheduler);
    }
}
```

#### Bounded Elastic Scheduler
**适用场景**：
- 阻塞 API 包装（如 JDBC）
- 同步第三方库调用
- 文件系统操作
- 不支持异步的遗留代码

**示例**：
```java
@Service
public class JobService {
    
    private final JobMapper jobMapper;
    private final Scheduler boundedElasticScheduler;

    public JobService(
            JobMapper jobMapper,
            @Qualifier("boundedElasticScheduler") Scheduler boundedElasticScheduler) {
        this.jobMapper = jobMapper;
        this.boundedElasticScheduler = boundedElasticScheduler;
    }

    public Mono<JobEntity> getByJobId(String jobId) {
        // 将 MyBatis 的阻塞调用包装为响应式
        return Mono.fromCallable(() -> jobMapper.selectByJobId(jobId))
            .subscribeOn(boundedElasticScheduler);
    }
}
```

#### Pipeline Scheduler
**适用场景**：
- Pipeline 主流程执行
- Graph 构建
- Job 调度
- 任务协调

**示例**：
```java
@Component
public class SpringGraphBasedPipelineBuilder {
    
    private final Scheduler pipelineScheduler;

    public SpringGraphBasedPipelineBuilder(
            @Qualifier("pipelineScheduler") Scheduler pipelineScheduler) {
        this.pipelineScheduler = pipelineScheduler;
    }

    public Mono<Pipeline<?, ?>> buildFromGraph(StreamGraph graph) {
        return Mono.defer(() -> {
            // 构建 Pipeline 逻辑
            return createPipeline(graph);
        })
        .subscribeOn(pipelineScheduler);
    }
}
```

---

## 🎯 Spring 依赖注入最佳实践

### 1. 构造函数注入（推荐）

```java
@Component
public class MyComponent {
    
    private final Scheduler ioScheduler;
    private final SpringSourceFactory sourceFactory;

    // 构造函数注入（Spring 推荐）
    public MyComponent(
            @Qualifier("ioScheduler") Scheduler ioScheduler,
            SpringSourceFactory sourceFactory) {
        this.ioScheduler = ioScheduler;
        this.sourceFactory = sourceFactory;
    }
}
```

**优势**：
- 不可变（final 字段）
- 易于测试（可以直接传入 mock 对象）
- 明确依赖关系

### 2. 使用 @Qualifier 区分同类型 Bean

```java
@Component
public class MyService {
    
    private final Scheduler ioScheduler;
    private final Scheduler computeScheduler;

    public MyService(
            @Qualifier("ioScheduler") Scheduler ioScheduler,
            @Qualifier("computeScheduler") Scheduler computeScheduler) {
        this.ioScheduler = ioScheduler;
        this.computeScheduler = computeScheduler;
    }
}
```

### 3. 使用 List 注入所有实现

```java
@Component
public class SpringOperatorFactory {
    
    private final Map<String, OperatorCreator> creatorMap;

    // Spring 会自动注入所有 OperatorCreator 实现
    public SpringOperatorFactory(List<OperatorCreator> creators) {
        this.creatorMap = new ConcurrentHashMap<>();
        for (OperatorCreator creator : creators) {
            creatorMap.put(creator.getType(), creator);
        }
    }
}
```

---

## 📖 完整示例

### 场景：创建一个新的 MySQL Source

#### 步骤 1：实现 DataSource

```java
public class MysqlSource implements DataSource<Map<String, Object>> {
    
    private final SourceConfig config;
    private final R2dbcEntityTemplate template;

    public MysqlSource(SourceConfig config, R2dbcEntityTemplate template) {
        this.config = config;
        this.template = template;
    }

    @Override
    public Flux<Map<String, Object>> read() {
        String sql = config.getProperty("sql");
        
        return template
            .getDatabaseClient()
            .sql(sql)
            .fetch()
            .all();
    }

    @Override
    public String getName() {
        return config.getProperty("name", "mysql-source");
    }

    @Override
    public SourceType getType() {
        return SourceType.MYSQL;
    }
}
```

#### 步骤 2：创建 Creator（添加 @Component）

```java
@Component
public class MysqlSourceCreator implements SourceCreator {
    
    private final Scheduler ioScheduler;
    private final R2dbcEntityTemplate template;

    public MysqlSourceCreator(
            @Qualifier("ioScheduler") Scheduler ioScheduler,
            R2dbcEntityTemplate template) {
        this.ioScheduler = ioScheduler;
        this.template = template;
    }

    @Override
    public Mono<DataSource<?>> create(SourceConfig config) {
        return Mono.fromCallable(() -> new MysqlSource(config, template))
            .subscribeOn(ioScheduler);
    }

    @Override
    public String getType() {
        return "mysql";
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
```

#### 步骤 3：使用

```java
@Service
public class PipelineService {
    
    private final SpringSourceFactory sourceFactory;

    public PipelineService(SpringSourceFactory sourceFactory) {
        this.sourceFactory = sourceFactory;
    }

    public Mono<DataSource<?>> createMysqlSource() {
        SourceConfig config = new SimpleSourceConfig(Map.of(
            "type", "mysql",
            "sql", "SELECT * FROM users"
        ));
        
        // 自动使用 MysqlSourceCreator
        return sourceFactory.createSource(config);
    }
}
```

---

## ⚡ 性能优化建议

### 1. 合理设置线程池大小

**IO 密集型**：
```yaml
reactor:
  scheduler:
    io:
      pool-size: 100  # 可以较大，因为线程大部分时间在等待 IO
```

**CPU 密集型**：
```yaml
reactor:
  scheduler:
    compute:
      pool-size: 0  # 使用 CPU 核心数，避免过度上下文切换
```

### 2. 避免在 Compute Scheduler 上执行阻塞操作

**❌ 错误示例**：
```java
return Mono.fromCallable(() -> {
    Thread.sleep(1000);  // 阻塞！
    return result;
})
.subscribeOn(computeScheduler);  // 不应该在 compute 上执行阻塞操作
```

**✅ 正确示例**：
```java
return Mono.fromCallable(() -> {
    Thread.sleep(1000);  // 阻塞操作
    return result;
})
.subscribeOn(boundedElasticScheduler);  // 使用 bounded-elastic
```

### 3. 使用 subscribeOn vs publishOn

**subscribeOn**：决定订阅（开始执行）时使用的线程
```java
Mono.fromCallable(() -> blockingCall())
    .subscribeOn(boundedElasticScheduler)  // 在这个线程池执行
```

**publishOn**：切换后续操作的线程
```java
Flux.range(1, 10)
    .map(i -> i * 2)
    .publishOn(computeScheduler)  // 后续操作在这个线程池执行
    .map(i -> i + 1)
```

### 4. 监控线程池

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

查看指标：
- `reactor.scheduler.threads.active`
- `reactor.scheduler.threads.max`
- `reactor.scheduler.tasks.pending`

---

## 🔍 调试技巧

### 1. 打印当前线程

```java
Mono.fromCallable(() -> {
    System.out.println("Executing on: " + Thread.currentThread().getName());
    return doWork();
})
.subscribeOn(ioScheduler);
```

### 2. 使用 Hooks 全局监控

```java
@Configuration
public class ReactorDebugConfig {
    
    @PostConstruct
    public void init() {
        // 开发环境启用调试
        Hooks.onOperatorDebug();
    }
}
```

### 3. 日志配置

```yaml
logging:
  level:
    reactor.core: DEBUG
    reactor.netty: DEBUG
```

---

## 📝 总结

### Scheduler 选择矩阵

| 场景 | 推荐 Scheduler | 原因 |
|-----|--------------|-----|
| 数据库查询 | `ioScheduler` | IO 密集型 |
| HTTP 请求 | `ioScheduler` | IO 密集型 |
| 数据转换 | `computeScheduler` | CPU 密集型 |
| JDBC 调用 | `boundedElasticScheduler` | 阻塞操作 |
| Pipeline 执行 | `pipelineScheduler` | 任务协调 |

### Spring 注解使用

| 注解 | 用途 | 示例 |
|-----|-----|-----|
| `@Component` | 通用组件 | Creator 类 |
| `@Service` | 业务逻辑 | PipelineService |
| `@Configuration` | 配置类 | ReactorSchedulerConfig |
| `@Bean` | Bean 定义 | Scheduler Bean |
| `@Qualifier` | 区分同类型 Bean | 多个 Scheduler |
| `@ConfigurationProperties` | 配置绑定 | ReactorSchedulerProperties |

### 核心原则

1. **正确的线程池，正确的任务**
2. **构造函数注入优于字段注入**
3. **使用 @Qualifier 明确指定 Bean**
4. **监控线程池使用情况**
5. **开发环境开启调试模式**
