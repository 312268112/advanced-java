# StreamGraph定义结构说明

## 1. graph_definition结构概述

`graph_definition`是JSON格式，存储StreamGraph的完整定义，包括节点（nodes）和边（edges）。

### 1.1 基本结构

```json
{
  "version": "1.0",
  "nodes": [
    {
      "node_id": "唯一节点ID",
      "node_name": "节点名称",
      "node_type": "SOURCE/OPERATOR/SINK",
      "operator_type": "具体算子类型",
      "config": {
        "算子特定配置": "..."
      }
    }
  ],
  "edges": [
    {
      "edge_id": "边ID",
      "source_node_id": "源节点ID",
      "target_node_id": "目标节点ID"
    }
  ],
  "global_config": {
    "全局配置": "..."
  }
}
```

## 2. 节点类型详解

### 2.1 SOURCE节点

Source节点定义数据源。

```json
{
  "node_id": "source-kafka-001",
  "node_name": "用户事件源",
  "node_type": "SOURCE",
  "operator_type": "KAFKA_SOURCE",
  "config": {
    "datasource_id": "kafka-prod-cluster",
    "topics": ["user-events", "user-actions"],
    "group_id": "etl-consumer-group-1",
    "auto_offset_reset": "latest",
    "poll_timeout_ms": 1000,
    "max_poll_records": 500,
    "enable_auto_commit": false,
    "properties": {
      "max.partition.fetch.bytes": "1048576"
    }
  }
}
```

**常见Source类型**:

#### JDBC_SOURCE
```json
{
  "node_id": "source-mysql-001",
  "node_name": "订单数据源",
  "node_type": "SOURCE",
  "operator_type": "JDBC_SOURCE",
  "config": {
    "datasource_id": "mysql-prod",
    "query": "SELECT * FROM orders WHERE updated_at > ? AND updated_at <= ?",
    "fetch_size": 1000,
    "poll_interval_seconds": 60,
    "timestamp_column": "updated_at",
    "start_timestamp": "2025-01-01 00:00:00"
  }
}
```

#### HTTP_SOURCE
```json
{
  "node_id": "source-api-001",
  "node_name": "API数据源",
  "node_type": "SOURCE",
  "operator_type": "HTTP_SOURCE",
  "config": {
    "url": "https://api.example.com/data",
    "method": "GET",
    "headers": {
      "Authorization": "Bearer ${token}",
      "Content-Type": "application/json"
    },
    "poll_interval_seconds": 30,
    "timeout_seconds": 10,
    "retry_times": 3
  }
}
```

#### FILE_SOURCE
```json
{
  "node_id": "source-file-001",
  "node_name": "CSV文件源",
  "node_type": "SOURCE",
  "operator_type": "FILE_SOURCE",
  "config": {
    "path": "/data/input/*.csv",
    "format": "CSV",
    "charset": "UTF-8",
    "delimiter": ",",
    "has_header": true,
    "watch_mode": "CONTINUOUS",
    "scan_interval_seconds": 10
  }
}
```

### 2.2 OPERATOR节点

Operator节点定义数据转换操作。

#### MAP算子
```json
{
  "node_id": "operator-map-001",
  "node_name": "解析JSON",
  "node_type": "OPERATOR",
  "operator_type": "MAP",
  "config": {
    "function_class": "com.example.etl.function.ParseJsonFunction",
    "function_config": {
      "output_fields": ["user_id", "event_type", "timestamp", "properties"]
    }
  }
}
```

#### FILTER算子
```json
{
  "node_id": "operator-filter-001",
  "node_name": "过滤活跃用户",
  "node_type": "OPERATOR",
  "operator_type": "FILTER",
  "config": {
    "predicate_class": "com.example.etl.predicate.ActiveUserPredicate",
    "predicate_expression": "user.is_active == true AND user.register_days > 7"
  }
}
```

#### FLATMAP算子
```json
{
  "node_id": "operator-flatmap-001",
  "node_name": "拆分数组",
  "node_type": "OPERATOR",
  "operator_type": "FLATMAP",
  "config": {
    "function_class": "com.example.etl.function.SplitArrayFunction",
    "source_field": "tags",
    "output_field": "tag"
  }
}
```

