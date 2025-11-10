# Reactor 使用决策指南

## 核心问题：除了流本身，其他地方是否需要用Reactor？

### 快速决策表

| 场景 | 是否用Reactor | 理由 |
|------|--------------|------|
| **数据流处理** | ✅ 必须 | 核心功能，需要背压和非阻塞 |
| **Job调度执行** | ✅ 建议 | 异步任务，避免阻塞主线程 |
| **状态管理** | ✅ 建议 | 可能涉及I/O持久化 |
| **检查点** | ✅ 建议 | 涉及文件/数据库I/O |
| **指标收集** | ✅ 建议 | 异步发送，不阻塞业务 |
| **配置查询（高频）** | ✅ 建议 | 在流处理中调用 |
| **配置查询（低频）** | ⚠️ 可选 | 启动时加载，同步可接受 |
| **元数据CRUD** | ⚠️ 可选 | 管理后台，同步更简单 |
| **缓存操作（分布式）** | ✅ 建议 | 网络I/O |
| **缓存操作（本地）** | ❌ 不需要 | 内存操作 |
| **日志记录** | ❌ 不需要 | 同步即可 |
| **纯计算** | ❌ 不需要 | 无I/O |

## 详细分析

### 1. Job 调度和执行 - ✅ 建议使用 Reactor

#### 为什么要用？
- Job调度是异步操作
- 执行Job不应阻塞调度线程
- 便于组合多个异步操作

#### 示例实现

```java
@Service
public class ReactiveJobScheduler implements JobScheduler {
    
    private final JobRepository jobRepository;
    private final JobExecutor jobExecutor;
    
    @Override
    public Mono<ScheduleResult> schedule(Job job, ScheduleConfig config) {
        return Mono.defer(() -> {
            // 1. 验证配置（可能涉及数据库查询）
            return validateConfig(config)
                // 2. 创建调度计划（数据库操作）
                .flatMap(valid -> createSchedule(job, config))
                // 3. 注册到调度器
                .flatMap(schedule -> registerSchedule(schedule))
                // 4. 返回结果
                .map(this::toScheduleResult);
        })
        .doOnSuccess(result -> log.info("Job scheduled: {}", job.getJobId()))
        .doOnError(error -> log.error("Schedule failed: {}", job.getJobId(), error));
    }
    
    @Override
    public Mono<Void> trigger(String jobId) {
        return jobRepository.findById(jobId)                    // 异步查询
            .switchIfEmpty(Mono.error(new JobNotFoundException(jobId)))
            .flatMap(job -> jobExecutor.submit(job))            // 异步提交
            .then();
    }
    
    private Mono<Boolean> validateConfig(ScheduleConfig config) {
        // 可能需要查询数据库验证
        return jobRepository.existsByName(config.getJobName())
            .map(exists -> !exists);
    }
    
    private Mono<Schedule> createSchedule(Job job, ScheduleConfig config) {
        Schedule schedule = new Schedule(job, config);
        return scheduleRepository.save(schedule);               // 异步保存
    }
}
```

**关键点**：
- ✅ 所有I/O操作都是异步的
- ✅ 操作可以方便地组合
- ✅ 不阻塞调度线程

### 2. Job 执行器 - ✅ 必须使用 Reactor

#### 为什么必须用？
- 需要并行执行多个Job
- 需要监控Job状态（流式）
- 需要异步启动/停止Job

