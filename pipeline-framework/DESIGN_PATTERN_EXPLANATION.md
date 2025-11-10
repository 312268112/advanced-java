# Pipeline Framework 设计模式详解

## 📐 设计模式应用

### 1. 策略模式（Strategy Pattern）

**问题**：如何避免 switch case 来创建不同类型的组件？

**解决方案**：使用策略模式 + Spring 依赖注入

#### 之前的代码（使用 switch case）：

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
- 每增加一个类型，就要修改这个方法（违反开闭原则）
- 代码耦合度高
- 难以测试

#### 现在的代码（使用策略模式）：

**步骤 1**: 定义策略接口

```java
public interface ComponentCreator<T, C> {
    Mono<T> create(C config);
    String getType();
    int getOrder();
}

public interface OperatorCreator extends ComponentCreator<Operator<?, ?>, OperatorConfig> {
}
```

**步骤 2**: 实现具体策略（每个类型一个）

```java
@Component  // Spring 自动扫描
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

@Component
public class MapOperatorCreator implements OperatorCreator {
    
    @Override
    public Mono<Operator<?, ?>> create(OperatorConfig config) {
        return Mono.fromCallable(() -> new MapOperator<>(config));
    }
    
    @Override
    public String getType() {
        return "map";
    }
}
```

**步骤 3**: Spring 工厂自动注入所有策略

```java
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
        
        if (creator == null) {
            return Mono.error(new IllegalArgumentException("Unsupported type: " + type));
        }
        
        return creator.create(config);
    }
}
```

**优势**：
- ✅ **开闭原则**：新增类型只需添加一个 `@Component` 类，无需修改工厂
- ✅ **低耦合**：每个策略独立，互不影响
- ✅ **易测试**：可以单独测试每个策略
- ✅ **Spring 管理**：自动发现和注入

---

### 2. 工厂模式（Factory Pattern）+ Spring IoC

**问题**：如何统一管理组件的创建？

**解决方案**：工厂模式 + Spring 依赖注入

```java
@Component
public class SpringSourceFactory {
    
    private final Map<String, SourceCreator> creatorMap;

    // Spring 自动注入所有 SourceCreator
    public SpringSourceFactory(List<SourceCreator> creators) {
        this.creatorMap = new ConcurrentHashMap<>();
        for (SourceCreator creator : creators) {
            creatorMap.put(creator.getType().toLowerCase(), creator);
        }
    }

    public Mono<DataSource<?>> createSource(SourceConfig config) {
        String type = config.getType().name().toLowerCase();
        SourceCreator creator = creatorMap.get(type);
        return creator.create(config);
    }
}
```

**使用示例**：

```java
@Component
public class SpringGraphBasedPipelineBuilder {
    
    private final SpringSourceFactory sourceFactory;
    private final SpringSinkFactory sinkFactory;
    private final SpringOperatorFactory operatorFactory;

    // Spring 自动注入三个工厂
    public SpringGraphBasedPipelineBuilder(
            SpringSourceFactory sourceFactory,
            SpringSinkFactory sinkFactory,
            SpringOperatorFactory operatorFactory) {
        this.sourceFactory = sourceFactory;
        this.sinkFactory = sinkFactory;
        this.operatorFactory = operatorFactory;
    }

    private Mono<DataSource<?>> createSource(StreamNode node) {
        SourceConfig config = parseSourceConfig(node);
        return sourceFactory.createSource(config);  // 无需 switch
    }
}
```

---

### 3. 建造者模式（Builder Pattern）

**问题**：如何优雅地构建复杂的 Pipeline？

**解决方案**：建造者模式

```java
@Component
public class SpringGraphBasedPipelineBuilder {
    
    public Mono<Pipeline<?, ?>> buildFromGraph(StreamGraph graph) {
        return Mono.defer(() -> {
            // 1. 验证
            if (!graph.validate()) {
                return Mono.error(new IllegalArgumentException("Invalid graph"));
            }
            
            // 2. 分类节点
            StreamNode sourceNode = findSourceNode(graph);
            List<StreamNode> operatorNodes = findOperatorNodes(graph);
            StreamNode sinkNode = findSinkNode(graph);
            
            // 3. 创建组件
            return createSource(sourceNode)
                .flatMap(source -> createOperators(operatorNodes)
                    .flatMap(operators -> createSink(sinkNode)
                        .map(sink -> assemblePipeline(graph, source, operators, sink))));
        });
    }
}
```

