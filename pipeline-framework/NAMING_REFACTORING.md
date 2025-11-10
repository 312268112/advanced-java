# 命名重构说明

## 🎯 重构目标

1. **去掉 "Spring" 前缀**：类名更简洁，不体现技术栈
2. **使用 Spring 自动装配**：配置类使用 @ConfigurationProperties 等注解
3. **Adapter 模式**：配置转换使用适配器模式

---

## 📋 类名重构对照表

### Factory 类

| 旧名称 | 新名称 | 说明 |
|-------|--------|-----|
| `SpringSourceFactory` | `SourceFactory` | 去掉 Spring 前缀 |
| `SpringSinkFactory` | `SinkFactory` | 去掉 Spring 前缀 |
| `SpringOperatorFactory` | `OperatorFactory` | 去掉 Spring 前缀 |

### Builder 类

| 旧名称 | 新名称 | 说明 |
|-------|--------|-----|
| `SpringGraphBasedPipelineBuilder` | `GraphPipelineBuilder` | 去掉 Spring 前缀，简化名称 |

### Config 类（改用 Adapter）

| 旧名称 | 新名称 | 说明 |
|-------|--------|-----|
| `SimpleSourceConfig` | `SourceConfigAdapter` | 使用适配器模式 |
| `SimpleOperatorConfig` | `OperatorConfigAdapter` | 使用适配器模式 |
| `SimpleSinkConfig` | `SinkConfigAdapter` | 使用适配器模式 |

### Configuration 类

| 旧名称 | 新名称 | 说明 |
|-------|--------|-----|
| `ReactorSchedulerConfig` | `ReactorSchedulerConfiguration` | 使用 Configuration 后缀 |

### 目录结构

| 旧路径 | 新路径 | 说明 |
|-------|--------|-----|
| `.../core/config/` | `.../core/scheduler/` | 调整目录结构 |

---

## 🏗️ 架构改进

### 1. 配置类改用适配器模式

**改造前**（SimpleSourceConfig 等）：
```java
public class SimpleSourceConfig implements SourceConfig {
    private final Map<String, Object> properties;
    
    public SimpleSourceConfig(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }
    // ...
}
```

**改造后**（SourceConfigAdapter）：
```java
public class SourceConfigAdapter implements SourceConfig {
    private final Map<String, Object> properties;
    
    private SourceConfigAdapter(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
    }
    
    // 静态工厂方法，更清晰的意图
    public static SourceConfig from(StreamNode node) {
        return new SourceConfigAdapter(node.getConfig());
    }
    // ...
}
```

**优势**：
- ✅ 清晰表达"适配"的意图
- ✅ 私有构造函数 + 静态工厂方法
- ✅ 符合适配器模式

### 2. Spring 配置自动装配

**ReactorSchedulerConfiguration**：
```java
@Configuration
@EnableConfigurationProperties(ReactorSchedulerProperties.class)
public class ReactorSchedulerConfiguration {
    
    @Bean(name = "ioScheduler", destroyMethod = "dispose")
    public Scheduler ioScheduler(ReactorSchedulerProperties properties) {
        // Spring 自动注入 properties
        ReactorSchedulerProperties.SchedulerConfig ioConfig = properties.getIo();
        return Schedulers.newBoundedElastic(...);
    }
}
```

**ReactorSchedulerProperties**：
```java
@Component
@ConfigurationProperties(prefix = "reactor.scheduler")
public class ReactorSchedulerProperties {
    private SchedulerConfig io = new SchedulerConfig();
    private SchedulerConfig compute = new SchedulerConfig();
    // Spring 自动绑定配置
}
```

**application.yml**：
```yaml
reactor:
  scheduler:
    io:
      pool-size: 100
      queue-size: 1000
```

**优势**：
- ✅ Spring 自动绑定配置
- ✅ 类型安全
- ✅ IDE 自动补全
- ✅ 支持配置校验

---

## 📁 目录结构变化

### 改造前
```
pipeline-core/src/main/java/com/pipeline/framework/core/
├── builder/
│   ├── SpringGraphBasedPipelineBuilder.java
│   ├── SimpleSourceConfig.java
│   ├── SimpleOperatorConfig.java
│   └── SimpleSinkConfig.java
├── config/
│   ├── ReactorSchedulerConfig.java
│   └── ReactorSchedulerProperties.java
└── factory/
    ├── SpringSourceFactory.java
    ├── SpringSinkFactory.java
    └── SpringOperatorFactory.java
```

