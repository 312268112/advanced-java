# Pipeline Framework 完整示例

## 概述

本文档通过一个完整的端到端示例，展示如何使用 Pipeline Framework 构建和执行数据管道。

## 核心流程

```
Graph JSON → StreamGraph → GraphBasedPipelineBuilder → Pipeline → Execute
```

## 示例场景

我们将构建一个简单的数据管道：
- **Source**: 生成测试数据（ConsoleSource）
- **Operator 1**: 过滤空数据（FilterOperator）
- **Operator 2**: 转换为大写（MapOperator）
- **Sink**: 输出到控制台（ConsoleSink）

## 步骤详解

### 1. 定义 Graph JSON

首先，定义一个 StreamGraph 的 JSON 配置：

```json
{
  "graphId": "example-pipeline-001",
  "graphName": "示例数据管道",
  "graphType": "STREAMING",
  "nodes": [
    {
      "nodeId": "source-1",
      "nodeName": "测试数据源",
      "nodeType": "SOURCE",
      "config": {
        "type": "CUSTOM",
        "count": 10,
        "intervalMs": 100
      }
    },
    {
      "nodeId": "operator-1",
      "nodeName": "过滤器",
      "nodeType": "OPERATOR",
      "operatorType": "FILTER",
      "config": {
        "name": "filter-empty",
        "expression": "item != null && !item.isEmpty()"
      }
    },
    {
      "nodeId": "operator-2",
      "nodeName": "转大写",
      "nodeType": "OPERATOR",
      "operatorType": "MAP",
      "config": {
        "name": "to-uppercase",
        "expression": "item.toUpperCase()"
      }
    },
    {
      "nodeId": "sink-1",
      "nodeName": "控制台输出",
      "nodeType": "SINK",
      "config": {
        "type": "CONSOLE"
      }
    }
  ],
  "edges": [
    {
      "fromNodeId": "source-1",
      "toNodeId": "operator-1"
    },
    {
      "fromNodeId": "operator-1",
      "toNodeId": "operator-2"
    },
    {
      "fromNodeId": "operator-2",
      "toNodeId": "sink-1"
    }
  ]
}
```

### 2. 创建 StreamGraph 实例

```java
// 从 JSON 创建 StreamGraph
StreamGraph graph = StreamGraphBuilder.fromJson(jsonString);

// 或者通过编程方式创建
StreamGraph graph = new DefaultStreamGraph(
    "example-pipeline-001",
    "示例数据管道",
    GraphType.STREAMING
);

// 添加节点
StreamNode sourceNode = new DefaultStreamNode(
    "source-1",
    "测试数据源",
    NodeType.SOURCE
);
sourceNode.setConfig(Map.of(
    "type", "CUSTOM",
    "count", 10,
    "intervalMs", 100
));
graph.addNode(sourceNode);

// ... 添加其他节点和边
```

### 3. 构建 Pipeline

```java
// 初始化必要的组件
ConnectorRegistry connectorRegistry = new ConnectorRegistryImpl();
OperatorFactory operatorFactory = new OperatorFactoryImpl();

// 注册 Connector
connectorRegistry.registerConnector("console", new ConsoleConnector());

// 创建 GraphBasedPipelineBuilder
GraphBasedPipelineBuilder builder = new GraphBasedPipelineBuilder(
    connectorRegistry,
    operatorFactory
);

// 从 Graph 构建 Pipeline
Mono<Pipeline<?, ?>> pipelineMono = builder.buildFromGraph(graph);
```

### 4. 执行 Pipeline

```java
// 执行 Pipeline
pipelineMono
    .flatMap(Pipeline::execute)
    .subscribe(
        result -> {
            System.out.println("Pipeline 执行成功!");
            System.out.println("处理记录数: " + result.getRecordsProcessed());
            System.out.println("执行时间: " + result.getDuration().toMillis() + " ms");
        },
        error -> {
            System.err.println("Pipeline 执行失败: " + error.getMessage());
            error.printStackTrace();
        },
        () -> {
            System.out.println("Pipeline 执行完成");
        }
    );
```

### 5. 完整的可运行示例