---

### 4. 模板方法模式（Template Method Pattern）

**问题**：Pipeline 执行流程固定，但具体实现不同？

**解决方案**：模板方法模式

```java
public abstract class AbstractPipeline<IN, OUT> implements Pipeline<IN, OUT> {
    
    // 模板方法：定义执行流程
    @Override
    public final Mono<PipelineResult> execute() {
        return Mono.defer(() -> {
            // 1. 执行前钩子
            return beforeExecute()
                // 2. 构建数据流
                .then(Mono.defer(this::buildDataFlow))
                // 3. 执行数据流
                .flatMap(this::executeDataFlow)
                // 4. 执行后钩子
                .flatMap(this::afterExecute);
        });
    }
    
    // 子类实现具体逻辑
    protected abstract Mono<Void> beforeExecute();
    protected abstract Flux<OUT> buildDataFlow();
    protected abstract Mono<PipelineResult> executeDataFlow(Flux<OUT> flow);
    protected abstract Mono<PipelineResult> afterExecute(PipelineResult result);
}
```

---

### 5. 观察者模式（Observer Pattern）

**问题**：如何监控 Pipeline 的执行状态？

**解决方案**：使用 Reactor 的 `doOnXxx` 操作符（内置观察者模式）

```java
public Mono<PipelineResult> execute() {
    return Mono.defer(() -> {
        Flux<OUT> dataFlow = buildDataFlow();
        
        return sink.write(dataFlow)
            .doOnSubscribe(s -> notifyListeners(PipelineEvent.STARTED))
            .doOnNext(data -> notifyListeners(PipelineEvent.PROCESSING, data))
            .doOnComplete(() -> notifyListeners(PipelineEvent.COMPLETED))
            .doOnError(e -> notifyListeners(PipelineEvent.FAILED, e));
    });
}
```

---

## 🔧 Spring 注解应用

### 1. 组件扫描

```java
// Source Creator
@Component
public class KafkaSourceCreator implements SourceCreator {
    // Spring 自动扫描并注册
}

// Sink Creator
@Component
public class ConsoleSinkCreator implements SinkCreator {
    // Spring 自动扫描并注册
}

// Operator Creator
@Component
public class FilterOperatorCreator implements OperatorCreator {
    // Spring 自动扫描并注册
}
```

### 2. 依赖注入

```java
@Component
public class ConsoleSourceCreator implements SourceCreator {
    
    private final Scheduler ioScheduler;

    // 构造函数注入
    public ConsoleSourceCreator(@Qualifier("ioScheduler") Scheduler ioScheduler) {
        this.ioScheduler = ioScheduler;
    }
}
```

### 3. 配置管理

```java
@Component
@ConfigurationProperties(prefix = "reactor.scheduler")
public class ReactorSchedulerProperties {
    private SchedulerConfig io;
    private SchedulerConfig compute;
    // Spring 自动绑定配置
}
```

### 4. Bean 管理

```java
@Configuration
public class ReactorSchedulerConfig {
    
    @Bean(name = "ioScheduler", destroyMethod = "dispose")
    public Scheduler ioScheduler(ReactorSchedulerProperties properties) {
        return Schedulers.newBoundedElastic(...);
    }
    
    @Bean(name = "computeScheduler", destroyMethod = "dispose")
    public Scheduler computeScheduler(ReactorSchedulerProperties properties) {
        return Schedulers.newParallel(...);
    }
}
```

### 5. 服务层

```java
@Service
public class PipelineExecutionService {
    
    private final SpringGraphBasedPipelineBuilder pipelineBuilder;
    private final Scheduler pipelineScheduler;

    public PipelineExecutionService(
            SpringGraphBasedPipelineBuilder pipelineBuilder,
            @Qualifier("pipelineScheduler") Scheduler pipelineScheduler) {
        this.pipelineBuilder = pipelineBuilder;
        this.pipelineScheduler = pipelineScheduler;
    }

    public Mono<PipelineResult> execute(StreamGraph graph) {
        return pipelineBuilder.buildFromGraph(graph)
            .flatMap(Pipeline::execute)
            .subscribeOn(pipelineScheduler);
    }
}
```

