# Pipeline Framework 实现指南

## 一、Graph 串联 Source-Operator-Sink 实现原理

### 核心实现：GraphExecutor

`GraphExecutor` 是将 `StreamGraph` 转换为可执行响应式流的核心组件。

#### 执行流程

```
StreamGraph (DAG)
    ↓
拓扑排序获取执行顺序
    ↓
递归构建每个节点的Flux
    ↓
Source.read() → Operator.apply() → Sink.write()
    ↓
组合为完整的响应式Pipeline
```

### 使用示例

```java
// 1. 准备组件
Map<String, DataSource<?>> sources = new HashMap<>();
sources.put("source-1", kafkaSource);

Map<String, Operator<?, ?>> operators = new HashMap<>();
operators.put("operator-1", mapOperator);
operators.put("operator-2", filterOperator);

Map<String, DataSink<?>> sinks = new HashMap<>();
sinks.put("sink-1", mysqlSink);

// 2. 创建GraphExecutor
GraphExecutor executor = new GraphExecutor(
    streamGraph,
    sources,
    operators,
    sinks
);

// 3. 执行
executor.execute()
    .subscribe(
        () -> log.info("Graph execution completed"),
        error -> log.error("Graph execution failed", error)
    );
```

### 内部工作原理

```java
/**
 * GraphExecutor如何构建Flux链
 */
private Flux<?> buildFluxForNode(StreamNode node) {
    switch (node.getNodeType()) {
        case SOURCE:
            // 直接从Source读取
            return source.read();
            
        case OPERATOR:
            // 1. 获取上游节点
            List<StreamNode> upstreamNodes = getUpstreamNodes(node);
            
            // 2. 构建上游Flux
            Flux<Object> upstreamFlux = mergeUpstreamFluxes(upstreamNodes);
            
            // 3. 应用当前Operator
            Operator<Object, Object> operator = operators.get(node.getNodeId());
            return operator.apply(upstreamFlux);
            
        case SINK:
            // Sink节点返回上游Flux
            return buildOperatorFlux(node);
    }
}
```

### 关键特性

1. **自动处理DAG拓扑**：根据节点依赖关系自动构建执行顺序
2. **支持多上游合并**：使用 `Flux.merge()` 合并多个上游数据流
3. **懒加载执行**：只有订阅时才真正执行
4. **缓存优化**：相同节点的Flux只构建一次

## 二、Pipeline 构建器实现

### 简化的Pipeline API

使用 `PipelineBuilder` 提供流式API：

```java
// 构建Pipeline
Pipeline<String, Integer> pipeline = PipelineBuilder.<String>create()
    .name("my-pipeline")
    .source(kafkaSource)                    // 设置Source
    .addOperator(parseOperator)             // 添加算子1
    .addOperator(filterOperator)            // 添加算子2
    .addOperator(aggregateOperator)         // 添加算子3
    .sink(mysqlSink)                        // 设置Sink
    .build();                               // 构建

// 执行Pipeline
pipeline.execute()
    .doOnSuccess(result -> {
        log.info("Pipeline completed in {} ms", 
            result.getDuration().toMillis());
        log.info("Processed {} records", 
            result.getRecordsProcessed());
    })
    .subscribe();
```

### DefaultPipeline 实现原理

```java
public class DefaultPipeline<IN, OUT> implements Pipeline<IN, OUT> {
    
    @Override
    public Mono<PipelineResult> execute() {
        return Mono.defer(() -> {
            // 1. 启动Source和Sink
            return source.start()
                .then(sink.start())
                // 2. 构建数据流
                .then(executePipeline())
                // 3. 返回执行结果
                .then(Mono.just(createResult()));
        });
    }
    
    private Mono<Void> executePipeline() {
        // Source读取
        Flux<IN> sourceFlux = source.read();
        
        // 算子链处理
        Flux<OUT> processedFlux = operatorChain.execute(sourceFlux);
        
        // Sink写入
        return sink.write(processedFlux);
    }
}
```

### 算子链实现

```java
public class DefaultOperatorChain<IN, OUT> implements OperatorChain<IN, OUT> {
    
    @Override
    public Flux<OUT> execute(Flux<IN> input) {
        Flux<?> current = input;
        
        // 依次应用每个算子
        for (Operator<?, ?> operator : operators) {
            current = operator.apply(current);
        }
        
        return (Flux<OUT>) current;
    }
}
```

## 三、何时使用 Reactor？

### 必须使用 Reactor 的场景 ✅

#### 1. 数据流处理（核心）
```java
// Source → Operator → Sink 全程响应式
Flux<Data> stream = source.read();
Flux<Result> processed = operator.apply(stream);
Mono<Void> written = sink.write(processed);
```

