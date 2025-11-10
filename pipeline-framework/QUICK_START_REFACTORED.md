# Pipeline Framework 快速开始指南（重构版）

## 🚀 5分钟快速上手

本指南将帮助你快速了解和使用重构后的Pipeline Framework。

## 📦 前置条件

- JDK 17+
- Maven 3.9+
- MySQL 8.0+（用于SQL批量任务）

## 🔧 安装

### 1. 克隆项目

```bash
git clone <repository-url>
cd pipeline-framework
```

### 2. 编译安装

```bash
mvn clean install -DskipTests
```

### 3. 配置数据库

编辑 `pipeline-starter/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pipeline_framework
    username: root
    password: your_password
```

### 4. 启动应用

```bash
cd pipeline-starter
mvn spring-boot:run
```

## 💡 核心特性

### ✨ 三种任务类型

```java
// 1. 流式任务 - 持续运行（如Kafka消费）
JobType.STREAMING

// 2. 批处理任务 - 一次性执行（如文件导入）
JobType.BATCH

// 3. SQL批量任务 - 大SQL多表整合（新增）
JobType.SQL_BATCH
```

### ⚙️ 自动配置

无需手动配置Bean，所有组件自动装配！

```yaml
pipeline:
  framework:
    enabled: true  # 默认启用
```

## 📝 使用示例

### 示例1：简单的SQL批量任务

```java
@Service
public class MyService {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private BatchJobExecutor executor;
    
    public void runSqlBatchJob() {
        // 1. 创建Source（从哪里读数据）
        SqlBatchSourceConfig sourceConfig = SqlBatchSourceConfig.builder()
            .componentId("my-source")
            .sql("SELECT * FROM source_table WHERE id > 1000")
            .fetchSize(500)
            .build();
        
        SqlBatchSource source = new SqlBatchSource(sourceConfig, dataSource);
        
        // 2. 创建Sink（写到哪里去）
        SqlBatchSinkConfig sinkConfig = SqlBatchSinkConfig.builder()
            .componentId("my-sink")
            .tableName("target_table")
            .batchSize(1000)
            .build();
        
        SqlBatchSink sink = new SqlBatchSink(sinkConfig, dataSource);
        
        // 3. 执行任务
        executor.execute(createJob(source, sink))
            .subscribe(result -> {
                System.out.println("处理了 " + 
                    result.getMetrics().getRecordsProcessed() + " 条记录");
            });
    }
}
```

### 示例2：多表关联查询

```java
public void joinMultipleTables() {
    SqlBatchSourceConfig sourceConfig = SqlBatchSourceConfig.builder()
        .componentId("join-source")
        .sql("""
            SELECT 
                o.order_id,
                c.customer_name,
                SUM(oi.quantity * oi.price) as total
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            JOIN order_items oi ON o.order_id = oi.order_id
            GROUP BY o.order_id, c.customer_name
        """)
        .fetchSize(1000)
        .build();
    
    // ... 创建sink并执行
}
```

### 示例3：带参数的查询

```java
public void queryWithParameters(LocalDate startDate, LocalDate endDate) {
    SqlBatchSourceConfig sourceConfig = SqlBatchSourceConfig.builder()
        .componentId("param-source")
        .sql("SELECT * FROM orders WHERE order_date BETWEEN ? AND ?")
        .parameters(List.of(startDate, endDate))
        .fetchSize(500)
        .build();
    
    // ... 创建sink并执行
}
```

## ⚙️ 配置说明

### application.yml 完整配置

```yaml
pipeline:
  framework:
    enabled: true
    
    # 执行器配置
    executor:
      core-pool-size: 10        # 核心线程数
      max-pool-size: 50         # 最大线程数
      queue-capacity: 500       # 队列容量
      
    # SQL批量任务配置
    sql-batch:
      enabled: true
      batch-size: 1000          # 批次大小
      fetch-size: 500           # 每次获取行数
      query-timeout-seconds: 300 # 查询超时
      parallel-query: true      # 是否并行
      parallelism: 4            # 并行度
      
    # 检查点配置（容错）
    checkpoint:
      enabled: true
      interval-seconds: 60      # 检查点间隔
      storage-path: ./checkpoints
      
    # 监控指标
    metrics:
      enabled: true
      report-interval-seconds: 30
```

