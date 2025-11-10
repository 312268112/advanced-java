# StreamGraph定义改进方案

## 问题：上下游关系不清晰

当前的graph_definition设计中，节点和边是分离的，不够直观：

```json
{
  "nodes": [
    {"node_id": "node-1", ...},
    {"node_id": "node-2", ...},
    {"node_id": "node-3", ...}
  ],
  "edges": [
    {"source_node_id": "node-1", "target_node_id": "node-2"},
    {"source_node_id": "node-2", "target_node_id": "node-3"}
  ]
}
```

**问题**：
- ❌ 需要在edges中查找才能知道上下游关系
- ❌ 节点多了之后很难追踪数据流向
- ❌ 修改连接关系容易出错

## 解决方案1：在节点中添加上下游信息（推荐）

### 方案A：添加辅助字段

在每个节点中添加`upstream_nodes`和`downstream_nodes`字段：

```json
{
  "version": "1.0",
  "nodes": [
    {
      "node_id": "source-001",
      "node_name": "Kafka数据源",
      "node_type": "SOURCE",
      "operator_type": "KAFKA_SOURCE",
      "upstream_nodes": [],
      "downstream_nodes": ["op-parse-001"],
      "config": {...}
    },
    {
      "node_id": "op-parse-001",
      "node_name": "解析JSON",
      "node_type": "OPERATOR",
      "operator_type": "MAP",
      "upstream_nodes": ["source-001"],
      "downstream_nodes": ["op-filter-001"],
      "config": {...}
    },
    {
      "node_id": "op-filter-001",
      "node_name": "过滤",
      "node_type": "OPERATOR",
      "operator_type": "FILTER",
      "upstream_nodes": ["op-parse-001"],
      "downstream_nodes": ["sink-001"],
      "config": {...}
    },
    {
      "node_id": "sink-001",
      "node_name": "写入MySQL",
      "node_type": "SINK",
      "operator_type": "JDBC_SINK",
      "upstream_nodes": ["op-filter-001"],
      "downstream_nodes": [],
      "config": {...}
    }
  ],
  "edges": [
    {"edge_id": "edge-001", "source_node_id": "source-001", "target_node_id": "op-parse-001"},
    {"edge_id": "edge-002", "source_node_id": "op-parse-001", "target_node_id": "op-filter-001"},
    {"edge_id": "edge-003", "source_node_id": "op-filter-001", "target_node_id": "sink-001"}
  ]
}
```

**优点**：
- ✅ 一眼就能看出节点的上下游
- ✅ 保留edges定义，用于详细配置
- ✅ upstream_nodes和downstream_nodes可以从edges自动生成

**缺点**：
- ⚠️ 信息有冗余（需要保持一致性）

### 方案B：嵌套结构（链式定义）

直接在节点中定义下游节点：

```json
{
  "version": "1.0",
  "pipeline": {
    "source": {
      "node_id": "source-001",
      "node_name": "Kafka数据源",
      "operator_type": "KAFKA_SOURCE",
      "config": {...},
      "next": {
        "node_id": "op-parse-001",
        "node_name": "解析JSON",
        "operator_type": "MAP",
        "config": {...},
        "next": {
          "node_id": "op-filter-001",
          "node_name": "过滤",
          "operator_type": "FILTER",
          "config": {...},
          "next": {
            "node_id": "sink-001",
            "node_name": "写入MySQL",
            "operator_type": "JDBC_SINK",
            "config": {...}
          }
        }
      }
    }
  }
}
```

**优点**：
- ✅ 数据流向非常清晰
- ✅ 适合简单的线性流程

**缺点**：
- ❌ 不支持多分支
- ❌ 不支持复杂的DAG结构

## 解决方案2：使用可视化标注

在JSON中添加注释和序号：

```json
{
  "version": "1.0",
  "flow_description": "Kafka → Parse → Filter → MySQL",
  "nodes": [
    {
      "node_id": "source-001",
      "node_name": "Kafka数据源",
      "node_type": "SOURCE",
      "operator_type": "KAFKA_SOURCE",
      "sequence": 1,
      "description": "第一步：从Kafka读取数据",
      "config": {...}
    },
    {
      "node_id": "op-parse-001",
      "node_name": "解析JSON",
      "node_type": "OPERATOR",
      "operator_type": "MAP",
      "sequence": 2,
      "description": "第二步：解析JSON数据，输入来自source-001",
      "config": {...}
    },
    {
      "node_id": "op-filter-001",
      "node_name": "过滤",
      "node_type": "OPERATOR",
      "operator_type": "FILTER",
      "sequence": 3,
      "description": "第三步：过滤有效数据，输入来自op-parse-001",
      "config": {...}
    },
    {
      "node_id": "sink-001",
      "node_name": "写入MySQL",
      "node_type": "SINK",
      "operator_type": "JDBC_SINK",
      "sequence": 4,
      "description": "第四步：写入MySQL，输入来自op-filter-001",
      "config": {...}
    }
  ],
  "edges": [
    {
      "edge_id": "edge-001",
      "source_node_id": "source-001",
      "target_node_id": "op-parse-001",
      "description": "Kafka数据源 → 解析JSON"
    },
    {
      "edge_id": "edge-002",
      "source_node_id": "op-parse-001",
      "target_node_id": "op-filter-001",
      "description": "解析JSON → 过滤"
    },
    {
      "edge_id": "edge-003",
      "source_node_id": "op-filter-001",
      "target_node_id": "sink-001",
      "description": "过滤 → 写入MySQL"
    }
  ]
}
```