#### AGGREGATE算子（有状态）
```json
{
  "node_id": "operator-aggregate-001",
  "node_name": "按城市聚合",
  "node_type": "OPERATOR",
  "operator_type": "AGGREGATE",
  "config": {
    "group_by_fields": ["city"],
    "aggregations": [
      {
        "field": "user_id",
        "function": "COUNT",
        "alias": "user_count"
      },
      {
        "field": "amount",
        "function": "SUM",
        "alias": "total_amount"
      },
      {
        "field": "amount",
        "function": "AVG",
        "alias": "avg_amount"
      }
    ],
    "window": {
      "type": "TUMBLING",
      "size": "5m"
    }
  }
}
```

#### WINDOW算子（有状态）
```json
{
  "node_id": "operator-window-001",
  "node_name": "5分钟滚动窗口",
  "node_type": "OPERATOR",
  "operator_type": "WINDOW",
  "config": {
    "window_type": "TUMBLING",
    "window_size": "5m",
    "allowed_lateness": "1m",
    "trigger": "ON_TIME"
  }
}
```

#### JOIN算子（有状态）
```json
{
  "node_id": "operator-join-001",
  "node_name": "关联用户信息",
  "node_type": "OPERATOR",
  "operator_type": "JOIN",
  "config": {
    "join_type": "LEFT",
    "left_key": "user_id",
    "right_key": "id",
    "right_source": {
      "type": "CACHE",
      "cache_name": "user_info_cache"
    },
    "output_fields": ["*", "user.name", "user.age", "user.city"]
  }
}
```

#### DEDUPLICATE算子（有状态）
```json
{
  "node_id": "operator-dedup-001",
  "node_name": "去重",
  "node_type": "OPERATOR",
  "operator_type": "DEDUPLICATE",
  "config": {
    "key_fields": ["user_id", "event_id"],
    "time_window": "1h",
    "keep_first": true
  }
}
```

### 2.3 SINK节点

Sink节点定义数据输出。

#### JDBC_SINK
```json
{
  "node_id": "sink-mysql-001",
  "node_name": "写入MySQL",
  "node_type": "SINK",
  "operator_type": "JDBC_SINK",
  "config": {
    "datasource_id": "mysql-warehouse",
    "table": "user_statistics",
    "write_mode": "UPSERT",
    "unique_key": ["date", "city"],
    "batch_size": 100,
    "flush_interval_ms": 5000,
    "max_retries": 3,
    "field_mapping": {
      "stat_date": "date",
      "city_name": "city",
      "user_cnt": "user_count",
      "total_amt": "total_amount"
    }
  }
}
```

#### KAFKA_SINK
```json
{
  "node_id": "sink-kafka-001",
  "node_name": "写入Kafka",
  "node_type": "SINK",
  "operator_type": "KAFKA_SINK",
  "config": {
    "datasource_id": "kafka-prod-cluster",
    "topic": "processed-events",
    "key_field": "user_id",
    "partition_strategy": "HASH",
    "serialization": "JSON",
    "compression": "gzip",
    "acks": "all",
    "batch_size": 100,
    "linger_ms": 10
  }
}
```

#### ELASTICSEARCH_SINK
```json
{
  "node_id": "sink-es-001",
  "node_name": "写入ES",
  "node_type": "SINK",
  "operator_type": "ELASTICSEARCH_SINK",
  "config": {
    "datasource_id": "elasticsearch-cluster",
    "index": "user_events_{date}",
    "id_field": "event_id",
    "batch_size": 500,
    "flush_interval_ms": 3000,
    "max_retries": 3
  }
}
```

#### FILE_SINK
```json
{
  "node_id": "sink-file-001",
  "node_name": "写入文件",
  "node_type": "SINK",
  "operator_type": "FILE_SINK",
  "config": {
    "path": "/data/output/result_{date}.json",
    "format": "JSON",
    "charset": "UTF-8",
    "rolling_policy": {
      "type": "TIME",
      "interval": "1h"
    },
    "compression": "gzip"
  }
}
```

## 3. 边（Edge）定义

边描述节点之间的数据流向关系。

```json
{
  "edge_id": "edge-001",
  "source_node_id": "source-kafka-001",
  "target_node_id": "operator-map-001",
  "edge_type": "FORWARD"
}
```

**边类型**:
- `FORWARD`: 一对一转发（默认）
- `BROADCAST`: 广播到所有下游
- `SHUFFLE`: 按key重新分区（暂时不用，单机执行）

## 4. 完整示例

### 4.1 简单ETL任务

**场景**: 从Kafka读取数据 → 解析JSON → 过滤 → 写入MySQL

