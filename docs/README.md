# 响应式ETL框架文档中心

## 📚 文档导航

### 核心设计文档

#### 1. [系统架构设计](reactive-etl-framework-design.md)
完整的系统架构设计文档，包含：
- 系统整体架构
- 核心模块设计（Job、StreamGraph、JobGraph、Scheduler、Executor等）
- 关键流程时序图
- 监控运维方案
- 最佳实践

**推荐阅读顺序**: ⭐️⭐️⭐️⭐️⭐️ 必读

---

#### 2. [数据库设计](database-design.md)
数据库表结构设计文档（单机版），包含：
- 13张核心表的详细设计
- 表关系ER图
- 索引策略
- 分区方案
- 数据保留策略

**推荐阅读顺序**: ⭐️⭐️⭐️⭐️⭐️ 必读

---

#### 3. [数据库建表脚本](database-schema.sql)
可直接执行的SQL脚本，包含：
- 所有表的CREATE TABLE语句
- 索引定义
- 初始化数据（内置连接器、系统配置、告警规则）
- 便捷查询视图

**使用方式**:
```bash
mysql -u root -p etl_framework < database-schema.sql
```

---

### StreamGraph配置文档

#### 4. [StreamGraph定义结构说明](graph-definition-examples.md)
详细的StreamGraph配置说明，包含：
- 完整的JSON结构定义
- 所有节点类型详解（Source、Operator、Sink）
- 配置参数说明
- 可视化流程图
- 最佳实践建议

**推荐阅读顺序**: ⭐️⭐️⭐️⭐️ 开发必读

---

#### 5. [JSON配置示例](graph-definition-json-examples.json)
7个完整的、可直接使用的JSON配置示例：
1. **简单ETL** - Kafka到MySQL
2. **实时统计** - 窗口聚合
3. **数据清洗** - 去重和转换
4. **多分支处理** - 日志分流
5. **API数据采集** - HTTP定期拉取
6. **文件处理** - CSV到JSON
7. **数据关联** - JOIN操作

**使用方式**: 直接复制粘贴到你的任务配置中

---

#### 6. [JSON示例使用指南](json-examples-guide.md)
JSON示例的详细使用说明，包含：
- 每个示例的场景说明
- 数据流程图
- 适用场景
- 配置说明
- 常见问题解答

**推荐阅读顺序**: ⭐️⭐️⭐️⭐️ 快速上手

---

## 🚀 快速开始

### 第一步：了解系统架构
阅读 [系统架构设计](reactive-etl-framework-design.md)，理解系统的整体设计理念。

### 第二步：初始化数据库
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE etl_framework DEFAULT CHARACTER SET utf8mb4;

# 执行建表脚本
mysql -u root -p etl_framework < database-schema.sql
```

### 第三步：查看示例
打开 [JSON配置示例](graph-definition-json-examples.json)，选择一个最接近你需求的示例。

### 第四步：创建任务
参考 [JSON示例使用指南](json-examples-guide.md)，修改配置并创建你的第一个ETL任务。

---

## 📖 按角色阅读

### 架构师
1. [系统架构设计](reactive-etl-framework-design.md) - 了解整体架构
2. [数据库设计](database-design.md) - 了解数据模型

### 开发人员
1. [系统架构设计](reactive-etl-framework-design.md) - 核心模块章节
2. [StreamGraph定义结构说明](graph-definition-examples.md) - 节点类型详解
3. [JSON示例使用指南](json-examples-guide.md) - 快速上手

### 运维人员
1. [系统架构设计](reactive-etl-framework-design.md) - 监控运维章节
2. [数据库设计](database-design.md) - 索引和分区优化
3. [数据库建表脚本](database-schema.sql) - 执行初始化

### 产品经理
1. [系统架构设计](reactive-etl-framework-design.md) - 概述和特性
2. [JSON示例使用指南](json-examples-guide.md) - 场景示例

---

## 🎯 按场景查找

### 场景1: 实时数据采集
- **Kafka数据采集**: 查看示例1和示例2
- **API数据拉取**: 查看示例5
- **文件监控采集**: 查看示例6

### 场景2: 数据转换清洗
- **简单转换**: 查看示例1（MAP + FILTER）
- **去重**: 查看示例3（DEDUPLICATE）
- **数组展开**: 查看示例5（FLATMAP）

### 场景3: 实时统计聚合
- **窗口聚合**: 查看示例2（WINDOW + AGGREGATE）
- **分组统计**: 查看示例2（GROUP BY）

### 场景4: 数据关联
- **JOIN操作**: 查看示例7
- **维度补全**: 查看示例7

### 场景5: 多目标输出
- **分支处理**: 查看示例4（多Filter + 多Sink）
- **双写**: 查看示例2（MySQL + Redis）

---

## 🔧 配置速查

### 常用Source配置

```json
// Kafka Source
{
  "operator_type": "KAFKA_SOURCE",
  "config": {
    "datasource_id": "kafka-prod",
    "topics": ["topic-name"],
    "group_id": "consumer-group"
  }
}