## 解决方案3：辅助工具类

提供工具方法快速查询节点关系：

```java
public class GraphHelper {
    
    /**
     * 获取节点的上游节点列表
     */
    public static List<String> getUpstreamNodes(String nodeId, StreamGraph graph) {
        return graph.getEdges().stream()
            .filter(edge -> edge.getTargetNodeId().equals(nodeId))
            .map(Edge::getSourceNodeId)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取节点的下游节点列表
     */
    public static List<String> getDownstreamNodes(String nodeId, StreamGraph graph) {
        return graph.getEdges().stream()
            .filter(edge -> edge.getSourceNodeId().equals(nodeId))
            .map(Edge::getTargetNodeId)
            .collect(Collectors.toList());
    }
    
    /**
     * 打印节点的上下游关系
     */
    public static void printNodeRelations(StreamGraph graph) {
        graph.getNodes().forEach(node -> {
            List<String> upstream = getUpstreamNodes(node.getNodeId(), graph);
            List<String> downstream = getDownstreamNodes(node.getNodeId(), graph);
            
            System.out.printf("节点: %s (%s)\n", node.getNodeName(), node.getNodeId());
            System.out.printf("  ← 上游: %s\n", upstream.isEmpty() ? "无" : String.join(", ", upstream));
            System.out.printf("  → 下游: %s\n", downstream.isEmpty() ? "无" : String.join(", ", downstream));
            System.out.println();
        });
    }
    
    /**
     * 生成Mermaid流程图
     */
    public static String generateMermaidDiagram(StreamGraph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph LR\n");
        
        // 节点定义
        graph.getNodes().forEach(node -> {
            sb.append(String.format("    %s[%s]\n", 
                node.getNodeId(), 
                node.getNodeName()
            ));
        });
        
        // 边定义
        graph.getEdges().forEach(edge -> {
            sb.append(String.format("    %s --> %s\n",
                edge.getSourceNodeId(),
                edge.getTargetNodeId()
            ));
        });
        
        return sb.toString();
    }
}
```

使用示例：

```java
// 加载StreamGraph
StreamGraph graph = loadFromDatabase(graphId);

// 打印节点关系
GraphHelper.printNodeRelations(graph);

// 输出：
// 节点: Kafka数据源 (source-001)
//   ← 上游: 无
//   → 下游: op-parse-001
//
// 节点: 解析JSON (op-parse-001)
//   ← 上游: source-001
//   → 下游: op-filter-001
//
// 节点: 过滤 (op-filter-001)
//   ← 上游: op-parse-001
//   → 下游: sink-001
//
// 节点: 写入MySQL (sink-001)
//   ← 上游: op-filter-001
//   → 下游: 无

// 生成可视化图表
String mermaid = GraphHelper.generateMermaidDiagram(graph);
System.out.println(mermaid);
```

## 推荐的最佳实践

### 方案：混合使用（推荐）

**1. JSON中添加辅助信息**

```json
{
  "version": "1.0",
  "metadata": {
    "name": "用户事件ETL",
    "description": "从Kafka读取用户事件，解析后写入MySQL",
    "flow_diagram": "Kafka → Parse → Filter → MySQL"
  },
  "nodes": [
    {
      "node_id": "source-001",
      "node_name": "Kafka数据源",
      "node_type": "SOURCE",
      "operator_type": "KAFKA_SOURCE",
      "position": {"x": 100, "y": 100},
      "config": {...}
    },
    {
      "node_id": "op-parse-001",
      "node_name": "解析JSON",
      "node_type": "OPERATOR",
      "operator_type": "MAP",
      "position": {"x": 300, "y": 100},
      "config": {...}
    }
  ],
  "edges": [
    {
      "edge_id": "edge-001",
      "source_node_id": "source-001",
      "target_node_id": "op-parse-001",
      "label": "原始数据"
    }
  ]
}
```

**2. 数据库表添加辅助字段**

修改`etl_stream_graph`表：

```sql
ALTER TABLE etl_stream_graph 
ADD COLUMN flow_diagram TEXT COMMENT '流程图描述',
ADD COLUMN node_relations JSON COMMENT '节点关系映射';
```

存储时自动生成node_relations：

```json
{
  "source-001": {
    "upstream": [],
    "downstream": ["op-parse-001"]
  },
  "op-parse-001": {
    "upstream": ["source-001"],
    "downstream": ["op-filter-001"]
  },
  "op-filter-001": {
    "upstream": ["op-parse-001"],
    "downstream": ["sink-001"]
  },
  "sink-001": {
    "upstream": ["op-filter-001"],
    "downstream": []
  }
}
```