```json
{
  "version": "1.0",
  "nodes": [
    {
      "node_id": "source-001",
      "node_name": "Kafka数据源",
      "node_type": "SOURCE",
      "operator_type": "KAFKA_SOURCE",
      "config": {
        "datasource_id": "kafka-prod",
        "topics": ["user-events"],
        "group_id": "etl-simple",
        "auto_offset_reset": "latest"
      }
    },
    {
      "node_id": "op-parse-001",
      "node_name": "解析JSON",
      "node_type": "OPERATOR",
      "operator_type": "MAP",
      "config": {
        "function_class": "com.example.ParseJsonFunction"
      }
    },
    {
      "node_id": "op-filter-001",
      "node_name": "过滤有效数据",
      "node_type": "OPERATOR",
      "operator_type": "FILTER",
      "config": {
        "predicate_expression": "data.user_id != null AND data.event_type != null"
      }
    },
    {
      "node_id": "sink-001",
      "node_name": "写入MySQL",
      "node_type": "SINK",
      "operator_type": "JDBC_SINK",
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
      "target_node_id": "op-parse-001"
    },
    {
      "edge_id": "edge-002",
      "source_node_id": "op-parse-001",
      "target_node_id": "op-filter-001"
    },
    {
      "edge_id": "edge-003",
      "source_node_id": "op-filter-001",
      "target_node_id": "sink-001"
    }
  ],
  "global_config": {
    "buffer_size": 1000,
    "backpressure_strategy": "BUFFER"
  }
}
```

### 4.2 带聚合的实时统计任务

**场景**: Kafka → 解析 → 窗口聚合 → 写入MySQL和Redis

```json
{
  "version": "1.0",
  "nodes": [
    {
      "node_id": "source-001",
      "node_name": "订单事件源",
      "node_type": "SOURCE",
      "operator_type": "KAFKA_SOURCE",
      "config": {
        "datasource_id": "kafka-prod",
        "topics": ["order-events"],
        "group_id": "order-stats-etl"
      }
    },
    {
      "node_id": "op-parse-001",
      "node_name": "解析订单JSON",
      "node_type": "OPERATOR",
      "operator_type": "MAP",
      "config": {
        "function_class": "com.example.ParseOrderFunction"
      }
    },
    {
      "node_id": "op-window-001",
      "node_name": "5分钟窗口",
      "node_type": "OPERATOR",
      "operator_type": "WINDOW",
      "config": {
        "window_type": "TUMBLING",
        "window_size": "5m"
      }
    },
    {
      "node_id": "op-agg-001",
      "node_name": "按城市聚合",
      "node_type": "OPERATOR",
      "operator_type": "AGGREGATE",
      "config": {
        "group_by_fields": ["city"],
        "aggregations": [
          {
            "field": "order_id",
            "function": "COUNT",
            "alias": "order_count"
          },
          {
            "field": "amount",
            "function": "SUM",
            "alias": "total_amount"
          }
        ]
      }
    },
    {
      "node_id": "sink-mysql-001",
      "node_name": "写入MySQL",
      "node_type": "SINK",
      "operator_type": "JDBC_SINK",
      "config": {
        "datasource_id": "mysql-warehouse",
        "table": "order_stats_5m",
        "write_mode": "INSERT",
        "batch_size": 50
      }
    },
    {
      "node_id": "sink-redis-001",
      "node_name": "写入Redis",
      "node_type": "SINK",
      "operator_type": "REDIS_SINK",
      "config": {
        "datasource_id": "redis-cache",
        "key_pattern": "order:stats:5m:{city}",
        "expire_seconds": 3600
      }
    }
  ],
  "edges": [
    {
      "edge_id": "edge-001",
      "source_node_id": "source-001",
      "target_node_id": "op-parse-001"
    },
    {
      "edge_id": "edge-002",
      "source_node_id": "op-parse-001",
      "target_node_id": "op-window-001"
    },
    {
      "edge_id": "edge-003",
      "source_node_id": "op-window-001",
      "target_node_id": "op-agg-001"
    },
    {
      "edge_id": "edge-004",
      "source_node_id": "op-agg-001",
      "target_node_id": "sink-mysql-001"
    },
    {
      "edge_id": "edge-005",
      "source_node_id": "op-agg-001",
      "target_node_id": "sink-redis-001"
    }
  ],
  "global_config": {
    "checkpoint_enabled": true,
    "checkpoint_interval_seconds": 60
  }
}
```