#### 2. 外部I/O操作
```java
// 数据库
Mono<User> user = r2dbcRepository.findById(id);

// HTTP请求
Mono<Response> response = webClient.get().retrieve().bodyToMono(Response.class);

// Kafka
Flux<Record> records = kafkaReceiver.receive();
```

#### 3. 异步任务调度
```java
// JobScheduler
public Mono<ScheduleResult> schedule(Job job, ScheduleConfig config) {
    return validateConfig(config)  // 异步验证
        .flatMap(valid -> persistSchedule(job, config))  // 异步持久化
        .map(this::toResult);
}
```

### 可选使用 Reactor 的场景 ⚠️

#### 1. 配置和元数据查询

**频繁调用**：建议用 Reactor
```java
public Mono<JobConfig> getJobConfig(String jobId) {
    return configRepository.findById(jobId);
}
```

**低频调用**（如启动时）：可以用同步
```java
@PostConstruct
public void init() {
    List<JobConfig> configs = configRepository.findAll();
    // 处理配置...
}
```

#### 2. 缓存操作

**本地缓存**：同步即可
```java
private final Map<String, Object> localCache = new ConcurrentHashMap<>();

public Object get(String key) {
    return localCache.get(key);
}
```

**分布式缓存**：建议响应式
```java
public Mono<Object> get(String key) {
    return reactiveRedisTemplate.opsForValue().get(key);
}
```

### 不应使用 Reactor 的场景 ❌

#### 1. 纯计算（无I/O）
```java
// ❌ 过度使用
Mono<Integer> result = Mono.fromCallable(() -> a + b);

// ✅ 直接计算
int result = a + b;
```

#### 2. 简单的内存操作
```java
// ❌ 没必要
Mono<String> value = Mono.just(map.get(key));

// ✅ 直接操作
String value = map.get(key);
```

#### 3. 日志记录
```java
// ✅ 同步日志
log.info("Processing data: {}", data);

// ❌ 过度包装
Mono.fromRunnable(() -> log.info(...)).subscribe();
```

## 四、MyBatis Plus 使用策略

### 为什么同时使用 R2DBC 和 MyBatis Plus？

```
R2DBC (响应式)              MyBatis Plus (同步)
    ↓                           ↓
数据流处理中的查询          配置和元数据管理
实时指标写入                 任务配置CRUD
状态持久化                   管理后台API
高并发场景                   低频调用场景
```

### MyBatis Plus 使用示例

#### 1. 实体类定义
```java
@Data
@TableName("pipeline_job")
public class JobEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("job_id")
    private String jobId;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableLogic  // 逻辑删除
    private Boolean isDeleted;
}
```

#### 2. Mapper接口
```java
@Mapper
public interface JobMapper extends BaseMapper<JobEntity> {
    
    // 自动继承标准CRUD方法
    // - insert
    // - deleteById
    // - updateById
    // - selectById
    // - selectList
    
    // 自定义查询
    @Select("SELECT * FROM pipeline_job WHERE job_id = #{jobId}")
    JobEntity selectByJobId(String jobId);
}
```

#### 3. Service层（提供响应式包装）
```java
@Service
public class JobService {
    
    private final JobMapper jobMapper;
    
    /**
     * 响应式API - 将阻塞调用包装为Mono。
     */
    public Mono<JobEntity> getByJobId(String jobId) {
        return Mono.fromCallable(() -> jobMapper.selectByJobId(jobId))
            .subscribeOn(Schedulers.boundedElastic());  // 关键：隔离到专用线程池
    }
    
    /**
     * 响应式API - 查询列表。
     */
    public Flux<JobEntity> getRunningJobs() {
        return Mono.fromCallable(jobMapper::selectRunningJobs)
            .flatMapMany(Flux::fromIterable)
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 同步API - 用于低频场景。
     */
    public List<JobEntity> listByPage(int pageNum, int pageSize) {
        LambdaQueryWrapper<JobEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobEntity::getIsDeleted, false)
               .orderByDesc(JobEntity::getCreateTime);
        return jobMapper.selectList(wrapper);
    }
}
```

### 关键注意事项

1. **线程池隔离**：必须使用 `subscribeOn(Schedulers.boundedElastic())`
2. **不要在流处理中频繁调用**：MyBatis的阻塞操作会影响性能
3. **适合场景**：配置查询、管理API、低频操作

## 五、完整示例：构建一个ETL Pipeline

### 场景：从Kafka读取，转换后写入MySQL