---

## 🎯 Reactor 线程池配置

### 1. 配置文件

```yaml
reactor:
  scheduler:
    # IO 密集型操作
    io:
      pool-size: 100
      queue-size: 1000
      thread-name-prefix: reactor-io-
    
    # CPU 密集型操作
    compute:
      pool-size: 0  # 0 = CPU 核心数
      thread-name-prefix: reactor-compute-
    
    # 阻塞操作包装
    bounded-elastic:
      pool-size: 200
      queue-size: 10000
      ttl-seconds: 60
      thread-name-prefix: reactor-bounded-
    
    # Pipeline 执行专用
    pipeline:
      pool-size: 50
      queue-size: 500
      thread-name-prefix: pipeline-exec-
```

### 2. Scheduler 使用场景

| Scheduler | 使用场景 | 示例 |
|-----------|---------|------|
| `ioScheduler` | IO 密集型操作 | 数据库查询、HTTP 请求、消息队列 |
| `computeScheduler` | CPU 密集型操作 | 数据转换、计算、聚合 |
| `boundedElasticScheduler` | 阻塞操作包装 | JDBC 调用、同步第三方库 |
| `pipelineScheduler` | Pipeline 执行 | Graph 构建、Pipeline 执行 |

### 3. 使用示例

```java
@Component
public class ConsoleSourceCreator implements SourceCreator {
    
    private final Scheduler ioScheduler;

    public ConsoleSourceCreator(@Qualifier("ioScheduler") Scheduler ioScheduler) {
        this.ioScheduler = ioScheduler;
    }

    @Override
    public Mono<DataSource<?>> create(SourceConfig config) {
        return Mono.fromCallable(() -> {
            // 创建逻辑
            return new ConsoleSource(config);
        })
        .subscribeOn(ioScheduler);  // 在 IO 线程池执行
    }
}
```

---

## 📊 架构对比

### 之前（使用 switch case）

```
GraphBuilder
    ↓
switch (type) {
    case SOURCE_A: return new SourceA();
    case SOURCE_B: return new SourceB();
    ...
}
```

**问题**：
- ❌ 违反开闭原则
- ❌ 代码耦合度高
- ❌ 难以扩展
- ❌ 测试困难

### 现在（使用设计模式 + Spring）

```
Spring 容器启动
    ↓
自动扫描所有 @Component
    ↓
注入到 Factory
    ↓
Factory.create(config)
    ↓
根据 type 查找 Creator
    ↓
Creator.create(config)
```

**优势**：
- ✅ 符合开闭原则
- ✅ 低耦合、高内聚
- ✅ 易于扩展
- ✅ 便于测试
- ✅ Spring 自动管理

---

## 🚀 如何添加新组件？

### 示例：添加一个新的 Source

**步骤 1**：实现 `DataSource` 接口

```java
public class MyCustomSource implements DataSource<MyData> {
    @Override
    public Flux<MyData> read() {
        return Flux.just(new MyData());
    }
}
```

**步骤 2**：创建 Creator（添加 `@Component`）

```java
@Component  // 这就够了！Spring 会自动发现
public class MyCustomSourceCreator implements SourceCreator {
    
    @Override
    public Mono<DataSource<?>> create(SourceConfig config) {
        return Mono.just(new MyCustomSource());
    }
    
    @Override
    public String getType() {
        return "mycustom";  // 定义类型标识
    }
}
```

**步骤 3**：完成！

不需要修改任何其他代码，Spring 会自动：
1. 扫描到 `MyCustomSourceCreator`
2. 注入到 `SpringSourceFactory`
3. 在 `creatorMap` 中注册

---

## 📝 总结

### 核心改进

1. **策略模式替代 switch case**：每个类型一个策略类
2. **Spring 依赖注入**：自动发现和管理所有组件
3. **Reactor 线程池配置**：针对不同场景使用不同的 Scheduler
4. **开闭原则**：扩展无需修改现有代码
5. **可测试性**：每个组件独立，易于单元测试

### 设计原则

- ✅ 单一职责原则（SRP）
- ✅ 开闭原则（OCP）
- ✅ 依赖倒置原则（DIP）
- ✅ 接口隔离原则（ISP）
