# Pipeline Framework 架构重构说明

## 🎯 重构目标

1. **消除所有 switch case**：使用策略模式替代
2. **增强抽象能力**：多层接口继承，泛型支持
3. **删除无用类**：清理冗余代码
4. **提升可扩展性**：符合 SOLID 原则

---

## 📐 新的接口层次结构

### 1. 组件基础接口（最顶层）

```
Component<C>
├── ComponentType getComponentType()
├── String getName()
├── C getConfig()
└── ComponentMetadata getMetadata()
```

**职责**：定义所有组件的通用属性和行为。

### 2. 生命周期接口

```
LifecycleAware
├── Mono<Void> start()
├── Mono<Void> stop()
└── boolean isRunning()
```

**职责**：提供组件生命周期管理能力。

### 3. 流式组件接口（中间层）

```
StreamingComponent<IN, OUT, C> extends Component<C>
├── Flux<OUT> process(Flux<IN> input)
├── Class<IN> getInputType()
└── Class<OUT> getOutputType()
```

**职责**：定义流式数据处理能力，使用泛型增强类型安全。

### 4. 具体组件接口（底层）

#### DataSource

```
DataSource<OUT> extends Component<SourceConfig>, LifecycleAware
├── Flux<OUT> read()
├── SourceType getType()
└── Class<OUT> getOutputType()
```

#### Operator

```
Operator<IN, OUT> extends StreamingComponent<IN, OUT, OperatorConfig>
├── Flux<OUT> apply(Flux<IN> input)
└── OperatorType getType()
```

#### DataSink

```
DataSink<IN> extends Component<SinkConfig>, LifecycleAware
├── Mono<Void> write(Flux<IN> data)
├── Mono<Void> writeBatch(Flux<IN> data, int batchSize)
├── SinkType getType()
└── Class<IN> getInputType()
```

---

## 🚀 策略模式架构

### 1. 节点执行器（NodeExecutor）

**接口定义**：

```java
public interface NodeExecutor<T> {
    Flux<T> buildFlux(StreamNode node, NodeExecutionContext context);
    NodeType getSupportedNodeType();
    int getOrder();
}
```

**实现类**：

| 类名 | 支持的节点类型 | 职责 |
|-----|-------------|------|
| `SourceNodeExecutor` | SOURCE | 从 DataSource 读取数据 |
| `OperatorNodeExecutor` | OPERATOR | 应用算子转换 |
| `SinkNodeExecutor` | SINK | 获取上游数据流 |

**Spring 自动注册**：

```java
@Component
public class NodeExecutorRegistry {
    // Spring 自动注入所有 NodeExecutor 实现
    public NodeExecutorRegistry(List<NodeExecutor<?>> executors) {
        for (NodeExecutor<?> executor : executors) {
            executorMap.put(executor.getSupportedNodeType(), executor);
        }
    }
}
```

### 2. 执行上下文（NodeExecutionContext）

**职责**：
- 提供 Graph 和组件访问
- 缓存节点的 Flux，避免重复构建
- 存储执行过程中的上下文信息

**接口方法**：

```java
public interface NodeExecutionContext {
    StreamGraph getGraph();
    <T> Optional<DataSource<T>> getSource(String nodeId);
    <IN, OUT> Optional<Operator<IN, OUT>> getOperator(String nodeId);
    <T> Optional<DataSink<T>> getSink(String nodeId);
    <T> Optional<Flux<T>> getCachedFlux(String nodeId);
    <T> void cacheFlux(String nodeId, Flux<T> flux);
}
```

### 3. 增强的图执行器（EnhancedGraphExecutor）

**核心逻辑**：

```java
@Component
public class EnhancedGraphExecutor {
    
    private final NodeExecutorRegistry executorRegistry;

    // Spring 注入执行器注册表
    public EnhancedGraphExecutor(NodeExecutorRegistry executorRegistry) {
        this.executorRegistry = executorRegistry;
    }

    private void buildAllNodes(List<StreamNode> sortedNodes, NodeExecutionContext context) {
        for (StreamNode node : sortedNodes) {
            // 策略模式：根据节点类型获取对应的执行器
            NodeExecutor<Object> executor = executorRegistry.getExecutor(node.getNodeType());
            
            // 执行器自动处理缓存和构建逻辑
            executor.buildFlux(node, context);
        }
    }
}
```

