# SQL批量任务使用示例

本文档展示如何使用Pipeline Framework的SQL批量任务功能。

## 场景1：订单数据汇总

将多个表的订单数据进行汇总统计。

### SQL查询

```sql
SELECT 
    o.order_id,
    o.order_date,
    c.customer_id,
    c.customer_name,
    c.customer_email,
    COUNT(oi.item_id) as item_count,
    SUM(oi.quantity) as total_quantity,
    SUM(oi.quantity * oi.unit_price) as total_amount,
    o.status
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
JOIN order_items oi ON o.order_id = oi.order_id
WHERE o.order_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY 
    o.order_id, 
    o.order_date, 
    c.customer_id, 
    c.customer_name, 
    c.customer_email,
    o.status
HAVING total_amount > 100
ORDER BY o.order_date DESC
```

### Java实现

```java
@Service
public class OrderSummaryService {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private BatchJobExecutor batchJobExecutor;
    
    public Mono<JobResult> generateOrderSummary() {
        // 1. 配置SQL批量数据源
        SqlBatchSourceConfig sourceConfig = SqlBatchSourceConfig.builder()
            .componentId("order-summary-source")
            .sql("""
                SELECT 
                    o.order_id,
                    o.order_date,
                    c.customer_id,
                    c.customer_name,
                    c.customer_email,
                    COUNT(oi.item_id) as item_count,
                    SUM(oi.quantity) as total_quantity,
                    SUM(oi.quantity * oi.unit_price) as total_amount,
                    o.status
                FROM orders o
                JOIN customers c ON o.customer_id = c.customer_id
                JOIN order_items oi ON o.order_id = oi.order_id
                WHERE o.order_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
                GROUP BY 
                    o.order_id, 
                    o.order_date, 
                    c.customer_id, 
                    c.customer_name, 
                    c.customer_email,
                    o.status
                HAVING total_amount > 100
                ORDER BY o.order_date DESC
            """)
            .fetchSize(1000)
            .queryTimeoutSeconds(300)
            .build();
        
        SqlBatchSource source = new SqlBatchSource(sourceConfig, dataSource);
        
        // 2. 配置SQL批量数据输出
        SqlBatchSinkConfig sinkConfig = SqlBatchSinkConfig.builder()
            .componentId("order-summary-sink")
            .tableName("order_summary_report")
            .columns(Arrays.asList(
                "order_id", "order_date", "customer_id", "customer_name",
                "customer_email", "item_count", "total_quantity", 
                "total_amount", "status"
            ))
            .batchSize(1000)
            .build();
        
        SqlBatchSink sink = new SqlBatchSink(sinkConfig, dataSource);
        
        // 3. 创建并执行任务
        Job job = createSqlBatchJob("order-summary-job", source, sink);
        
        return batchJobExecutor.execute(job)
            .doOnSuccess(result -> {
                log.info("Order summary completed: {} records processed", 
                    result.getMetrics().getRecordsProcessed());
            })
            .doOnError(error -> {
                log.error("Order summary failed", error);
            });
    }
    
    private Job createSqlBatchJob(String jobId, 
                                  SqlBatchSource source, 
                                  SqlBatchSink sink) {
        return new Job() {
            @Override
            public String getJobId() {
                return jobId;
            }
            
            @Override
            public String getJobName() {
                return "Order Summary Job";
            }
            
            @Override
            public JobType getType() {
                return JobType.SQL_BATCH;
            }
            
            // ... 其他方法实现
        };
    }
}
```

## 场景2：数据清洗和转换

从源表读取数据，进行清洗转换后写入目标表。