### 改造后
```
pipeline-core/src/main/java/com/pipeline/framework/core/
├── builder/
│   ├── GraphPipelineBuilder.java ✅
│   ├── SourceConfigAdapter.java ✅
│   ├── OperatorConfigAdapter.java ✅
│   └── SinkConfigAdapter.java ✅
├── scheduler/ ✅ (新目录)
│   ├── ReactorSchedulerConfiguration.java ✅
│   └── ReactorSchedulerProperties.java
└── factory/
    ├── SourceFactory.java ✅
    ├── SinkFactory.java ✅
    └── OperatorFactory.java ✅
```

---

## 🔄 使用示例

### Factory 使用

```java
@Service
public class PipelineService {
    
    private final SourceFactory sourceFactory;  // 不再是 SpringSourceFactory
    
    public PipelineService(SourceFactory sourceFactory) {
        this.sourceFactory = sourceFactory;
    }
    
    public Mono<DataSource<?>> createSource(StreamNode node) {
        SourceConfig config = SourceConfigAdapter.from(node);  // 使用 Adapter
        return sourceFactory.createSource(config);
    }
}
```

### Builder 使用

```java
@Service
public class ExecutionService {
    
    private final GraphPipelineBuilder builder;  // 不再是 SpringGraphBasedPipelineBuilder
    
    public ExecutionService(GraphPipelineBuilder builder) {
        this.builder = builder;
    }
    
    public Mono<Pipeline<?, ?>> buildPipeline(StreamGraph graph) {
        return builder.buildFromGraph(graph);
    }
}
```

### 配置使用

```java
@Component
public class MyComponent {
    
    private final Scheduler ioScheduler;
    
    public MyComponent(@Qualifier("ioScheduler") Scheduler ioScheduler) {
        this.ioScheduler = ioScheduler;
    }
}
```

---

## ✅ 改进总结

### 命名改进

- ✅ **去掉技术栈前缀**：`SpringSourceFactory` → `SourceFactory`
- ✅ **使用业务术语**：更关注"做什么"而不是"用什么"
- ✅ **简洁明了**：类名更短、更清晰

### 架构改进

- ✅ **适配器模式**：配置转换使用 `XXXAdapter.from()` 静态工厂
- ✅ **Spring 自动装配**：配置类使用 `@ConfigurationProperties`
- ✅ **职责分离**：Builder 负责构建，Adapter 负责转换

### 代码质量

- ✅ **可读性**：类名更简洁，意图更清晰
- ✅ **可维护性**：目录结构更合理
- ✅ **可扩展性**：符合设计模式

---

## 📚 相关文档

- `FINAL_REFACTORING_SUMMARY.md` - 终极重构总结
- `REFACTORING_ARCHITECTURE.md` - 架构重构说明
- `DESIGN_PATTERN_EXPLANATION.md` - 设计模式详解

---

## 🎓 命名原则

### 应该遵循的原则

1. **业务导向**：类名反映业务意图，不体现技术栈
2. **简洁明了**：去掉冗余前缀/后缀
3. **一致性**：同类型的类使用统一的命名风格
4. **可读性**：让人一眼能看懂类的用途

### 应该避免的命名

- ❌ `SpringXXX`：不要在类名中体现技术栈
- ❌ `SimpleXXX`：Simple 没有实际意义
- ❌ `XXXImpl`：实现类尽量用更具体的名字
- ❌ `XXXConfig`：配置类用 Adapter、Properties 等更准确的术语

### 推荐的命名

- ✅ `XXXFactory`：工厂类
- ✅ `XXXBuilder`：建造者类
- ✅ `XXXAdapter`：适配器类
- ✅ `XXXConfiguration`：Spring 配置类
- ✅ `XXXProperties`：配置属性类
- ✅ `XXXExecutor`：执行器类
- ✅ `XXXRegistry`：注册表类

---

**重构完成！代码更简洁、更清晰、更符合业务语义！** ✅