```java
@Service
public class ReactiveJobExecutor implements JobExecutor {
    
    private final Map<String, Disposable> runningJobs = new ConcurrentHashMap<>();
    
    @Override
    public Mono<JobResult> submit(Job job) {
        return Mono.defer(() -> {
            // 1. 创建Job实例记录
            return createJobInstance(job)
                // 2. 启动Pipeline执行
                .flatMap(instance -> executePipeline(job, instance))
                // 3. 更新实例状态
                .flatMap(result -> updateJobInstance(result))
                // 4. 返回执行结果
                .map(this::toJobResult);
        })
        .doOnSubscribe(s -> log.info("Job submitted: {}", job.getJobId()))
        .doOnSuccess(result -> log.info("Job completed: {}", job.getJobId()));
    }
    
    @Override
    public Flux<ExecutionMetrics> getMetrics(String jobId) {
        // 实时推送指标流
        return Flux.interval(Duration.ofSeconds(1))
            .flatMap(tick -> metricsCollector.collect(jobId))
            .takeUntil(metrics -> isJobCompleted(jobId));
    }
    
    @Override
    public Mono<Void> stop(String jobId) {
        return Mono.defer(() -> {
            Disposable disposable = runningJobs.get(jobId);
            if (disposable != null) {
                disposable.dispose();
                runningJobs.remove(jobId);
            }
            return updateJobStatus(jobId, JobStatus.STOPPED);
        });
    }
    
    private Mono<PipelineResult> executePipeline(Job job, JobInstance instance) {
        // 构建并执行Pipeline
        Pipeline<?, ?> pipeline = buildPipeline(job);
        
        Disposable execution = pipeline.execute()
            .subscribe(
                result -> handleSuccess(instance, result),
                error -> handleError(instance, error)
            );
        
        runningJobs.put(job.getJobId(), execution);
        return Mono.just(new PipelineResult());
    }
}
```

**关键点**：
- ✅ 支持并发执行多个Job
- ✅ 实时指标推送（Flux）
- ✅ 异步启动/停止

### 3. 状态管理 - ✅ 建议使用 Reactor

#### 为什么建议用？
- 状态可能持久化到数据库/Redis
- 在流处理中频繁访问
- 需要原子性操作（CAS）

```java
@Service
public class ReactiveStateManager implements StateManager {
    
    private final R2dbcEntityTemplate r2dbcTemplate;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    
    @Override
    public <T> Mono<State<T>> createState(String name, T initialValue) {
        return Mono.defer(() -> {
            // 创建状态实例
            ReactiveState<T> state = new ReactiveState<>(name, initialValue);
            
            // 持久化到Redis（异步）
            return redisTemplate.opsForValue()
                .set(stateKey(name), initialValue)
                .thenReturn(state);
        });
    }
    
    @Override
    public Mono<Map<String, Object>> snapshot() {
        // 从Redis批量读取所有状态
        return redisTemplate.keys(stateKeyPattern())
            .flatMap(key -> redisTemplate.opsForValue().get(key)
                .map(value -> Map.entry(extractName(key), value)))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }
    
    @Override
    public Mono<Void> restore(Map<String, Object> snapshot) {
        // 批量恢复状态到Redis
        return Flux.fromIterable(snapshot.entrySet())
            .flatMap(entry -> redisTemplate.opsForValue()
                .set(stateKey(entry.getKey()), entry.getValue()))
            .then();
    }
}

// 状态实现
public class ReactiveState<T> implements State<T> {
    
    private final String name;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Mono<T> get() {
        return redisTemplate.opsForValue()
            .get(stateKey())
            .cast(getTypeClass());
    }
    
    @Override
    public Mono<Void> update(T value) {
        return redisTemplate.opsForValue()
            .set(stateKey(), value)
            .then();
    }
    
    @Override
    public Mono<Boolean> compareAndSet(T expect, T update) {
        // 使用Lua脚本实现原子CAS
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                       "return redis.call('set', KEYS[1], ARGV[2]) else " +
                       "return 0 end";
        
        return redisTemplate.execute(
            RedisScript.of(script, Boolean.class),
            Collections.singletonList(stateKey()),
            expect, update
        ).next();
    }
}
```

**关键点**：
- ✅ 支持分布式状态存储
- ✅ 原子操作（CAS）
- ✅ 在流处理中使用不阻塞

### 4. 检查点 - ✅ 建议使用 Reactor

#### 为什么建议用？
- 涉及文件I/O或数据库I/O
- 在流处理中触发
- 需要定期调度

