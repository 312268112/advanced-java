# 项目结构说明

## 目录树

```
pipeline-framework/
├── pom.xml                                    # 父POM文件
├── README.md                                  # 项目说明
├── CONTRIBUTING.md                            # 贡献指南
├── Dockerfile                                 # Docker镜像构建文件
├── docker-compose.yml                         # Docker Compose配置
├── .gitignore                                 # Git忽略文件
├── .dockerignore                              # Docker忽略文件
│
├── etl-api/                                   # 核心API定义模块
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/api/
│       ├── source/                            # Source相关接口
│       │   ├── DataSource.java
│       │   ├── SourceType.java
│       │   ├── SourceConfig.java
│       │   └── SourceException.java
│       ├── operator/                          # Operator相关接口
│       │   ├── Operator.java
│       │   ├── OperatorType.java
│       │   ├── OperatorConfig.java
│       ├── sink/                              # Sink相关接口
│       │   ├── DataSink.java
│       │   ├── SinkConfig.java
│       │   └── SinkException.java
│       ├── job/                               # Job相关接口
│       │   ├── Job.java
│       │   ├── JobType.java
│       │   ├── JobStatus.java
│       │   ├── JobConfig.java
│       │   └── RestartStrategy.java
│       ├── graph/                             # Graph相关接口
│       │   ├── StreamGraph.java
│       │   ├── StreamNode.java
│       │   ├── StreamEdge.java
│       │   ├── NodeType.java
│       │   └── GraphValidationException.java
│       ├── scheduler/                         # Scheduler相关接口
│       │   ├── JobScheduler.java
│       │   ├── SchedulePolicy.java
│       │   ├── ScheduleType.java
│       │   ├── ScheduleResult.java
│       │   └── ScheduleStatus.java
│       └── executor/                          # Executor相关接口
│           ├── JobExecutor.java
│           ├── JobResult.java
│           ├── ExecutionStatus.java
│           └── ExecutionMetrics.java
│
├── etl-core/                                  # 核心运行时实现
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/core/
│       ├── runtime/                           # 运行时
│       ├── pipeline/                          # Pipeline实现
│       └── config/                            # 配置类
│
├── etl-connectors/                            # 连接器实现
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/connectors/
│       ├── jdbc/                              # JDBC连接器
│       ├── kafka/                             # Kafka连接器
│       ├── http/                              # HTTP连接器
│       ├── file/                              # 文件连接器
│       └── redis/                             # Redis连接器
│
├── etl-operators/                             # 算子实现
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/operators/
│       ├── transform/                         # 转换算子（Map、Filter等）
│       ├── aggregate/                         # 聚合算子
│       └── window/                            # 窗口算子
│
├── etl-scheduler/                             # 任务调度
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/scheduler/
│       ├── impl/                              # 调度器实现
│       └── policy/                            # 调度策略
│
├── etl-executor/                              # 任务执行引擎
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/executor/
│       ├── impl/                              # 执行器实现
│       └── context/                           # 执行上下文
│
├── etl-state/                                 # 状态管理
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/state/
│       ├── impl/                              # 状态实现
│       └── backend/                           # 状态后端
│
├── etl-checkpoint/                            # 检查点机制
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/checkpoint/
│       ├── coordinator/                       # 检查点协调器
│       └── storage/                           # 检查点存储
│
├── etl-metrics/                               # 监控指标
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/metrics/
│       ├── collector/                         # 指标收集器
│       └── reporter/                          # 指标报告器
│
├── etl-web/                                   # Web API
│   ├── pom.xml
│   └── src/main/java/com/etl/framework/web/
│       ├── controller/                        # REST控制器
│       ├── service/                           # 服务层
│       └── repository/                        # 数据访问层
│
├── etl-starter/                               # Spring Boot启动模块
│   ├── pom.xml
│   ├── src/main/java/com/etl/framework/
│   │   └── EtlFrameworkApplication.java       # 主启动类
│   └── src/main/resources/
│       ├── application.yml                    # 主配置文件
│       ├── application-dev.yml                # 开发环境配置
│       ├── application-prod.yml               # 生产环境配置
│       └── logback-spring.xml                 # 日志配置
│
├── monitoring/                                # 监控配置
│   └── prometheus.yml                         # Prometheus配置
│
└── docs/                                      # 设计文档
    ├── pipeline-framework-design.md       # 系统架构设计
    ├── database-design.md                     # 数据库设计
    ├── database-schema.sql                    # 建表SQL
    ├── graph-definition-examples.md           # StreamGraph配置说明
    ├── graph-definition-json-examples.json    # JSON配置示例
    └── json-examples-guide.md                 # 使用指南
```

