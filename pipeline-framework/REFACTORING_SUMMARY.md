# Pipeline Framework 重构总结

## 🎉 重构完成

本次重构主要聚焦三个方面：
1. **使用设计模式替代 switch case**
2. **使用 Spring 注解管理所有组件**
3. **配置 Reactor 线程池**

---

## 📋 主要改动

### 1. 策略模式替代 Switch Case

#### ❌ 重构前

```java
public Operator createOperator(OperatorType type, OperatorConfig config) {
    switch (type) {
        case FILTER:
            return new FilterOperator(config);
        case MAP:
            return new MapOperator(config);
        case AGGREGATE:
            return new AggregateOperator(config);
        default:
            throw new IllegalArgumentException("Unsupported type: " + type);
    }
}
```

**问题**：
- 每增加一个类型都要修改这个方法
- 违反开闭原则
- 代码耦合度高

#### ✅ 重构后

```java
// 1. 定义策略接口
public interface OperatorCreator extends ComponentCreator<Operator<?, ?>, OperatorConfig> {
    Mono<Operator<?, ?>> create(OperatorConfig config);
    String getType();
}

// 2. 实现具体策略（每个类型一个 @Component 类）
@Component
public class FilterOperatorCreator implements OperatorCreator {
    @Override
    public Mono<Operator<?, ?>> create(OperatorConfig config) {
        return Mono.fromCallable(() -> new FilterOperator<>(config));
    }
    
    @Override
    public String getType() {
        return "filter";
    }
}

// 3. Spring 工厂自动注入所有策略
@Component
public class SpringOperatorFactory {
    private final Map<String, OperatorCreator> creatorMap;

    // Spring 自动注入所有 OperatorCreator 实现
    public SpringOperatorFactory(List<OperatorCreator> creators) {
        this.creatorMap = new ConcurrentHashMap<>();
        for (OperatorCreator creator : creators) {
            creatorMap.put(creator.getType(), creator);
        }
    }

    public Mono<Operator<?, ?>> createOperator(OperatorConfig config) {
        String type = config.getType().name().toLowerCase();
        OperatorCreator creator = creatorMap.get(type);
        return creator.create(config);  // 无需 switch！
    }
}
```

**优势**：
- ✅ 符合开闭原则：新增类型只需添加一个 `@Component` 类
- ✅ 低耦合：每个策略独立
- ✅ 易于测试：可以单独测试每个策略
- ✅ Spring 自动管理：无需手动注册

---

### 2. Spring 注解管理组件

#### 新增的 Spring 组件

| 组件类型 | 注解 | 示例 |
|---------|-----|------|
| Creator（策略） | `@Component` | `FilterOperatorCreator` |
| Factory（工厂） | `@Component` | `SpringSourceFactory` |
| Builder（构建器） | `@Component` | `SpringGraphBasedPipelineBuilder` |
| Service（服务） | `@Service` | `PipelineExecutionService` |
| Config（配置） | `@Configuration` | `ReactorSchedulerConfig` |
| Properties（属性） | `@ConfigurationProperties` | `ReactorSchedulerProperties` |

#### 依赖注入示例

```java
@Component
public class SpringGraphBasedPipelineBuilder {
    
    private final SpringSourceFactory sourceFactory;
    private final SpringSinkFactory sinkFactory;
    private final SpringOperatorFactory operatorFactory;
    private final Scheduler pipelineScheduler;

    // 构造函数注入所有依赖
    public SpringGraphBasedPipelineBuilder(
            SpringSourceFactory sourceFactory,
            SpringSinkFactory sinkFactory,
            SpringOperatorFactory operatorFactory,
            @Qualifier("pipelineScheduler") Scheduler pipelineScheduler) {
        this.sourceFactory = sourceFactory;
        this.sinkFactory = sinkFactory;
        this.operatorFactory = operatorFactory;
        this.pipelineScheduler = pipelineScheduler;
    }
}
```

---

### 3. Reactor 线程池配置

#### 配置文件（application.yml）

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
      pool-size: 0  # 0 = CPU 核心数
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

#### Scheduler Bean 定义

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
    
    // ... 其他 Scheduler Bean
}
```

#### 使用 Scheduler

```java
@Component
public class KafkaSourceCreator implements SourceCreator {
    
    private final Scheduler ioScheduler;

    public KafkaSourceCreator(@Qualifier("ioScheduler") Scheduler ioScheduler) {
        this.ioScheduler = ioScheduler;
    }

    @Override
    public Mono<DataSource<?>> create(SourceConfig config) {
        return Mono.fromCallable(() -> new KafkaSource<>(config))
            .subscribeOn(ioScheduler);  // 在 IO 线程池执行
    }
}
```

---

## 📊 架构对比

### 重构前

```
┌──────────────────────────────────┐
│    手动创建工厂和组件             │
│  - switch case 判断类型          │
│  - 硬编码组件创建逻辑             │
│  - 无线程池管理                  │
└──────────────────────────────────┘
```

### 重构后

```
┌──────────────────────────────────┐
│         Spring 容器               │
│  - 自动扫描 @Component           │
│  - 依赖注入                      │
│  - 生命周期管理                  │
└──────────────────────────────────┘
        ↓
