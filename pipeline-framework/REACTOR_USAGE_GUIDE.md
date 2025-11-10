# Project Reactor 使用指南

## 何时使用 Reactor？

### ✅ 必须使用 Reactor 的场景

#### 1. **数据流处理**（核心流程）
```java
// Source → Operator → Sink 整个链路必须是响应式的
Flux<Data> dataStream = source.read();          // 必须
Flux<Result> transformed = operator.apply(dataStream);  // 必须
Mono<Void> written = sink.write(transformed);   // 必须
```

#### 2. **I/O 操作**
```java
// 数据库操作
Mono<User> user = userRepository.findById(id);  // 必须

// 网络请求
Mono<Response> response = webClient.get().retrieve().bodyToMono(Response.class);  // 必须

// 文件操作（大文件）
Flux<String> lines = DataBufferUtils.read(path, ...);  // 建议
```

#### 3. **外部系统交互**
```java
// Kafka消息
Flux<Record> records = kafkaReceiver.receive();  // 必须

// Redis操作
Mono<String> value = reactiveRedisTemplate.opsForValue().get(key);  // 建议

// HTTP API调用
Mono<Data> data = webClient.post().bodyValue(request).retrieve().bodyToMono(Data.class);  // 必须
```

### ⚠️ 可选使用 Reactor 的场景

#### 1. **配置和元数据查询**（不频繁调用）
```java
// 可以使用 Reactor
Mono<JobConfig> config = configService.getConfig(jobId);

// 也可以使用同步
JobConfig config = configService.getConfigSync(jobId);
```

**建议**：如果调用频率低（如启动时加载配置），可以用同步；如果在流处理中调用，用Reactor。

#### 2. **缓存操作**
```java
// 简单缓存可以同步
Map<String, Object> cache = new ConcurrentHashMap<>();
Object value = cache.get(key);

// 分布式缓存建议响应式
Mono<Object> value = reactiveCache.get(key);
```

#### 3. **日志记录**
```java
// 同步日志记录是可以的
log.info("Processing data: {}", data);

// 不需要
// Mono.fromRunnable(() -> log.info(...)).subscribe();
```

### ❌ 不应该使用 Reactor 的场景

#### 1. **纯计算操作**（无I/O）
```java
// ❌ 不需要
Mono<Integer> result = Mono.fromCallable(() -> x + y);

// ✅ 直接计算
int result = x + y;
```

#### 2. **简单的内存操作**
```java
// ❌ 过度使用
Mono<String> value = Mono.just(map.get(key));

// ✅ 直接操作
String value = map.get(key);
```

#### 3. **阻塞且无法改造的第三方库**
```java
// 如果必须用阻塞库，隔离到专门的线程池
Mono<Result> result = Mono.fromCallable(() -> blockingLibrary.call())
    .subscribeOn(Schedulers.boundedElastic());  // 使用专门的线程池
```

## 实践建议

### 层次划分

```
┌─────────────────────────────────────────┐
│  Controller/API Layer                   │  ← 使用 Reactor
│  返回 Mono/Flux                         │
├─────────────────────────────────────────┤
│  Service Layer                          │  ← 混合使用
│  - 业务逻辑：可同步                     │
│  - I/O操作：用 Reactor                  │
├─────────────────────────────────────────┤
│  Repository/DAO Layer                   │  ← 使用 Reactor
│  R2DBC/Reactive MongoDB                 │  (如果用响应式DB)
├─────────────────────────────────────────┤
│  Stream Processing Layer                │  ← 必须 Reactor
│  Source → Operator → Sink              │
└─────────────────────────────────────────┘
```

### 本项目的使用策略

#### 核心流处理 - 100% Reactor
```java
// Pipeline执行
public Mono<PipelineResult> execute() {
    return source.read()                    // Flux<T>
        .transform(operatorChain::execute)  // Flux<T>
        .as(sink::write)                    // Mono<Void>
        .then(Mono.just(result));
}
```

#### Job管理 - 大部分 Reactor
```java
// JobScheduler
public Mono<ScheduleResult> schedule(Job job, ScheduleConfig config) {
    return Mono.defer(() -> {
        // 业务逻辑（同步）
        Schedule schedule = createSchedule(job, config);
        
        // 持久化（响应式）
        return scheduleRepository.save(schedule)
            .map(this::toScheduleResult);
    });
}
```

