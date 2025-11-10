# Pipeline Framework 架构说明

## 为什么去掉 start() 和 stop()？

### 原来的问题

在 `DefaultPipeline` 中，有这样的逻辑：

```java
public Mono<PipelineResult> execute() {
    return source.start()           // 1. 先启动 Source
        .then(sink.start())         // 2. 再启动 Sink
        .then(executePipeline())    // 3. 最后执行数据流
        .doFinally(signal -> {
            source.stop();          // 4. 停止 Source
            sink.stop();            // 5. 停止 Sink
        });
}
```

**这样做的问题**：

1. **概念混淆**: Source 和 Sink 是数据流的一部分，不应该有独立的生命周期
2. **冗余操作**: `start()` 做什么？只是为了初始化？那为什么不在构造函数或第一次读取时初始化？
3. **响应式违和**: Reactor 本身就管理订阅/取消订阅，不需要手动 start/stop
4. **复杂度增加**: 开发者需要理解两套生命周期：Reactor 的订阅模型 + 自定义的 start/stop

### 新的设计

```java
public Mono<PipelineResult> execute() {
    // 直接构建数据流
    Flux<OUT> dataFlow = buildDataFlow();
    
    // 写入 Sink
    return sink.write(dataFlow)
        .then(...)  // 返回结果
}

private Flux<OUT> buildDataFlow() {
    // 1. 从 Source 读取
    Flux<?> dataFlow = source.read();
    
    // 2. 通过 Operators
    for (Operator op : operators) {
        dataFlow = op.apply(dataFlow);
    }
    
    return dataFlow;
}
```

**优势**：

1. **语义清晰**: `execute()` = 构建流 + 执行流
2. **符合 Reactor**: 订阅时自动开始，取消时自动停止
3. **代码简洁**: 不需要管理额外的生命周期
4. **易于理解**: 新人一看就懂

## 核心架构

### 三层模型

```
┌─────────────────────────────────────────────┐
│             Graph Layer                      │
│  (StreamGraph, StreamNode, StreamEdge)      │
│  定义：JSON → Graph 对象                     │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│           Builder Layer                      │
│  (GraphBasedPipelineBuilder)                │
│  转换：Graph → 实际组件                      │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          Execution Layer                     │
│  (SimplePipeline)                           │
│  执行：组件 → 响应式流                       │
└─────────────────────────────────────────────┘
```

### Graph Layer（图层）

**职责**: 定义 Pipeline 的结构

- `StreamGraph`: 整个数据流图
- `StreamNode`: 图中的节点（Source/Operator/Sink）
- `StreamEdge`: 节点之间的连接

**示例**:

```java
StreamGraph graph = new DefaultStreamGraph("my-pipeline");
graph.addNode(sourceNode);
graph.addNode(operatorNode);
graph.addNode(sinkNode);
graph.addEdge(new StreamEdge("source", "operator"));
graph.addEdge(new StreamEdge("operator", "sink"));
```

### Builder Layer（构建层）

**职责**: 将 Graph 转换为实际的可执行组件

核心类：`GraphBasedPipelineBuilder`

**流程**:

```java
public Mono<Pipeline<?, ?>> buildFromGraph(StreamGraph graph) {
    // 1. 验证 Graph
    graph.validate();
    
    // 2. 拓扑排序（确保正确的执行顺序）
    List<StreamNode> sorted = graph.topologicalSort();
    
    // 3. 创建 Source
    DataSource<?> source = createSource(sourceNode);
    
    // 4. 创建 Operators
    List<Operator<?, ?>> operators = createOperators(operatorNodes);
    
    // 5. 创建 Sink
    DataSink<?> sink = createSink(sinkNode);
    
    // 6. 组装 Pipeline
    return new SimplePipeline(name, source, operators, sink);
}
```

**关键点**:

- 使用 `ConnectorRegistry` 查找和创建 Source/Sink
- 使用 `OperatorFactory` 创建 Operator
- 所有创建操作都是响应式的（返回 `Mono`）

### Execution Layer（执行层）

**职责**: 执行实际的数据处理

核心类：`SimplePipeline`

**流程**:

```java
public Mono<PipelineResult> execute() {
    // 1. 构建数据流
    Flux<OUT> dataFlow = source.read()           // 从 Source 读取
        .transform(operator1::apply)             // 应用 Operator1
        .transform(operator2::apply)             // 应用 Operator2
        ...;
    
    // 2. 写入 Sink
    return sink.write(dataFlow)
        .then(Mono.just(result));                // 返回结果
}
```