// JDBC Source
{
  "operator_type": "JDBC_SOURCE",
  "config": {
    "datasource_id": "mysql-prod",
    "query": "SELECT * FROM table WHERE ...",
    "fetch_size": 1000
  }
}
```

### 常用Operator配置

```json
// MAP
{
  "operator_type": "MAP",
  "config": {
    "function_class": "com.example.YourFunction"
  }
}

// FILTER
{
  "operator_type": "FILTER",
  "config": {
    "predicate_expression": "field > 100"
  }
}

// AGGREGATE
{
  "operator_type": "AGGREGATE",
  "config": {
    "group_by_fields": ["city"],
    "aggregations": [
      {"field": "amount", "function": "SUM"}
    ]
  }
}
```

### 常用Sink配置

```json
// JDBC Sink
{
  "operator_type": "JDBC_SINK",
  "config": {
    "datasource_id": "mysql-warehouse",
    "table": "target_table",
    "batch_size": 100,
    "write_mode": "INSERT"
  }
}

// Kafka Sink
{
  "operator_type": "KAFKA_SINK",
  "config": {
    "datasource_id": "kafka-prod",
    "topic": "output-topic",
    "batch_size": 100
  }
}
```

---

## 📊 表结构速查

### 核心表（13张）

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| etl_job | 任务定义 | job_id, job_status |
| etl_job_instance | 运行实例 | instance_id, job_id |
| etl_job_schedule | 调度配置 | schedule_type, cron_expression |
| etl_stream_graph | 流图定义 | graph_id, graph_definition |
| etl_connector | 连接器注册 | connector_id, connector_type |
| etl_datasource | 数据源配置 | datasource_id, connection_config |
| etl_checkpoint | 检查点 | checkpoint_id, instance_id |
| etl_job_metrics | 运行指标 | job_id, metric_time |
| etl_system_config | 系统配置 | config_key, config_value |
| etl_alert_rule | 告警规则 | rule_id, rule_type |
| etl_alert_record | 告警记录 | alert_id, alert_time |
| etl_user | 用户 | user_id, username |
| etl_operation_log | 操作日志 | operation_type, resource_type |

---

## ❓ 常见问题

### Q1: 数据源配置在哪里？
在`etl_datasource`表中配置，然后在graph_definition中通过`datasource_id`引用。

### Q2: 如何添加自定义算子？
在nodes配置中指定你的`function_class`，框架会通过反射加载。

### Q3: 支持哪些数据源？
内置支持：JDBC、Kafka、HTTP、File、Redis、Elasticsearch。可通过SPI机制扩展。

### Q4: 如何配置检查点？
在`etl_job`表的`checkpoint_enabled`字段或graph_definition的`global_config`中配置。

### Q5: 如何监控任务运行？
查看`etl_job_instance`和`etl_job_metrics`表，或使用Prometheus等监控系统。

---

## 🔗 相关资源

### 技术栈
- [Project Reactor](https://projectreactor.io/) - 响应式编程框架
- [Apache Kafka](https://kafka.apache.org/) - 消息队列
- [MySQL](https://www.mysql.com/) - 关系型数据库
- [Elasticsearch](https://www.elastic.co/) - 搜索引擎

### 参考项目
- [Apache Flink](https://flink.apache.org/) - 分布式流处理框架
- [Spring Cloud Data Flow](https://spring.io/projects/spring-cloud-dataflow) - 数据流编排

---

## 📝 文档版本

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| v1.0 | 2025-11-09 | 初始版本 |
| v2.0 | 2025-11-09 | 简化为单机版架构 |

---

## 👥 贡献者

ETL Framework Team

---

## 📧 联系方式

如有问题或建议，请联系项目维护者。