**3. 提供可视化界面**

在Web管理界面提供图形化编辑器：

```
┌─────────────────────────────────────────────┐
│  ETL任务可视化编辑器                          │
├─────────────────────────────────────────────┤
│                                             │
│   ┌─────────┐    ┌─────────┐    ┌────────┐│
│   │ Kafka   │───▶│  Parse  │───▶│ Filter ││
│   │ Source  │    │  JSON   │    │        ││
│   └─────────┘    └─────────┘    └────────┘│
│                                      │      │
│                                      ▼      │
│                                  ┌────────┐│
│                                  │ MySQL  ││
│                                  │  Sink  ││
│                                  └────────┘│
│                                             │
└─────────────────────────────────────────────┘
```

## 完整示例：带关系信息的JSON

```json
{
  "version": "1.0",
  "metadata": {
    "name": "用户事件实时处理",
    "description": "从Kafka读取用户事件，解析、过滤后写入MySQL",
    "flow_diagram": "Kafka Source → Parse JSON → Filter Valid → MySQL Sink",
    "created_by": "admin",
    "created_at": "2025-11-09T10:00:00Z"
  },
  "nodes": [
    {
      "node_id": "source-001",
      "node_name": "Kafka用户事件源",
      "node_type": "SOURCE",
      "operator_type": "KAFKA_SOURCE",
      "position": {"x": 100, "y": 100},
      "upstream": [],
      "downstream": ["op-parse-001"],
      "config": {
        "datasource_id": "kafka-prod",
        "topics": ["user-events"],
        "group_id": "user-etl"
      }
    },
    {
      "node_id": "op-parse-001",
      "node_name": "解析JSON",
      "node_type": "OPERATOR",
      "operator_type": "MAP",
      "position": {"x": 300, "y": 100},
      "upstream": ["source-001"],
      "downstream": ["op-filter-001"],
      "config": {
        "function_class": "com.example.ParseJsonFunction"
      }
    },
    {
      "node_id": "op-filter-001",
      "node_name": "过滤有效数据",
      "node_type": "OPERATOR",
      "operator_type": "FILTER",
      "position": {"x": 500, "y": 100},
      "upstream": ["op-parse-001"],
      "downstream": ["sink-001"],
      "config": {
        "predicate_expression": "user_id != null && event_type != null"
      }
    },
    {
      "node_id": "sink-001",
      "node_name": "MySQL用户事件表",
      "node_type": "SINK",
      "operator_type": "JDBC_SINK",
      "position": {"x": 700, "y": 100},
      "upstream": ["op-filter-001"],
      "downstream": [],
      "config": {
        "datasource_id": "mysql-warehouse",
        "table": "user_events",
        "batch_size": 100
      }
    }
  ],
  "edges": [
    {
      "edge_id": "edge-001",
      "source_node_id": "source-001",
      "target_node_id": "op-parse-001",
      "label": "原始消息",
      "description": "从Kafka读取的原始JSON消息"
    },
    {
      "edge_id": "edge-002",
      "source_node_id": "op-parse-001",
      "target_node_id": "op-filter-001",
      "label": "解析后数据",
      "description": "解析后的结构化数据"
    },
    {
      "edge_id": "edge-003",
      "source_node_id": "op-filter-001",
      "target_node_id": "sink-001",
      "label": "有效数据",
      "description": "过滤后的有效用户事件"
    }
  ],
  "global_config": {
    "buffer_size": 1000,
    "checkpoint_enabled": true
  }
}
```

## 查询节点关系的SQL

```sql
-- 查询节点及其上下游关系
SELECT 
    node_id,
    node_name,
    upstream,
    downstream
FROM (
    SELECT 
        node_id,
        node_name,
        JSON_EXTRACT(graph_definition, CONCAT('$.nodes[', idx, '].upstream')) as upstream,
        JSON_EXTRACT(graph_definition, CONCAT('$.nodes[', idx, '].downstream')) as downstream
    FROM etl_stream_graph,
    JSON_TABLE(
        graph_definition,
        '$.nodes[*]' COLUMNS (
            idx FOR ORDINALITY,
            node_id VARCHAR(64) PATH '$.node_id',
            node_name VARCHAR(128) PATH '$.node_name'
        )
    ) AS nodes_table
    WHERE graph_id = 'your-graph-id'
) AS node_relations;
```

## 总结

**最佳方案组合**：

1. ✅ 在nodes中添加`upstream`和`downstream`字段（冗余但直观）
2. ✅ 保留edges定义（用于详细配置）
3. ✅ 添加`metadata`和`flow_diagram`（总览描述）
4. ✅ 添加`position`坐标（用于可视化）
5. ✅ 在edge中添加`label`和`description`（说明数据流）
6. ✅ 提供工具类快速查询关系
7. ✅ 提供Web可视化编辑器

这样既保持了灵活性，又提高了可读性！

---

**文档版本**: v1.0  
**最后更新**: 2025-11-09