#### 状态和检查点 - Reactor
```java
// StateManager
public Mono<Void> saveState(String name, Object value) {
    return stateRepository.save(name, value);  // 响应式持久化
}

// CheckpointCoordinator
public Mono<Checkpoint> triggerCheckpoint() {
    return stateManager.snapshot()             // Mono<Map>
        .flatMap(snapshot -> {
            Checkpoint checkpoint = createCheckpoint(snapshot);
            return checkpointStorage.save(checkpoint);  // Mono<Void>
        })
        .thenReturn(checkpoint);
}
```

#### 配置和元数据 - 混合使用
```java
// 启动时加载（同步可接受）
@PostConstruct
public void init() {
    List<Connector> connectors = loadConnectors();  // 同步
    connectors.forEach(connectorRegistry::register);
}

// 运行时查询（建议响应式）
public Mono<JobConfig> getJobConfig(String jobId) {
    return configRepository.findById(jobId);  // Mono<JobConfig>
}
```

## 性能考虑

### 何时响应式带来好处？

1. **高并发I/O**
   - 大量数据库查询
   - 多个HTTP请求
   - 文件读写

2. **长连接和流式数据**
   - WebSocket
   - Server-Sent Events
   - Kafka消费

3. **需要背压控制**
   - 生产速度 > 消费速度
   - 需要限流

### 何时响应式可能降低性能？

1. **纯CPU密集型计算**
   - 响应式的调度开销 > 并行计算收益

2. **极简单的操作**
   - 一次数据库查询 + 简单转换
   - 响应式的抽象层开销可能更大

3. **阻塞操作**
   - 必须使用 `subscribeOn(Schedulers.boundedElastic())`
   - 引入额外的线程切换开销

## 最佳实践

### 1. 避免阻塞
```java
// ❌ 错误：在响应式链中阻塞
public Mono<Result> process(String id) {
    Result result = blockingService.get(id);  // 阻塞！
    return Mono.just(result);
}

// ✅ 正确：隔离阻塞操作
public Mono<Result> process(String id) {
    return Mono.fromCallable(() -> blockingService.get(id))
        .subscribeOn(Schedulers.boundedElastic());
}
```

### 2. 正确的错误处理
```java
public Flux<Data> processData() {
    return source.read()
        .onErrorContinue((error, data) -> {
            log.error("Error processing: {}", data, error);
            // 继续处理下一个
        })
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
}
```

### 3. 资源管理
```java
public Flux<Data> readFile(Path path) {
    return Flux.using(
        () -> Files.newInputStream(path),      // 获取资源
        inputStream -> readFromStream(inputStream),  // 使用资源
        inputStream -> {                        // 清理资源
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("Error closing stream", e);
            }
        }
    );
}
```

### 4. 背压处理
```java
public Flux<Data> processWithBackpressure() {
    return source.read()
        .onBackpressureBuffer(1000)          // 缓冲区
        .onBackpressureDrop(data ->          // 丢弃策略
            log.warn("Dropped: {}", data))
        .limitRate(100);                     // 限速
}
```

## 调试建议

### 启用日志
```java
Flux<Data> flux = source.read()
    .log("source-read")                      // 记录所有信号
    .map(this::transform)
    .log("transform")
    .filter(this::validate)
    .log("filter");
```

### 检查点（Checkpoint）
```java
Flux<Data> flux = source.read()
    .checkpoint("after-source")              // 标记位置
    .map(this::transform)
    .checkpoint("after-transform")
    .filter(this::validate);
```

### 订阅追踪
```java
// 启用订阅追踪
Hooks.onOperatorDebug();

// 生产环境禁用（性能影响）
Hooks.resetOnOperatorDebug();
```

## 总结

### Pipeline Framework 中的 Reactor 使用原则

1. **数据流处理**：必须全程使用 Reactor（Source → Operator → Sink）
2. **外部I/O**：建议使用 Reactor（数据库、缓存、消息队列、HTTP）
3. **业务逻辑**：简单的可以同步，复杂的组合建议 Reactor
4. **配置管理**：启动时可同步，运行时建议 Reactor
5. **日志和监控**：同步即可
6. **纯计算**：同步即可

### 记住三个原则

1. **I/O 边界必须响应式** - 所有与外部系统交互的地方
2. **数据流必须响应式** - 从源到目标的整个流程
3. **其他地方看情况** - 根据并发需求和调用频率决定