```java
@Service
public class DataCleansingService {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private BatchJobExecutor batchJobExecutor;
    
    public Mono<JobResult> cleanCustomerData() {
        // 1. 从源表读取数据
        SqlBatchSourceConfig sourceConfig = SqlBatchSourceConfig.builder()
            .componentId("customer-source")
            .sql("""
                SELECT 
                    customer_id,
                    TRIM(customer_name) as customer_name,
                    LOWER(TRIM(email)) as email,
                    phone,
                    address,
                    city,
                    state,
                    zip_code,
                    created_at
                FROM raw_customers
                WHERE created_at >= ?
            """)
            .parameters(List.of(LocalDate.now().minusDays(7)))
            .fetchSize(500)
            .build();
        
        SqlBatchSource source = new SqlBatchSource(sourceConfig, dataSource);
        
        // 2. 写入清洗后的数据
        SqlBatchSinkConfig sinkConfig = SqlBatchSinkConfig.builder()
            .componentId("customer-sink")
            .tableName("cleaned_customers")
            .batchSize(500)
            .build();
        
        SqlBatchSink sink = new SqlBatchSink(sinkConfig, dataSource);
        
        Job job = createSqlBatchJob("customer-cleansing-job", source, sink);
        
        return batchJobExecutor.execute(job);
    }
}
```

## 场景3：增量数据同步

定期同步增量数据到数仓。

```java
@Service
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
public class DataSyncService {
    
    @Autowired
    private DataSource sourceDataSource;
    
    @Autowired
    private DataSource targetDataSource;
    
    @Autowired
    private BatchJobExecutor batchJobExecutor;
    
    public void syncIncrementalData() {
        // 1. 从业务数据库读取增量数据
        SqlBatchSourceConfig sourceConfig = SqlBatchSourceConfig.builder()
            .componentId("incremental-source")
            .sql("""
                SELECT 
                    t1.*,
                    t2.additional_field,
                    t3.calculated_metric
                FROM transaction_table t1
                LEFT JOIN reference_table t2 ON t1.ref_id = t2.id
                LEFT JOIN metrics_table t3 ON t1.id = t3.transaction_id
                WHERE t1.updated_at > (
                    SELECT COALESCE(MAX(sync_time), '1970-01-01') 
                    FROM sync_checkpoint 
                    WHERE table_name = 'transaction_table'
                )
            """)
            .fetchSize(2000)
            .queryTimeoutSeconds(600)
            .build();
        
        SqlBatchSource source = new SqlBatchSource(sourceConfig, sourceDataSource);
        
        // 2. 写入数仓
        SqlBatchSinkConfig sinkConfig = SqlBatchSinkConfig.builder()
            .componentId("warehouse-sink")
            .tableName("dw_transactions")
            .batchSize(2000)
            .build();
        
        SqlBatchSink sink = new SqlBatchSink(sinkConfig, targetDataSource);
        
        Job job = createSqlBatchJob("incremental-sync-job", source, sink);
        
        batchJobExecutor.execute(job)
            .doOnSuccess(result -> {
                // 更新同步检查点
                updateSyncCheckpoint("transaction_table", Instant.now());
                log.info("Incremental sync completed: {} records", 
                    result.getMetrics().getRecordsProcessed());
            })
            .subscribe();
    }
    
    private void updateSyncCheckpoint(String tableName, Instant syncTime) {
        // 更新同步时间戳
    }
}
```

## 场景4：复杂聚合报表

生成多维度的业务报表。