**对比旧代码**：

```java
// ❌ 旧代码：使用 switch case
switch (node.getNodeType()) {
    case SOURCE:
        flux = buildSourceFlux(node);
        break;
    case OPERATOR:
        flux = buildOperatorFlux(node);
        break;
    case SINK:
        flux = buildOperatorFlux(node);
        break;
    default:
        throw new IllegalStateException("Unknown node type");
}

// ✅ 新代码：使用策略模式
NodeExecutor<Object> executor = executorRegistry.getExecutor(node.getNodeType());
executor.buildFlux(node, context);
```

---

## 🗑️ 删除的无用类

| 类名 | 原因 | 替代方案 |
|-----|------|---------|
| `DefaultPipeline` | 功能重复 | `SimplePipeline` |
| `GraphBasedPipelineBuilder` | 未使用 Spring | `SpringGraphBasedPipelineBuilder` |
| `PipelineBuilder` | 无实际用途 | - |
| `GraphExecutor` | 使用 switch case | `EnhancedGraphExecutor` |
| `OperatorChain` | 过度抽象 | 直接在 `SimplePipeline` 中实现 |
| `DefaultOperatorChain` | 过度抽象 | 直接在 `SimplePipeline` 中实现 |

---

## 📊 完整的架构图

```
┌─────────────────────────────────────────────────────────┐
│                     API 层（接口定义）                    │
├─────────────────────────────────────────────────────────┤
│  Component<C>                                           │
│      ├── ComponentType                                  │
│      ├── ComponentMetadata                              │
│      └── LifecycleAware                                 │
│                                                          │
│  StreamingComponent<IN, OUT, C> extends Component<C>   │
│                                                          │
│  DataSource<OUT>     Operator<IN, OUT>    DataSink<IN> │
│  extends Component   extends Streaming    extends Component│
│                                                          │
│  NodeExecutor<T>                                        │
│      ├── getSupportedNodeType()                         │
│      └── buildFlux()                                    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  Core 层（核心实现）                      │
├─────────────────────────────────────────────────────────┤
│  NodeExecutorRegistry  (管理所有 NodeExecutor)          │
│      ├── SourceNodeExecutor                             │
│      ├── OperatorNodeExecutor                           │
│      └── SinkNodeExecutor                               │
│                                                          │
│  EnhancedGraphExecutor (无 switch case！)               │
│      └── execute()                                      │
│                                                          │
│  SimplePipeline<IN, OUT>                                │
│      └── execute()                                      │
│                                                          │
│  SpringGraphBasedPipelineBuilder                        │
│      └── buildFromGraph()                               │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                 Connectors 层（具体实现）                │
├─────────────────────────────────────────────────────────┤
│  KafkaSource, ConsoleSource                             │
│  KafkaSourceCreator, ConsoleSourceCreator               │
│                                                          │
│  ConsoleSink                                            │
│  ConsoleSinkCreator                                     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                 Operators 层（具体实现）                 │
├─────────────────────────────────────────────────────────┤
│  FilterOperator, MapOperator                            │
│  FilterOperatorCreator, MapOperatorCreator              │
└─────────────────────────────────────────────────────────┘
```

---

## 🎓 设计模式应用

### 1. 策略模式（Strategy Pattern）

**应用场景**：
- `NodeExecutor` 体系：根据节点类型选择执行策略
- `ComponentCreator` 体系：根据组件类型选择创建策略

**优势**：
- ✅ 消除 switch case
- ✅ 符合开闭原则
- ✅ 易于扩展

### 2. 工厂模式（Factory Pattern）

**应用场景**：
- `SpringSourceFactory`
- `SpringSinkFactory`
- `SpringOperatorFactory`

**特点**：
- Spring 自动注入所有 Creator
- 使用 Map 存储类型到 Creator 的映射

### 3. 模板方法模式（Template Method Pattern）

**应用场景**：
- `AbstractNodeExecutor`：定义构建流程，子类实现具体逻辑

```java
public abstract class AbstractNodeExecutor<T> implements NodeExecutor<T> {
    
    @Override
    public final Flux<T> buildFlux(StreamNode node, NodeExecutionContext context) {
        // 1. 检查缓存
        // 2. 构建 Flux（模板方法）
        Flux<T> flux = doBuildFlux(node, context);
        // 3. 缓存结果
        return flux;
    }

    // 子类实现
    protected abstract Flux<T> doBuildFlux(StreamNode node, NodeExecutionContext context);
}
```