```java
@Service
public class ReactiveCheckpointCoordinator implements CheckpointCoordinator {
    
    private final StateManager stateManager;
    private final CheckpointStorage storage;
    
    @Override
    public Mono<Checkpoint> triggerCheckpoint() {
        return Mono.defer(() -> {
            String checkpointId = generateCheckpointId();
            
            // 1. 创建状态快照（异步）
            return stateManager.snapshot()
                // 2. 创建检查点对象
                .map(snapshot -> createCheckpoint(checkpointId, snapshot))
                // 3. 持久化到存储（异步）
                .flatMap(checkpoint -> storage.save(checkpoint)
                    .thenReturn(checkpoint))
                // 4. 记录到数据库（异步）
                .flatMap(checkpoint -> recordCheckpoint(checkpoint));
        })
        .doOnSuccess(cp -> log.info("Checkpoint created: {}", cp.getCheckpointId()))
        .timeout(Duration.ofMinutes(5));  // 检查点超时保护
    }
    
    @Override
    public Flux<Checkpoint> scheduleCheckpoints(Duration interval) {
        // 定期触发检查点
        return Flux.interval(interval)
            .flatMap(tick -> triggerCheckpoint()
                .onErrorResume(error -> {
                    log.error("Checkpoint failed", error);
                    return Mono.empty();  // 失败不中断调度
                }));
    }
    
    @Override
    public Mono<Void> restoreFromCheckpoint(String checkpointId) {
        return storage.load(checkpointId)
            .flatMap(checkpoint -> {
                Map<String, Object> snapshot = checkpoint.getStateSnapshot();
                return stateManager.restore(snapshot);
            });
    }
}

// 检查点存储实现
@Service
public class FileCheckpointStorage implements CheckpointStorage {
    
    private final Path storagePath;
    
    @Override
    public Mono<Void> save(Checkpoint checkpoint) {
        return Mono.fromCallable(() -> {
            // 序列化为JSON
            String json = objectMapper.writeValueAsString(checkpoint);
            // 写入文件
            Path file = getCheckpointFile(checkpoint.getCheckpointId());
            Files.writeString(file, json);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())  // 文件I/O，隔离到专用线程池
        .then();
    }
    
    @Override
    public Mono<Checkpoint> load(String checkpointId) {
        return Mono.fromCallable(() -> {
            Path file = getCheckpointFile(checkpointId);
            String json = Files.readString(file);
            return objectMapper.readValue(json, CheckpointImpl.class);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
}
```

**关键点**：
- ✅ 文件I/O异步化
- ✅ 定期调度不阻塞
- ✅ 超时保护

### 5. 指标收集 - ✅ 建议使用 Reactor

#### 为什么建议用？
- 需要定期推送指标
- 发送到外部监控系统（网络I/O）
- 不应阻塞业务逻辑

```java
@Service
public class ReactiveMetricsCollector implements MetricsCollector {
    
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final MetricsReporter reporter;
    
    @Override
    public Mono<Void> recordCounter(String name, long value, Map<String, String> tags) {
        // 同步更新内存计数器（快速）
        counters.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(value);
        
        // 不需要返回Mono，除非要立即持久化
        return Mono.empty();
    }
    
    @Override
    public Flux<Map<String, Object>> publishMetrics(Duration interval) {
        // 定期推送指标流
        return Flux.interval(interval)
            .map(tick -> snapshot())
            .flatMap(metrics -> reporter.report(metrics)
                .thenReturn(metrics))
            .onErrorContinue((error, metrics) -> 
                log.warn("Failed to report metrics", error));
    }
    
    @Override
    public Mono<Map<String, Object>> snapshot() {
        // 快照是内存操作，可以同步
        return Mono.fromCallable(() -> {
            Map<String, Object> snapshot = new HashMap<>();
            counters.forEach((name, value) -> 
                snapshot.put(name, value.get()));
            return snapshot;
        });
    }
}

// 指标报告器
@Service
public class PrometheusMetricsReporter implements MetricsReporter {
    
    private final WebClient webClient;
    
    @Override
    public Mono<Void> report(Map<String, Object> metrics) {
        // 异步发送到Prometheus Push Gateway
        return webClient.post()
            .uri("/metrics/job/{job}", "pipeline-framework")
            .bodyValue(formatMetrics(metrics))
            .retrieve()
            .bodyToMono(Void.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(error -> {
                log.warn("Failed to push metrics", error);
                return Mono.empty();
            });
    }
}
```

