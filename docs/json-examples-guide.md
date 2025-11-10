# StreamGraph JSON配置示例指南

## 概述

本文档提供了7个完整的、可直接使用的StreamGraph JSON配置示例，涵盖常见的ETL场景。

完整的JSON文件位于：`graph-definition-json-examples.json`

## 示例列表

### 1. 简单ETL - Kafka到MySQL

**场景**: 从Kafka读取用户事件，解析JSON后写入MySQL

**数据流程**:
```
Kafka Source → Parse JSON → Filter → MySQL Sink
```

**适用场景**:
- 基础数据采集
- 消息队列到数据库同步
- 实时数据入库

**关键配置**:
```json
{
  "source": "KAFKA_SOURCE",
  "operators": ["MAP", "FILTER"],
  "sink": "JDBC_SINK"
}
```

---

### 2. 实时统计 - 窗口聚合

**场景**: 实时统计每5分钟各城市的订单数和金额

**数据流程**:
```
Kafka Source → Parse → Window(5m) → Aggregate → MySQL + Redis
```

**适用场景**:
- 实时监控大屏
- 业务指标统计
- 实时报表

**特点**:
- ✅ 有状态计算（Window + Aggregate）
- ✅ 多Sink输出（数据库 + 缓存）
- ✅ 支持检查点容错

**聚合函数**:
- COUNT: 订单数量
- SUM: 总金额
- AVG: 平均金额
- MAX: 最大金额

---

### 3. 数据清洗 - 去重和转换

**场景**: 从数据库读取数据，去重、转换后写入数据仓库

**数据流程**:
```
JDBC Source → Deduplicate → Transform → Filter → JDBC Sink
```

**适用场景**:
- 数据同步
- 离线数据处理
- 数据仓库ETL

**特点**:
- ✅ 增量读取（基于时间戳）
- ✅ 去重操作（DEDUPLICATE）
- ✅ UPSERT写入模式
- ✅ 事务支持

---

### 4. 多分支处理 - 日志分流

**场景**: 读取日志流，按日志级别分流到不同的存储

**数据流程**:
```
                  ┌→ Filter(ERROR) → HTTP Alert
Kafka Source ────┼→ Filter(WARN)  → MySQL
                  └→ All Logs     → Elasticsearch
```

**适用场景**:
- 日志分析
- 告警系统
- 日志归档

**特点**:
- ✅ 一个Source多个Sink
- ✅ 条件分支处理
- ✅ 不同级别不同处理策略

---

### 5. API数据采集

**场景**: 定期从HTTP API拉取数据并存储

**数据流程**:
```
HTTP Source → FlatMap → Map → JDBC Sink
```

**适用场景**:
- 第三方API数据同步
- 定时数据拉取
- 外部数据集成

**特点**:
- ✅ 周期性拉取（poll_interval）
- ✅ 数组展开（FlatMap）
- ✅ 字段映射
- ✅ 重试机制

---

### 6. 文件处理 - CSV到JSON

**场景**: 读取CSV文件，转换为JSON后写入Kafka和归档

**数据流程**:
```
                    ┌→ Kafka Sink
File Source → Map ─┤
                    └→ File Sink (JSON)
```

**适用场景**:
- 文件导入
- 数据格式转换
- 批量数据处理

**特点**:
- ✅ 文件监控（watch_mode）
- ✅ CSV解析
- ✅ 多目标输出
- ✅ 文件归档

---

### 7. 数据关联 - JOIN操作

**场景**: 订单流关联用户信息和商品信息

**数据流程**:
```
Kafka Source → Parse → Join(User) → Join(Product) → ES Sink
```

**适用场景**:
- 数据补全
- 维度关联
- 实时宽表

**特点**:
- ✅ 多次JOIN操作
- ✅ 支持缓存（提高性能）
- ✅ 从MySQL/Redis读取维度数据
- ✅ 字段别名

---

## 如何使用这些示例

### 方法1: 直接插入数据库

```sql
-- 插入StreamGraph
INSERT INTO etl_stream_graph (graph_id, graph_name, job_id, graph_definition)
VALUES (
    'graph-001',
    '简单ETL任务',
    'job-001',
    '这里粘贴完整的graph_definition JSON'
);
```

### 方法2: 通过API创建

```bash
curl -X POST http://localhost:8080/api/stream-graphs \
  -H "Content-Type: application/json" \
  -d @graph-definition-json-examples.json
```

### 方法3: 使用可视化界面

1. 登录Web管理界面
2. 点击"创建任务"
3. 选择"导入JSON"
4. 粘贴对应的graph_definition
5. 保存并提交

## 配置说明