### 4.3 复杂的多分支处理任务

**场景**: 一个Source → 多个处理分支 → 多个Sink

```json
{
  "version": "1.0",
  "nodes": [
    {
      "node_id": "source-001",
      "node_name": "用户行为日志",
      "node_type": "SOURCE",
      "operator_type": "KAFKA_SOURCE",
      "config": {
        "datasource_id": "kafka-prod",
        "topics": ["user-behavior"],
        "group_id": "behavior-etl"
      }
    },
    {
      "node_id": "op-parse-001",
      "node_name": "解析日志",
      "node_type": "OPERATOR",
      "operator_type": "MAP",
      "config": {
        "function_class": "com.example.ParseBehaviorFunction"
      }
    },
    {
      "node_id": "op-filter-login-001",
      "node_name": "过滤登录事件",
      "node_type": "OPERATOR",
      "operator_type": "FILTER",
      "config": {
        "predicate_expression": "event_type == 'LOGIN'"
      }
    },
    {
      "node_id": "op-filter-purchase-001",
      "node_name": "过滤购买事件",
      "node_type": "OPERATOR",
      "operator_type": "FILTER",
      "config": {
        "predicate_expression": "event_type == 'PURCHASE'"
      }
    },
    {
      "node_id": "op-filter-view-001",
      "node_name": "过滤浏览事件",
      "node_type": "OPERATOR",
      "operator_type": "FILTER",
      "config": {
        "predicate_expression": "event_type == 'VIEW'"
      }
    },
    {
      "node_id": "op-enrich-001",
      "node_name": "关联用户信息",
      "node_type": "OPERATOR",
      "operator_type": "JOIN",
      "config": {
        "join_type": "LEFT",
        "left_key": "user_id",
        "right_key": "id",
        "right_source": {
          "type": "JDBC",
          "datasource_id": "mysql-user",
          "query": "SELECT id, name, city, vip_level FROM users WHERE id IN (?)"
        }
      }
    },
    {
      "node_id": "sink-login-001",
      "node_name": "登录日志入库",
      "node_type": "SINK",
      "operator_type": "JDBC_SINK",
      "config": {
        "datasource_id": "mysql-log",
        "table": "login_logs",
        "batch_size": 100
      }
    },
    {
      "node_id": "sink-purchase-001",
      "node_name": "购买记录入库",
      "node_type": "SINK",
      "operator_type": "JDBC_SINK",
      "config": {
        "datasource_id": "mysql-order",
        "table": "purchase_records",
        "batch_size": 50
      }
    },
    {
      "node_id": "sink-view-001",
      "node_name": "浏览行为入ES",
      "node_type": "SINK",
      "operator_type": "ELASTICSEARCH_SINK",
      "config": {
        "datasource_id": "es-behavior",
        "index": "view_logs_{date}",
        "batch_size": 500
      }
    },
    {
      "node_id": "sink-all-001",
      "node_name": "全量数据归档",
      "node_type": "SINK",
      "operator_type": "FILE_SINK",
      "config": {
        "path": "/data/archive/behavior_{date}.json",
        "format": "JSON",
        "rolling_policy": {
          "type": "SIZE",
          "max_size_mb": 100
        }
      }
    }
  ],
  "edges": [
    {
      "edge_id": "edge-001",
      "source_node_id": "source-001",
      "target_node_id": "op-parse-001"
    },
    {
      "edge_id": "edge-002",
      "source_node_id": "op-parse-001",
      "target_node_id": "op-filter-login-001"
    },
    {
      "edge_id": "edge-003",
      "source_node_id": "op-parse-001",
      "target_node_id": "op-filter-purchase-001"
    },
    {
      "edge_id": "edge-004",
      "source_node_id": "op-parse-001",
      "target_node_id": "op-filter-view-001"
    },
    {
      "edge_id": "edge-005",
      "source_node_id": "op-filter-login-001",
      "target_node_id": "sink-login-001"
    },
    {
      "edge_id": "edge-006",
      "source_node_id": "op-filter-purchase-001",
      "target_node_id": "op-enrich-001"
    },
    {
      "edge_id": "edge-007",
      "source_node_id": "op-enrich-001",
      "target_node_id": "sink-purchase-001"
    },
    {
      "edge_id": "edge-008",
      "source_node_id": "op-filter-view-001",
      "target_node_id": "sink-view-001"
    },
    {
      "edge_id": "edge-009",
      "source_node_id": "op-parse-001",
      "target_node_id": "sink-all-001"
    }
  ],
  "global_config": {
    "buffer_size": 2000,
    "backpressure_strategy": "DROP_OLDEST",
    "checkpoint_enabled": true,
    "checkpoint_interval_seconds": 300
  }
}
```