## 🎯 常见场景

### 场景1：数据ETL

```java
// 从MySQL读取 -> 处理 -> 写入MySQL
public void etlJob() {
    // 读取源数据
    SqlBatchSource source = createSource("SELECT * FROM source_table");
    
    // 写入目标表
    SqlBatchSink sink = createSink("target_table");
    
    // 执行
    executor.execute(createJob(source, sink)).subscribe();
}
```

### 场景2：报表生成

```java
// 复杂SQL聚合 -> 生成报表
public void generateReport() {
    SqlBatchSource source = createSource("""
        SELECT 
            DATE(order_date) as date,
            COUNT(*) as order_count,
            SUM(amount) as total_amount
        FROM orders
        GROUP BY DATE(order_date)
    """);
    
    SqlBatchSink sink = createSink("daily_report");
    
    executor.execute(createJob(source, sink)).subscribe();
}
```

### 场景3：数据同步

```java
// 定时同步增量数据
@Scheduled(cron = "0 0 * * * ?")  // 每小时执行
public void syncData() {
    SqlBatchSource source = createSource("""
        SELECT * FROM transactions 
        WHERE updated_at > ?
    """, lastSyncTime);
    
    SqlBatchSink sink = createSink("transactions_backup");
    
    executor.execute(createJob(source, sink)).subscribe();
}
```

## 📊 性能调优

### 小数据量（< 10万条）

```yaml
pipeline.framework.sql-batch:
  batch-size: 500
  fetch-size: 200
  parallel-query: false
```

### 中等数据量（10万 - 100万条）

```yaml
pipeline.framework.sql-batch:
  batch-size: 1000
  fetch-size: 500
  parallel-query: true
  parallelism: 4
```

### 大数据量（> 100万条）

```yaml
pipeline.framework.sql-batch:
  batch-size: 2000
  fetch-size: 1000
  parallel-query: true
  parallelism: 8
  max-memory-mb: 1024
```

## 🔍 监控和日志

### 查看任务状态

```java
executor.getJobResult(jobId)
    .subscribe(result -> {
        System.out.println("状态: " + result.getStatus());
        System.out.println("已处理: " + result.getMetrics().getRecordsProcessed());
        System.out.println("失败: " + result.getMetrics().getRecordsFailed());
    });
```

### 访问监控端点

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# Prometheus指标
curl http://localhost:8080/actuator/prometheus

# 所有端点
curl http://localhost:8080/actuator
```

## ❓ 常见问题

### Q1: 如何处理大结果集？

**A:** 设置合适的fetch size，避免一次性加载所有数据到内存：

```java
sourceConfig.setFetchSize(500);  // 每次只获取500行
```

### Q2: 如何实现事务回滚？

**A:** SqlBatchSink自动支持事务，批次失败会自动回滚：

```java
sinkConfig.setBatchSize(1000);  // 1000条为一个事务
```

### Q3: 如何提高性能？

**A:** 启用并行查询：

```yaml
pipeline.framework.sql-batch:
  parallel-query: true
  parallelism: 4
```

### Q4: 如何处理错误？

**A:** 使用Reactor的错误处理：

```java
executor.execute(job)
    .doOnError(error -> log.error("任务失败", error))
    .retry(3)  // 重试3次
    .subscribe();
```

## 📚 更多资源

- [完整重构指南](REFACTORING_GUIDE.md)
- [SQL批量任务示例](SQL_BATCH_EXAMPLE.md)
- [重构总结](README_REFACTORING.md)
- [API文档](https://docs.pipeline-framework.example.com)

## 🆘 获取帮助

遇到问题？

1. 查看文档：[docs/](docs/)
2. 查看示例：[SQL_BATCH_EXAMPLE.md](SQL_BATCH_EXAMPLE.md)
3. 提交Issue：[GitHub Issues](https://github.com/your-org/pipeline-framework/issues)
4. 发送邮件：pipeline-framework-team@example.com

## 🎉 开始使用

```bash
# 1. 编译
mvn clean install

# 2. 运行示例
cd pipeline-starter
mvn spring-boot:run

# 3. 访问
open http://localhost:8080/actuator/health
```

---

**祝你使用愉快！** 🚀

如果觉得有用，别忘了给项目一个 ⭐️