```java
@Service
public class EtlPipelineExample {
    
    @Autowired
    private KafkaSource<String> kafkaSource;
    
    @Autowired
    private OperatorFactory operatorFactory;
    
    @Autowired
    private MysqlSink<UserData> mysqlSink;
    
    public Mono<PipelineResult> runEtlJob() {
        // 1. 创建算子
        Operator<String, JsonNode> parseOperator = 
            operatorFactory.createOperator(OperatorType.MAP, parseConfig).block();
            
        Operator<JsonNode, UserData> transformOperator =
            operatorFactory.createOperator(OperatorType.MAP, transformConfig).block();
            
        Operator<UserData, UserData> filterOperator =
            operatorFactory.createOperator(OperatorType.FILTER, filterConfig).block();
        
        // 2. 构建Pipeline
        Pipeline<String, UserData> pipeline = PipelineBuilder.<String>create()
            .name("kafka-to-mysql-pipeline")
            .source(kafkaSource)
            .addOperator(parseOperator)      // JSON解析
            .addOperator(transformOperator)  // 数据转换
            .addOperator(filterOperator)     // 数据过滤
            .sink(mysqlSink)
            .build();
        
        // 3. 执行Pipeline
        return pipeline.execute()
            .doOnSuccess(result -> {
                log.info("ETL completed:");
                log.info("- Duration: {} ms", result.getDuration().toMillis());
                log.info("- Records processed: {}", result.getRecordsProcessed());
            })
            .doOnError(error -> log.error("ETL failed", error));
    }
}
```

### 使用GraphExecutor的完整示例

```java
@Service
public class GraphExecutionExample {
    
    public Mono<Void> executeComplexPipeline() {
        // 1. 构建StreamGraph（通常从数据库加载）
        StreamGraph graph = loadGraphFromDatabase();
        
        // 2. 准备组件实例
        Map<String, DataSource<?>> sources = prepareSources(graph);
        Map<String, Operator<?, ?>> operators = prepareOperators(graph);
        Map<String, DataSink<?>> sinks = prepareSinks(graph);
        
        // 3. 创建并执行GraphExecutor
        GraphExecutor executor = new GraphExecutor(graph, sources, operators, sinks);
        
        return executor.execute()
            .doOnSuccess(() -> log.info("Complex pipeline completed"))
            .doOnError(e -> log.error("Pipeline failed", e))
            .then();
    }
    
    private StreamGraph loadGraphFromDatabase() {
        // 从数据库加载graph_definition JSON
        String graphJson = jobService.getGraphDefinition(jobId);
        return GraphParser.parse(graphJson);
    }
    
    private Map<String, DataSource<?>> prepareSources(StreamGraph graph) {
        Map<String, DataSource<?>> sources = new HashMap<>();
        
        for (StreamNode node : graph.getSourceNodes()) {
            // 根据配置创建Source
            SourceConfig config = parseSourceConfig(node.getConfig());
            Connector connector = connectorRegistry.getConnector(config.getType()).block();
            DataSource<?> source = connector.createSource(config).block();
            sources.put(node.getNodeId(), source);
        }
        
        return sources;
    }
}
```

## 六、性能优化建议

### 1. 使用合适的Scheduler

```java
// CPU密集型
flux.publishOn(Schedulers.parallel())

// I/O操作
mono.subscribeOn(Schedulers.boundedElastic())

// 单线程（顺序处理）
flux.subscribeOn(Schedulers.single())
```

### 2. 批量处理

```java
source.read()
    .buffer(1000)  // 每1000条批处理
    .flatMap(batch -> sink.writeBatch(Flux.fromIterable(batch), 1000))
    .subscribe();
```

### 3. 背压控制

```java
source.read()
    .onBackpressureBuffer(10000)  // 缓冲区
    .limitRate(100)               // 限速
    .subscribe();
```

### 4. 并行处理

```java
source.read()
    .parallel(4)                  // 4个并行流
    .runOn(Schedulers.parallel()) // 使用并行调度器
    .map(this::transform)
    .sequential()                 // 合并回单个流
    .subscribe();
```

## 七、调试和监控

### 启用日志

```java
Flux<Data> flux = source.read()
    .log("source")                    // 记录所有信号
    .map(this::transform)
    .log("transform")
    .subscribe();
```

### 检查点标记

```java
flux.checkpoint("after-source")      // 标记位置，便于定位错误
    .map(this::transform)
    .checkpoint("after-transform")
    .subscribe();
```

### 指标收集

```java
flux.doOnNext(data -> metrics.recordProcessed(1))
    .doOnError(error -> metrics.recordError())
    .subscribe();
```

## 总结

1. **数据流处理**：使用 `GraphExecutor` 或 `PipelineBuilder` 构建响应式Pipeline
2. **响应式原则**：I/O操作必须响应式，纯计算可以同步
3. **MyBatis Plus**：用于配置管理和低频操作，通过 `Schedulers.boundedElastic()` 隔离
4. **性能优化**：合理使用批处理、背压和并行度
5. **监控调试**：使用日志、检查点和指标收集

项目已具备完整的响应式流处理能力，可以开始实际业务开发！