**关键点**：
- ✅ 内存操作可以同步（计数器更新）
- ✅ 网络I/O必须异步（发送指标）
- ✅ 定期推送用Flux

### 6. 配置管理 - ⚠️ 看场景

#### 高频查询（流处理中）- ✅ 用 Reactor

```java
@Service
public class ReactiveConfigService {
    
    private final R2dbcEntityTemplate template;
    private final ReactiveRedisTemplate<String, Object> cache;
    
    /**
     * 在流处理中获取配置 - 必须响应式
     */
    public Mono<OperatorConfig> getOperatorConfig(String operatorId) {
        // 1. 先查缓存
        return cache.opsForValue().get(configKey(operatorId))
            .cast(OperatorConfig.class)
            // 2. 缓存未命中，查数据库
            .switchIfEmpty(Mono.defer(() -> 
                template.selectOne(
                    Query.query(Criteria.where("operator_id").is(operatorId)),
                    OperatorConfig.class
                )
                // 3. 写入缓存
                .flatMap(config -> cache.opsForValue()
                    .set(configKey(operatorId), config, Duration.ofMinutes(10))
                    .thenReturn(config))
            ));
    }
}

// 在Operator中使用
public class DynamicOperator<IN, OUT> implements Operator<IN, OUT> {
    
    private final ReactiveConfigService configService;
    private final String operatorId;
    
    @Override
    public Flux<OUT> apply(Flux<IN> input) {
        return input.flatMap(data -> 
            // 每次处理都可能查询最新配置
            configService.getOperatorConfig(operatorId)
                .map(config -> transform(data, config))
        );
    }
}
```

#### 低频查询（启动时）- ⚠️ 同步可以

```java
@Service
public class ConfigLoader {
    
    private final JobMapper jobMapper;
    private Map<String, JobConfig> configCache;
    
    /**
     * 应用启动时加载配置 - 同步可接受
     */
    @PostConstruct
    public void loadConfigs() {
        log.info("Loading job configurations...");
        
        // 同步查询
        List<JobEntity> jobs = jobMapper.selectList(null);
        
        configCache = jobs.stream()
            .collect(Collectors.toMap(
                JobEntity::getJobId,
                this::parseConfig
            ));
        
        log.info("Loaded {} job configurations", configCache.size());
    }
    
    /**
     * 从缓存获取（内存操作）
     */
    public JobConfig getConfig(String jobId) {
        return configCache.get(jobId);
    }
}
```

### 7. 元数据 CRUD - ⚠️ 可选

#### 管理API - 同步更简单

```java
@RestController
@RequestMapping("/api/jobs")
public class JobController {
    
    private final JobService jobService;
    
    /**
     * 管理后台API - 同步即可
     */
    @GetMapping("/{id}")
    public JobEntity getJob(@PathVariable String id) {
        return jobService.getByIdSync(id);
    }
    
    @PostMapping
    public JobEntity createJob(@RequestBody JobEntity job) {
        return jobService.saveSync(job);
    }
    
    @GetMapping
    public PageResult<JobEntity> listJobs(
            @RequestParam int page,
            @RequestParam int size) {
        return jobService.listByPageSync(page, size);
    }
}
```

#### 在流处理中使用 - 建议响应式

```java
@Service
public class JobExecutionService {
    
    private final JobService jobService;
    
    /**
     * 流处理中查询Job信息 - 建议响应式
     */
    public Mono<Void> executeJob(String jobId) {
        return jobService.getByJobId(jobId)  // 响应式查询
            .flatMap(job -> buildPipeline(job))
            .flatMap(pipeline -> pipeline.execute())
            .then();
    }
}
```

## 判断标准

### 使用 Reactor 的判断标准

```
是否需要 Reactor？
    ↓
[涉及I/O操作？]
    ├─ 是 → [调用频率？]
    │       ├─ 高频 → ✅ 必须用 Reactor
    │       └─ 低频 → ⚠️ 可选（建议用）
    └─ 否 → [纯计算？]
            ├─ 是 → ❌ 不用 Reactor
            └─ 否 → [在流处理中？]
                    ├─ 是 → ✅ 必须用 Reactor
                    └─ 否 → ⚠️ 可选
```