### 4. 组合模式（Composite Pattern）

**应用场景**：
- `SimplePipeline`：将 Source、Operators、Sink 组合成一个整体

---

## 🔄 泛型应用

### 1. 组件接口

```java
// 基础组件
Component<C>  // C 是配置类型

// 流式组件
StreamingComponent<IN, OUT, C>  // IN 输入，OUT 输出，C 配置
```

### 2. 具体实现

```java
// Source：只有输出类型
DataSource<OUT> extends Component<SourceConfig>

// Operator：有输入和输出类型
Operator<IN, OUT> extends StreamingComponent<IN, OUT, OperatorConfig>

// Sink：只有输入类型
DataSink<IN> extends Component<SinkConfig>
```

### 3. 执行器

```java
// 节点执行器
NodeExecutor<T>

// 具体实现
SourceNodeExecutor extends AbstractNodeExecutor<Object>
OperatorNodeExecutor extends AbstractNodeExecutor<Object>
```

---

## ✅ SOLID 原则遵守

### 1. 单一职责原则（SRP）

- `NodeExecutor`：只负责构建节点的 Flux
- `NodeExecutionContext`：只负责提供上下文信息
- `EnhancedGraphExecutor`：只负责协调执行

### 2. 开闭原则（OCP）

- 新增节点类型：添加一个 `@Component` 的 `NodeExecutor` 实现
- 新增组件类型：添加一个 `@Component` 的 `ComponentCreator` 实现
- 无需修改现有代码

### 3. 里氏替换原则（LSP）

- 所有 `NodeExecutor` 实现可互相替换
- 所有 `Component` 实现可互相替换

### 4. 接口隔离原则（ISP）

- `Component`：通用属性
- `LifecycleAware`：生命周期管理
- `StreamingComponent`：流式处理
- 客户端只依赖需要的接口

### 5. 依赖倒置原则（DIP）

- 依赖抽象（`NodeExecutor`），不依赖具体实现
- 通过 Spring 注入，实现依赖倒置

---

## 📈 性能和可维护性提升

| 方面 | 改进前 | 改进后 |
|-----|-------|--------|
| switch case 数量 | 3+ | 0 |
| 接口层次 | 1-2 层 | 4-5 层（清晰的抽象） |
| 泛型使用 | 少 | 广泛使用，类型安全 |
| 可扩展性 | 需修改代码 | 添加 @Component 即可 |
| 代码重复 | 有缓存重复逻辑 | 统一在 AbstractNodeExecutor |
| 测试性 | 较难 | 每个执行器独立测试 |

---

## 🚀 如何扩展

### 示例：添加自定义节点类型

```java
// 1. 定义新的节点类型
public enum NodeType {
    SOURCE, OPERATOR, SINK,
    CUSTOM_TRANSFORM  // 新增
}

// 2. 实现 NodeExecutor（添加 @Component）
@Component
public class CustomTransformNodeExecutor extends AbstractNodeExecutor<Object> {
    
    @Override
    protected Flux<Object> doBuildFlux(StreamNode node, NodeExecutionContext context) {
        // 实现自定义逻辑
        return Flux.just("custom");
    }
    
    @Override
    public NodeType getSupportedNodeType() {
        return NodeType.CUSTOM_TRANSFORM;
    }
}

// 3. 完成！Spring 自动发现并注册
```

---

## 📝 总结

### 核心改进

1. ✅ **消除所有 switch case**：使用策略模式
2. ✅ **增强抽象能力**：4-5 层接口继承
3. ✅ **广泛使用泛型**：类型安全
4. ✅ **删除无用类**：6 个类被删除
5. ✅ **提升可扩展性**：符合 SOLID 原则

### 关键优势

- 🚀 **易扩展**：新增类型只需添加 @Component 类
- 🧪 **易测试**：每个组件独立
- 📖 **易理解**：清晰的层次结构
- 🔧 **易维护**：低耦合、高内聚
- ⚡ **高性能**：缓存机制、响应式流

### 架构特点

- **分层清晰**：API → Core → Impl
- **职责明确**：每个类只做一件事
- **依赖倒置**：依赖抽象，不依赖具体
- **开闭原则**：对扩展开放，对修改关闭