## 模块说明

### etl-api (核心API定义)
- **职责**: 定义所有核心接口和抽象类
- **依赖**: 仅依赖Reactor Core和基础工具类
- **关键接口**:
  - DataSource: 数据源接口
  - Operator: 算子接口
  - DataSink: 数据输出接口
  - Job: 任务接口
  - StreamGraph: 流图接口
  - JobScheduler: 调度器接口
  - JobExecutor: 执行器接口

### etl-core (核心运行时)
- **职责**: 实现核心运行时逻辑
- **依赖**: etl-api
- **功能**:
  - Pipeline管道实现
  - 数据流执行引擎
  - 配置管理

### etl-connectors (连接器)
- **职责**: 实现各种数据源和输出的连接器
- **依赖**: etl-api, etl-core
- **内置连接器**:
  - JDBC: 关系型数据库
  - Kafka: 消息队列
  - HTTP: REST API
  - File: 文件系统
  - Redis: 缓存

### etl-operators (算子)
- **职责**: 实现各种数据转换算子
- **依赖**: etl-api, etl-core, etl-state
- **内置算子**:
  - Map: 一对一映射
  - Filter: 过滤
  - FlatMap: 一对多映射
  - Aggregate: 聚合
  - Window: 窗口
  - Join: 关联
  - Deduplicate: 去重

### etl-scheduler (任务调度)
- **职责**: 任务调度管理
- **依赖**: etl-api, etl-core
- **功能**:
  - 立即调度
  - Cron定时调度
  - 手动触发

### etl-executor (任务执行)
- **职责**: 执行ETL任务
- **依赖**: etl-api, etl-core, etl-connectors, etl-operators
- **功能**:
  - 将StreamGraph转换为可执行的Reactor流
  - 管理任务生命周期
  - 收集执行指标

### etl-state (状态管理)
- **职责**: 管理有状态算子的状态
- **依赖**: etl-api
- **功能**:
  - 内存状态后端
  - RocksDB状态后端（可选）

### etl-checkpoint (检查点)
- **职责**: 实现检查点容错机制
- **依赖**: etl-api, etl-state
- **功能**:
  - 定期创建检查点
  - 检查点存储和恢复
  - 容错机制

### etl-metrics (监控指标)
- **职责**: 收集和报告运行时指标
- **依赖**: etl-api
- **功能**:
  - 指标收集
  - Prometheus导出
  - 自定义指标

### etl-web (Web API)
- **职责**: 提供REST API和Web管理界面
- **依赖**: etl-scheduler, etl-executor
- **功能**:
  - 任务管理API
  - 监控查询API
  - 健康检查

### etl-starter (启动模块)
- **职责**: Spring Boot应用启动
- **依赖**: 所有其他模块
- **功能**:
  - 主启动类
  - 配置文件
  - 日志配置

## 开发流程

1. **定义接口**: 在etl-api中定义新接口
2. **实现核心逻辑**: 在etl-core中实现
3. **扩展连接器**: 在etl-connectors中添加新连接器
4. **扩展算子**: 在etl-operators中添加新算子
5. **配置启动**: 在etl-starter中配置和测试

## 编译顺序

Maven会按照依赖关系自动确定编译顺序：

1. etl-api
2. etl-core, etl-state
3. etl-connectors, etl-operators, etl-checkpoint, etl-metrics
4. etl-scheduler, etl-executor
5. etl-web
6. etl-starter

## 运行要求

- **JDK**: 17+
- **Maven**: 3.9+
- **数据库**: MySQL 8.0+
- **消息队列**: Apache Kafka (可选)
- **缓存**: Redis (可选)
- **内存**: 建议2GB+

## 下一步

1. 实现核心运行时（etl-core）
2. 实现基础连接器（JDBC、Kafka）
3. 实现基础算子（Map、Filter）
4. 实现调度器和执行器
5. 实现Web API
6. 添加单元测试和集成测试

---

**项目创建时间**: 2025-11-09  
**当前状态**: 项目骨架已搭建完成，待实现具体功能