### 具体判断问题

1. **有网络I/O吗？**（数据库、HTTP、消息队列）
   - 是 → ✅ 用 Reactor

2. **有文件I/O吗？**
   - 是，且文件大 → ✅ 用 Reactor
   - 是，且文件小且不频繁 → ⚠️ 可选

3. **操作频繁吗？**
   - 是（每秒多次） → ✅ 用 Reactor
   - 否（启动时、人工操作） → ⚠️ 可选

4. **在数据流处理中调用吗？**
   - 是 → ✅ 必须用 Reactor
   - 否 → ⚠️ 可选

5. **需要并发执行吗？**
   - 是 → ✅ 用 Reactor
   - 否 → ⚠️ 可选

## 实践建议

### 1. 优先级排序

**必须用 Reactor（P0）**：
- ✅ 数据流处理（Source/Operator/Sink）
- ✅ Job执行器
- ✅ 流式指标推送

**建议用 Reactor（P1）**：
- ✅ Job调度器
- ✅ 状态管理（持久化）
- ✅ 检查点
- ✅ 指标收集（发送）
- ✅ 配置查询（在流处理中）

**可选用 Reactor（P2）**：
- ⚠️ 配置加载（启动时）
- ⚠️ 元数据CRUD（管理API）
- ⚠️ 本地缓存操作

**不用 Reactor（P3）**：
- ❌ 日志记录
- ❌ 纯计算
- ❌ 简单内存操作

### 2. 渐进式引入

#### 阶段1：核心必须响应式
```java
// 数据流处理
source.read() → operator.apply() → sink.write()

// Job执行
jobExecutor.submit(job)
```

#### 阶段2：扩展建议响应式
```java
// 调度
scheduler.schedule(job, config)

// 状态
stateManager.snapshot()

// 检查点
checkpointCoordinator.triggerCheckpoint()
```

#### 阶段3：逐步优化
```java
// 配置查询
configService.getConfig(id)  // 从同步改为响应式

// 元数据
jobService.getByJobId(id)  // 从同步改为响应式
```

### 3. 混合使用策略

```java
@Service
public class HybridJobService {
    
    private final JobMapper jobMapper;  // MyBatis Plus（同步）
    
    /**
     * 响应式API - 包装同步调用
     * 用于流处理中调用
     */
    public Mono<JobEntity> getByJobId(String jobId) {
        return Mono.fromCallable(() -> jobMapper.selectByJobId(jobId))
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * 同步API - 直接调用
     * 用于管理后台
     */
    public JobEntity getByJobIdSync(String jobId) {
        return jobMapper.selectByJobId(jobId);
    }
    
    /**
     * 根据场景选择
     */
    public Object getJob(String jobId, boolean async) {
        if (async) {
            return getByJobId(jobId);  // 返回 Mono<JobEntity>
        } else {
            return getByJobIdSync(jobId);  // 返回 JobEntity
        }
    }
}
```

## 总结

### 核心原则

1. **I/O边界必须响应式** - 所有外部系统交互
2. **数据流必须响应式** - Source到Sink的完整链路
3. **高频操作建议响应式** - 避免阻塞累积
4. **低频操作可以同步** - 启动、配置、管理
5. **纯计算不用响应式** - 避免过度抽象

### 记住三句话

1. **有I/O就用Reactor** - 数据库、网络、文件
2. **在流里就用Reactor** - 数据流处理中的所有调用
3. **其他看情况** - 频繁用Reactor，偶尔可同步

### 最后的建议

**不要过度使用 Reactor**：
- ❌ 不是所有代码都要响应式
- ❌ 不是所有方法都要返回Mono/Flux
- ✅ 在关键路径上使用（数据流、I/O）
- ✅ 其他地方根据实际需求决定

**找到平衡点**：
- 响应式带来的好处 > 增加的复杂度 → 使用
- 响应式带来的好处 < 增加的复杂度 → 不用

项目中已经提供了**两套API**（响应式 + 同步），可以根据实际场景灵活选择！