┌──────────────────────────────────┐
│      策略模式 (Creator)           │
│  - FilterOperatorCreator         │
│  - MapOperatorCreator            │
│  - KafkaSourceCreator            │
│  - ConsoleSinkCreator            │
└──────────────────────────────────┘
        ↓
┌──────────────────────────────────┐
│      工厂模式 (Factory)           │
│  - SpringSourceFactory           │
│  - SpringSinkFactory             │
│  - SpringOperatorFactory         │
└──────────────────────────────────┘
        ↓
┌──────────────────────────────────┐
│      构建器 (Builder)             │
│  - SpringGraphBasedPipelineBuilder│
└──────────────────────────────────┘
        ↓
┌──────────────────────────────────┐
│      服务层 (Service)             │
│  - PipelineExecutionService      │
└──────────────────────────────────┘
```

---

## 📁 新增文件列表

### API 层（策略接口）
- `pipeline-api/src/main/java/com/pipeline/framework/api/strategy/ComponentCreator.java`
- `pipeline-api/src/main/java/com/pipeline/framework/api/strategy/SourceCreator.java`
- `pipeline-api/src/main/java/com/pipeline/framework/api/strategy/SinkCreator.java`
- `pipeline-api/src/main/java/com/pipeline/framework/api/strategy/OperatorCreator.java`

### Core 层（工厂、配置）
- `pipeline-core/src/main/java/com/pipeline/framework/core/factory/SpringSourceFactory.java`
- `pipeline-core/src/main/java/com/pipeline/framework/core/factory/SpringSinkFactory.java`
- `pipeline-core/src/main/java/com/pipeline/framework/core/factory/SpringOperatorFactory.java`
- `pipeline-core/src/main/java/com/pipeline/framework/core/builder/SpringGraphBasedPipelineBuilder.java`
- `pipeline-core/src/main/java/com/pipeline/framework/core/service/PipelineExecutionService.java`
- `pipeline-core/src/main/java/com/pipeline/framework/core/config/ReactorSchedulerConfig.java`
- `pipeline-core/src/main/java/com/pipeline/framework/core/config/ReactorSchedulerProperties.java`

### Connectors 层（具体策略实现）
- `pipeline-connectors/src/main/java/com/pipeline/framework/connectors/console/ConsoleSourceCreator.java`
- `pipeline-connectors/src/main/java/com/pipeline/framework/connectors/console/ConsoleSinkCreator.java`
- `pipeline-connectors/src/main/java/com/pipeline/framework/connectors/kafka/KafkaSourceCreator.java`

### Operators 层（具体策略实现）
- `pipeline-operators/src/main/java/com/pipeline/framework/operators/filter/FilterOperatorCreator.java`
- `pipeline-operators/src/main/java/com/pipeline/framework/operators/map/MapOperatorCreator.java`

### 文档
- `DESIGN_PATTERN_EXPLANATION.md` - 设计模式详解
- `SPRING_REACTOR_GUIDE.md` - Spring + Reactor 集成指南
- `REFACTORING_SUMMARY.md` - 重构总结（本文档）

---

## 🎯 如何添加新组件

### 示例：添加一个新的 AggregateOperator

#### 步骤 1：实现 Operator

```java
public class AggregateOperator<IN, OUT> implements Operator<IN, OUT> {
    
    @Override
    public Flux<OUT> apply(Flux<IN> input) {
        return input
            .window(Duration.ofSeconds(5))
            .flatMap(window -> window.reduce(...))
            .cast(...);
    }
}
```

#### 步骤 2：创建 Creator（添加 @Component）

```java
@Component  // 就这么简单！
public class AggregateOperatorCreator implements OperatorCreator {
    
    private final Scheduler computeScheduler;

    public AggregateOperatorCreator(@Qualifier("computeScheduler") Scheduler computeScheduler) {
        this.computeScheduler = computeScheduler;
    }

    @Override
    public Mono<Operator<?, ?>> create(OperatorConfig config) {
        return Mono.fromCallable(() -> new AggregateOperator<>(config))
            .subscribeOn(computeScheduler);
    }
    
    @Override
    public String getType() {
        return "aggregate";
    }
}
```

#### 步骤 3：完成！

不需要修改任何其他代码：
- ✅ Spring 自动扫描 `AggregateOperatorCreator`
- ✅ 自动注入到 `SpringOperatorFactory`
- ✅ 自动在 `creatorMap` 中注册

---

## 🚀 使用示例

### 完整的 Pipeline 创建和执行

```java
@Service
public class MyPipelineService {
    