```java
package com.pipeline.framework.examples;

import com.pipeline.framework.api.graph.*;
import com.pipeline.framework.connectors.ConnectorRegistry;
import com.pipeline.framework.connectors.ConnectorRegistryImpl;
import com.pipeline.framework.connectors.console.ConsoleConnector;
import com.pipeline.framework.core.builder.GraphBasedPipelineBuilder;
import com.pipeline.framework.core.pipeline.Pipeline;
import com.pipeline.framework.operators.OperatorFactory;
import com.pipeline.framework.operators.OperatorFactoryImpl;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Pipeline Framework 完整示例。
 */
public class CompleteExample {
    
    public static void main(String[] args) {
        // 1. 创建 Graph
        StreamGraph graph = buildExampleGraph();
        
        // 2. 初始化组件
        ConnectorRegistry connectorRegistry = new ConnectorRegistryImpl();
        connectorRegistry.registerConnector("console", new ConsoleConnector());
        
        OperatorFactory operatorFactory = new OperatorFactoryImpl();
        
        // 3. 创建 Builder
        GraphBasedPipelineBuilder builder = new GraphBasedPipelineBuilder(
            connectorRegistry,
            operatorFactory
        );
        
        // 4. 构建并执行 Pipeline
        builder.buildFromGraph(graph)
            .flatMap(Pipeline::execute)
            .block(); // 阻塞等待完成（仅用于演示）
    }
    
    /**
     * 构建示例 Graph。
     */
    private static StreamGraph buildExampleGraph() {
        DefaultStreamGraph graph = new DefaultStreamGraph(
            "example-pipeline-001",
            "示例数据管道",
            GraphType.STREAMING
        );
        
        // Source 节点
        DefaultStreamNode sourceNode = new DefaultStreamNode(
            "source-1",
            "测试数据源",
            NodeType.SOURCE
        );
        sourceNode.setConfig(Map.of(
            "type", "CUSTOM",
            "count", 10,
            "intervalMs", 100
        ));
        graph.addNode(sourceNode);
        
        // Filter Operator 节点
        DefaultStreamNode filterNode = new DefaultStreamNode(
            "operator-1",
            "过滤器",
            NodeType.OPERATOR
        );
        filterNode.setOperatorType("FILTER");
        filterNode.setConfig(Map.of(
            "name", "filter-empty"
        ));
        graph.addNode(filterNode);
        
        // Map Operator 节点
        DefaultStreamNode mapNode = new DefaultStreamNode(
            "operator-2",
            "转大写",
            NodeType.OPERATOR
        );
        mapNode.setOperatorType("MAP");
        mapNode.setConfig(Map.of(
            "name", "to-uppercase"
        ));
        graph.addNode(mapNode);
        
        // Sink 节点
        DefaultStreamNode sinkNode = new DefaultStreamNode(
            "sink-1",
            "控制台输出",
            NodeType.SINK
        );
        sinkNode.setConfig(Map.of(
            "type", "CONSOLE"
        ));
        graph.addNode(sinkNode);
        
        // 添加边
        graph.addEdge(new DefaultStreamEdge("source-1", "operator-1"));
        graph.addEdge(new DefaultStreamEdge("operator-1", "operator-2"));
        graph.addEdge(new DefaultStreamEdge("operator-2", "sink-1"));
        
        return graph;
    }
}
```

## 执行流程详解

### SimplePipeline 执行逻辑

```java
public Mono<PipelineResult> execute() {
    // 1. 构建响应式数据流
    Flux<?> dataFlow = source.read()              // 从 Source 读取
        .doOnNext(...)                            // 记录日志
        
    // 2. 依次通过每个 Operator
    for (Operator op : operators) {
        dataFlow = op.apply(dataFlow);            // 串联转换
    }
    
    // 3. 写入 Sink
    return sink.write(dataFlow)
        .then(...)                                // 返回结果
}
```

### GraphBasedPipelineBuilder 构建逻辑

```java
public Mono<Pipeline<?, ?>> buildFromGraph(StreamGraph graph) {
    // 1. 验证 Graph
    if (!graph.validate()) {
        return Mono.error(...);
    }
    
    // 2. 拓扑排序
    List<StreamNode> sortedNodes = graph.topologicalSort();
    
    // 3. 分类节点
    StreamNode sourceNode = findSourceNode(graph);
    List<StreamNode> operatorNodes = findOperatorNodes(sortedNodes);
    StreamNode sinkNode = findSinkNode(graph);
    
    // 4. 创建组件（响应式）
    return createSource(sourceNode)
        .flatMap(source -> 
            createOperators(operatorNodes)
                .flatMap(operators -> 
                    createSink(sinkNode)
                        .map(sink -> 
                            new SimplePipeline(name, source, operators, sink)
                        )
                )
        );
}
```

## 核心优势

### 1. 清晰的数据流

不再有 `start()` 和 `stop()` 的困扰，直接构建响应式流：

```
Source.read() → Operator1.apply() → Operator2.apply() → Sink.write()
```

### 2. 纯响应式

整个过程使用 Reactor 的 `Flux` 和 `Mono`，充分利用响应式编程的优势：
- **背压（Backpressure）**: 自动处理生产者/消费者速度不匹配
- **异步非阻塞**: 高效的资源利用
- **声明式组合**: 易于理解和维护

### 3. 可扩展

- 通过 `ConnectorRegistry` 注册自定义 Connector
- 通过 `OperatorFactory` 注册自定义 Operator
- 所有组件都是接口，易于替换和扩展

## 预期输出

```
=== Starting Pipeline: 示例数据管道 ===
Source started: 测试数据源
Operator[0] started: filter-empty
Operator[1] started: to-uppercase
[控制台输出] [1] MESSAGE-1
[控制台输出] [2] MESSAGE-2
[控制台输出] [3] MESSAGE-3
...
[控制台输出] [10] MESSAGE-10
Source completed: 测试数据源
Operator[0] completed: filter-empty
Operator[1] completed: to-uppercase
Console sink completed: 10 records written
=== Pipeline Completed: 示例数据管道 ===
Duration: 1234 ms
Records: 10
```

## 总结

通过这个完整示例，你可以看到：

1. **Graph 定义**: 声明式定义数据管道结构
2. **组件创建**: 通过 Factory 和 Registry 创建实际组件
3. **Pipeline 构建**: 将组件串联成响应式流
4. **执行**: 一行代码启动整个流程

整个过程逻辑清晰，易于理解和维护！