### 常用配置项

#### Source配置
```json
{
  "datasource_id": "数据源ID（在etl_datasource表中）",
  "topics": ["Kafka主题列表"],
  "group_id": "消费者组ID",
  "poll_interval_seconds": "轮询间隔（秒）"
}
```

#### Operator配置
```json
{
  "function_class": "自定义函数类全限定名",
  "predicate_expression": "过滤条件表达式",
  "group_by_fields": ["分组字段"],
  "window_size": "窗口大小（如5m、1h）"
}
```

#### Sink配置
```json
{
  "datasource_id": "目标数据源ID",
  "table": "目标表名",
  "batch_size": 100,
  "write_mode": "INSERT/UPSERT/UPDATE"
}
```

### 全局配置

```json
{
  "buffer_size": 1000,
  "backpressure_strategy": "BUFFER/DROP/ERROR",
  "checkpoint_enabled": true,
  "checkpoint_interval_seconds": 60
}
```

## 节点类型速查

| 节点类型 | operator_type | 说明 |
| --- | --- | --- |
| Source | KAFKA_SOURCE | Kafka数据源 |
| Source | JDBC_SOURCE | 数据库数据源 |
| Source | HTTP_SOURCE | HTTP API数据源 |
| Source | FILE_SOURCE | 文件数据源 |
| Operator | MAP | 一对一转换 |
| Operator | FILTER | 数据过滤 |
| Operator | FLATMAP | 一对多转换 |
| Operator | AGGREGATE | 聚合计算 |
| Operator | WINDOW | 窗口计算 |
| Operator | JOIN | 数据关联 |
| Operator | DEDUPLICATE | 数据去重 |
| Sink | JDBC_SINK | 数据库写入 |
| Sink | KAFKA_SINK | Kafka写入 |
| Sink | ELASTICSEARCH_SINK | ES写入 |
| Sink | FILE_SINK | 文件写入 |
| Sink | REDIS_SINK | Redis写入 |
| Sink | HTTP_SINK | HTTP API写入 |

## 配置模板

### 最小配置（必填字段）

```json
{
  "version": "1.0",
  "nodes": [
    {
      "node_id": "必填-唯一标识",
      "node_name": "必填-显示名称",
      "node_type": "必填-SOURCE/OPERATOR/SINK",
      "operator_type": "必填-具体算子类型",
      "config": {}
    }
  ],
  "edges": [
    {
      "edge_id": "必填-唯一标识",
      "source_node_id": "必填-源节点ID",
      "target_node_id": "必填-目标节点ID"
    }
  ]
}
```

### 完整配置（包含可选字段）

```json
{
  "version": "1.0",
  "nodes": [...],
  "edges": [...],
  "global_config": {
    "buffer_size": 1000,
    "backpressure_strategy": "BUFFER",
    "checkpoint_enabled": true,
    "checkpoint_interval_seconds": 60,
    "restart_on_failure": true,
    "max_restart_attempts": 3,
    "error_handling": {
      "on_source_error": "RETRY",
      "on_operator_error": "SKIP",
      "on_sink_error": "FAIL"
    }
  }
}
```

## 常见问题

### Q1: 如何添加自定义算子？

修改nodes中的operator配置：
```json
{
  "operator_type": "MAP",
  "config": {
    "function_class": "com.yourcompany.YourCustomFunction",
    "function_config": {
      "param1": "value1"
    }
  }
}
```

### Q2: 如何实现一个Source多个Sink？

添加多个edge指向不同的Sink：
```json
{
  "edges": [
    {"source_node_id": "op-001", "target_node_id": "sink-001"},
    {"source_node_id": "op-001", "target_node_id": "sink-002"},
    {"source_node_id": "op-001", "target_node_id": "sink-003"}
  ]
}
```

### Q3: 如何配置检查点？

在global_config中设置：
```json
{
  "global_config": {
    "checkpoint_enabled": true,
    "checkpoint_interval_seconds": 60
  }
}
```

### Q4: 数据源ID在哪里配置？

数据源需要先在`etl_datasource`表中创建，然后在配置中引用其datasource_id。

### Q5: 如何调试配置？

1. 使用JSON验证工具检查语法
2. 先创建简单的任务测试
3. 查看任务执行日志
4. 使用监控指标分析性能

## 下一步

- 查看完整的JSON文件：`graph-definition-json-examples.json`
- 阅读详细的配置说明：`graph-definition-examples.md`
- 参考数据库设计文档：`database-design.md`
- 查看系统设计文档：`reactive-etl-framework-design.md`

---

**文档版本**: v1.0  
**最后更新**: 2025-11-09