```java
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private BatchJobExecutor batchJobExecutor;
    
    @PostMapping("/sales-summary")
    public Mono<JobResult> generateSalesSummary(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        
        SqlBatchSourceConfig sourceConfig = SqlBatchSourceConfig.builder()
            .componentId("sales-report-source")
            .sql("""
                SELECT 
                    DATE(o.order_date) as report_date,
                    p.product_category,
                    p.product_name,
                    r.region_name,
                    COUNT(DISTINCT o.order_id) as order_count,
                    COUNT(DISTINCT o.customer_id) as customer_count,
                    SUM(oi.quantity) as total_quantity,
                    SUM(oi.quantity * oi.unit_price) as total_revenue,
                    AVG(oi.unit_price) as avg_unit_price,
                    MAX(oi.unit_price) as max_unit_price,
                    MIN(oi.unit_price) as min_unit_price
                FROM orders o
                JOIN order_items oi ON o.order_id = oi.order_id
                JOIN products p ON oi.product_id = p.product_id
                JOIN customers c ON o.customer_id = c.customer_id
                JOIN regions r ON c.region_id = r.region_id
                WHERE o.order_date BETWEEN ? AND ?
                    AND o.status = 'COMPLETED'
                GROUP BY 
                    DATE(o.order_date),
                    p.product_category,
                    p.product_name,
                    r.region_name
                ORDER BY report_date, total_revenue DESC
            """)
            .parameters(List.of(startDate, endDate))
            .fetchSize(1000)
            .build();
        
        SqlBatchSource source = new SqlBatchSource(sourceConfig, dataSource);
        
        SqlBatchSinkConfig sinkConfig = SqlBatchSinkConfig.builder()
            .componentId("sales-report-sink")
            .tableName("sales_summary_report")
            .batchSize(1000)
            .build();
        
        SqlBatchSink sink = new SqlBatchSink(sinkConfig, dataSource);
        
        Job job = createSqlBatchJob("sales-summary-job", source, sink);
        
        return batchJobExecutor.execute(job);
    }
}
```

## 配置优化建议

### 1. 大数据量场景

```yaml
pipeline:
  framework:
    sql-batch:
      batch-size: 2000      # 增大批次
      fetch-size: 1000      # 增大fetch size
      parallel-query: true  # 启用并行
      parallelism: 8        # 增加并行度
      max-memory-mb: 1024   # 增加内存限制
```

### 2. 小数据量场景

```yaml
pipeline:
  framework:
    sql-batch:
      batch-size: 500
      fetch-size: 200
      parallel-query: false
      max-memory-mb: 256
```

### 3. 复杂SQL查询

```yaml
pipeline:
  framework:
    sql-batch:
      query-timeout-seconds: 600  # 增加超时时间
      fetch-size: 500             # 适中的fetch size
```

## 监控和日志

### 查看任务执行状态

```java
batchJobExecutor.getJobResult(jobId)
    .subscribe(result -> {
        log.info("Job Status: {}", result.getStatus());
        log.info("Records Processed: {}", result.getMetrics().getRecordsProcessed());
        log.info("Records Failed: {}", result.getMetrics().getRecordsFailed());
    });
```

### 监控指标

Pipeline Framework会自动收集以下指标：

- `pipeline.framework.job.execution.count` - 任务执行次数
- `pipeline.framework.job.execution.duration` - 任务执行时间
- `pipeline.framework.job.records.processed` - 处理记录数
- `pipeline.framework.job.records.failed` - 失败记录数

## 常见问题

### Q1: 如何处理大结果集的内存问题？

A: 使用流式处理和合适的fetch size：

```java
sourceConfig.setFetchSize(500);  // 每次只取500条
sinkConfig.setBatchSize(500);    // 批量写入500条
```

### Q2: 如何实现断点续传？

A: 使用检查点机制：

```yaml
pipeline:
  framework:
    checkpoint:
      enabled: true
      interval-seconds: 60
```

### Q3: 如何提高并行处理性能？

A: 启用并行查询并合理设置并行度：

```yaml
pipeline:
  framework:
    sql-batch:
      parallel-query: true
      parallelism: 4  # 设置为CPU核心数的1-2倍
```

## 总结

SQL批量任务非常适合以下场景：

- ✅ 多表关联查询
- ✅ 复杂聚合统计
- ✅ 大批量数据ETL
- ✅ 定期数据同步
- ✅ 报表生成

不适合的场景：

- ❌ 实时数据处理（使用STREAMING类型）
- ❌ 小数据量的简单查询
- ❌ 需要复杂业务逻辑的场景

---

更多示例和文档请参考 [REFACTORING_GUIDE.md](REFACTORING_GUIDE.md)