### 4.4 批处理任务示例

**场景**: 从MySQL增量读取 → 转换 → 写入数据仓库

```json
{
  "version": "1.0",
  "nodes": [
    {
      "node_id": "source-001",
      "node_name": "MySQL增量源",
      "node_type": "SOURCE",
      "operator_type": "JDBC_SOURCE",
      "config": {
        "datasource_id": "mysql-app",
        "query": "SELECT * FROM orders WHERE updated_at > ? AND updated_at <= ? ORDER BY updated_at",
        "fetch_size": 5000,
        "timestamp_column": "updated_at",
        "increment_type": "TIME_BASED"
      }
    },
    {
      "node_id": "op-transform-001",
      "node_name": "数据转换",
      "node_type": "OPERATOR",
      "operator_type": "MAP",
      "config": {
        "function_class": "com.example.OrderTransformFunction",
        "function_config": {
          "date_format": "yyyy-MM-dd HH:mm:ss",
          "timezone": "Asia/Shanghai"
        }
      }
    },
    {
      "node_id": "op-dedup-001",
      "node_name": "去重",
      "node_type": "OPERATOR",
      "operator_type": "DEDUPLICATE",
      "config": {
        "key_fields": ["order_id"],
        "keep_first": false
      }
    },
    {
      "node_id": "sink-001",
      "node_name": "写入数仓",
      "node_type": "SINK",
      "operator_type": "JDBC_SINK",
      "config": {
        "datasource_id": "mysql-dw",
        "table": "dw_orders",
        "write_mode": "UPSERT",
        "unique_key": ["order_id"],
        "batch_size": 1000,
        "use_transaction": true
      }
    }
  ],
  "edges": [
    {
      "edge_id": "edge-001",
      "source_node_id": "source-001",
      "target_node_id": "op-transform-001"
    },
    {
      "edge_id": "edge-002",
      "source_node_id": "op-transform-001",
      "target_node_id": "op-dedup-001"
    },
    {
      "edge_id": "edge-003",
      "source_node_id": "op-dedup-001",
      "target_node_id": "sink-001"
    }
  ],
  "global_config": {
    "job_type": "BATCH",
    "checkpoint_enabled": false
  }
}
```

## 5. 全局配置说明

```json
{
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

**配置项说明**:
- `buffer_size`: 数据缓冲区大小
- `backpressure_strategy`: 背压策略（BUFFER/DROP/ERROR）
- `checkpoint_enabled`: 是否启用检查点
- `checkpoint_interval_seconds`: 检查点间隔
- `error_handling`: 错误处理策略

## 6. 图定义的可视化表示

### 简单线性流程
```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Source  │───▶│   Map    │───▶│  Filter  │───▶│   Sink   │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

### 多分支流程
```
                ┌──────────┐    ┌──────────┐
            ┌──▶│ Filter 1 │───▶│  Sink 1  │
            │   └──────────┘    └──────────┘
┌──────────┐│   ┌──────────┐    ┌──────────┐
│  Source  ├┼──▶│ Filter 2 │───▶│  Sink 2  │
└──────────┘│   └──────────┘    └──────────┘
            │   ┌──────────┐    ┌──────────┐
            └──▶│ Filter 3 │───▶│  Sink 3  │
                └──────────┘    └──────────┘
```

### 聚合流程
```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Source  │───▶│  Window  │───▶│ Aggregate│───▶│   Sink   │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
                                      │
                                      └──────────────┐
                                                     ▼
                                               [State Store]
```

## 7. 建议和最佳实践

### 7.1 节点命名规范
- 使用有意义的名称
- 按类型添加前缀：source-、op-、sink-
- 使用连字符分隔单词

### 7.2 配置管理
- 敏感信息使用占位符：`${variable_name}`
- 在运行时从配置中心或环境变量读取
- 避免硬编码

### 7.3 错误处理
- Source错误：重试
- Operator错误：跳过或记录到死信队列
- Sink错误：重试或失败

### 7.4 性能优化
- 合理设置batch_size
- 调整buffer_size避免内存溢出
- 使用合适的window大小

---

**文档版本**: v1.0  
**最后更新**: 2025-11-09