    private final PipelineExecutionService executionService;

    public MyPipelineService(PipelineExecutionService executionService) {
        this.executionService = executionService;
    }

    public Mono<PipelineResult> runPipeline() {
        // 1. 创建 Graph
        StreamGraph graph = buildGraph();
        
        // 2. 执行（所有组件创建都由 Spring 管理）
        return executionService.execute(graph);
    }
    
    private StreamGraph buildGraph() {
        DefaultStreamGraph graph = new DefaultStreamGraph(
            "my-pipeline",
            "示例数据管道",
            GraphType.STREAMING
        );
        
        // 添加节点
        DefaultStreamNode sourceNode = new DefaultStreamNode(
            "source-1", "Console Source", NodeType.SOURCE
        );
        sourceNode.setConfig(Map.of(
            "type", "console",  // Spring 会自动找到 ConsoleSourceCreator
            "count", 10
        ));
        graph.addNode(sourceNode);
        
        DefaultStreamNode filterNode = new DefaultStreamNode(
            "operator-1", "Filter", NodeType.OPERATOR
        );
        filterNode.setOperatorType("FILTER");  // Spring 会自动找到 FilterOperatorCreator
        filterNode.setConfig(Map.of("name", "filter-empty"));
        graph.addNode(filterNode);
        
        DefaultStreamNode sinkNode = new DefaultStreamNode(
            "sink-1", "Console Sink", NodeType.SINK
        );
        sinkNode.setConfig(Map.of(
            "type", "console"  // Spring 会自动找到 ConsoleSinkCreator
        ));
        graph.addNode(sinkNode);
        
        // 添加边
        graph.addEdge(new DefaultStreamEdge("source-1", "operator-1"));
        graph.addEdge(new DefaultStreamEdge("operator-1", "sink-1"));
        
        return graph;
    }
}
```

---

## 📈 性能和可维护性提升

### 性能提升

| 方面 | 改进 |
|-----|------|
| 线程管理 | 针对不同场景使用专用线程池 |
| 资源利用 | IO/Compute 线程池分离，避免阻塞 |
| 扩展性 | 无需修改核心代码，性能不受组件数量影响 |

### 可维护性提升

| 方面 | 改进 |
|-----|------|
| 代码结构 | 清晰的分层架构 |
| 扩展性 | 新增组件无需修改现有代码 |
| 测试性 | 每个组件独立，易于单元测试 |
| 配置 | 线程池等参数可通过配置文件调整 |

---

## 🔍 Scheduler 使用矩阵

| 场景 | 推荐 Scheduler | 配置 Key |
|-----|---------------|---------|
| 数据库查询 | `ioScheduler` | `reactor.scheduler.io` |
| HTTP 请求 | `ioScheduler` | `reactor.scheduler.io` |
| 消息队列 | `ioScheduler` | `reactor.scheduler.io` |
| 数据转换 | `computeScheduler` | `reactor.scheduler.compute` |
| 数据计算 | `computeScheduler` | `reactor.scheduler.compute` |
| JDBC 调用 | `boundedElasticScheduler` | `reactor.scheduler.bounded-elastic` |
| 阻塞 API | `boundedElasticScheduler` | `reactor.scheduler.bounded-elastic` |
| Pipeline 执行 | `pipelineScheduler` | `reactor.scheduler.pipeline` |
| Graph 构建 | `pipelineScheduler` | `reactor.scheduler.pipeline` |

---

## 📚 相关文档

1. **DESIGN_PATTERN_EXPLANATION.md** - 详细的设计模式应用说明
2. **SPRING_REACTOR_GUIDE.md** - Spring 和 Reactor 集成指南
3. **ARCHITECTURE_EXPLANATION.md** - 整体架构说明
4. **COMPLETE_EXAMPLE.md** - 完整的使用示例

---

## ✅ 总结

### 核心改进

1. **策略模式** - 替代 switch case，符合开闭原则
2. **Spring 依赖注入** - 自动管理所有组件
3. **Reactor 线程池** - 针对不同场景优化性能
4. **清晰的架构** - 分层明确，职责清晰

### 设计原则

- ✅ 单一职责原则（SRP）
- ✅ 开闭原则（OCP）
- ✅ 里氏替换原则（LSP）
- ✅ 接口隔离原则（ISP）
- ✅ 依赖倒置原则（DIP）

### 关键优势

- 🚀 **高性能** - 专用线程池优化
- 🔧 **易扩展** - 新增组件只需一个 `@Component` 类
- 🧪 **易测试** - 组件独立，依赖注入方便 mock
- 📖 **易理解** - 清晰的设计模式和分层架构
- ⚙️ **易配置** - 通过配置文件调整参数

---

**重构完成！项目现在拥有更清晰的设计、更好的性能和更强的可扩展性！** 🎉