**关键点**:

- 使用 `Flux.transform()` 串联 Operators
- 整个过程是惰性的（Lazy），只在订阅时才执行
- 自动处理背压（Backpressure）

## 组件注册机制

### ConnectorRegistry

管理所有的 Connector（Source/Sink 的工厂）

```java
public interface ConnectorRegistry {
    Mono<Void> registerConnector(String type, Connector connector);
    Mono<Connector> getConnector(String type);
}
```

**使用**:

```java
ConnectorRegistry registry = new ConnectorRegistryImpl();

// 注册
registry.registerConnector("kafka", new KafkaConnector());
registry.registerConnector("mysql", new MysqlConnector());

// 获取
Connector connector = registry.getConnector("kafka").block();
DataSource source = connector.createSource(config).block();
```

### OperatorFactory

管理所有的 Operator 创建逻辑

```java
public interface OperatorFactory {
    Mono<Operator<?, ?>> createOperator(OperatorType type, OperatorConfig config);
}
```

**使用**:

```java
OperatorFactory factory = new OperatorFactoryImpl();

// 创建 Filter
Operator filter = factory.createOperator(
    OperatorType.FILTER,
    filterConfig
).block();

// 创建 Map
Operator map = factory.createOperator(
    OperatorType.MAP,
    mapConfig
).block();
```

## 数据流转详解

### 从 JSON 到执行

```
1. JSON 字符串
   ↓
2. StreamGraph 对象 (通过 Jackson 解析)
   ↓
3. 验证 + 拓扑排序
   ↓
4. 创建 Source (通过 ConnectorRegistry)
   ↓
5. 创建 Operators (通过 OperatorFactory)
   ↓
6. 创建 Sink (通过 ConnectorRegistry)
   ↓
7. 组装 SimplePipeline
   ↓
8. 调用 pipeline.execute()
   ↓
9. 构建响应式流: Source.read() → Ops → Sink.write()
   ↓
10. 订阅并执行
   ↓
11. 返回 PipelineResult
```

### Reactor 数据流

```
订阅时刻：
subscriber.subscribe(pipeline.execute())
    ↓
SimplePipeline.execute()
    ↓
sink.write(
    operator2.apply(
        operator1.apply(
            source.read()  ← 从这里开始产生数据
        )
    )
)
    ↓
数据从 Source 流向 Sink：
[Source] → [Operator1] → [Operator2] → [Sink]
```

**重要特性**:

1. **惰性求值**: 只有在 `subscribe()` 时才开始执行
2. **自动背压**: 如果 Sink 处理慢，会自动减缓 Source 的生成速度
3. **异步非阻塞**: 所有 I/O 操作都在后台线程池执行
4. **自动资源管理**: 订阅取消时自动清理资源

## 扩展点

### 1. 自定义 Source

```java
public class MyCustomSource implements DataSource<MyData> {
    @Override
    public Flux<MyData> read() {
        return Flux.create(sink -> {
            // 你的数据生成逻辑
            for (MyData data : fetchData()) {
                sink.next(data);
            }
            sink.complete();
        });
    }
}
```

### 2. 自定义 Operator

```java
public class MyCustomOperator implements Operator<IN, OUT> {
    @Override
    public Flux<OUT> apply(Flux<IN> input) {
        return input
            .map(this::transform)     // 转换
            .filter(this::isValid);   // 过滤
    }
}
```

### 3. 自定义 Sink

```java
public class MyCustomSink implements DataSink<MyData> {
    @Override
    public Mono<Void> write(Flux<MyData> data) {
        return data
            .buffer(100)              // 批量
            .flatMap(this::batchWrite)
            .then();
    }
}
```

## 总结

### 设计原则

1. **简单优先**: 去掉不必要的抽象（start/stop）
2. **响应式优先**: 充分利用 Reactor 的能力
3. **声明式**: Graph 定义 + 响应式流组合
4. **可扩展**: 通过 Registry 和 Factory 注册自定义组件

### 核心优势

1. **易于理解**: 清晰的三层架构
2. **易于开发**: 简单的接口，丰富的示例
3. **易于扩展**: 灵活的注册机制
4. **高性能**: 响应式非阻塞 I/O

### 适用场景

- 实时数据流处理
- ETL 数据管道
- 事件驱动架构
- 微服务间的数据集成
