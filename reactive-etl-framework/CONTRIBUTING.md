# 贡献指南

感谢你对Reactive ETL Framework项目的关注！

## 如何贡献

### 报告Bug

如果发现Bug，请通过GitHub Issues提交，包含以下信息：

1. **Bug描述**: 清晰描述问题
2. **复现步骤**: 详细的复现步骤
3. **期望行为**: 你期望的正确行为
4. **实际行为**: 实际发生的错误行为
5. **环境信息**: Java版本、操作系统等
6. **日志**: 相关的错误日志

### 提交功能请求

通过GitHub Issues提交功能请求，包含：

1. **功能描述**: 清晰描述新功能
2. **使用场景**: 为什么需要这个功能
3. **预期效果**: 功能的预期表现

### 提交代码

1. **Fork项目**

```bash
git clone <your-fork-url>
cd reactive-etl-framework
```

2. **创建分支**

```bash
git checkout -b feature/your-feature-name
# 或
git checkout -b bugfix/your-bugfix-name
```

3. **编写代码**

遵循以下规范：

- 遵循Google Java Style Guide
- 所有公共方法必须有JavaDoc
- 添加单元测试
- 确保所有测试通过
- 更新相关文档

4. **提交代码**

```bash
git add .
git commit -m "feat: add amazing feature"
```

提交信息格式：
- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

5. **推送代码**

```bash
git push origin feature/your-feature-name
```

6. **创建Pull Request**

在GitHub上创建Pull Request，描述你的更改。

## 代码规范

### Java代码规范

- 使用Google Java Style
- 类名使用大驼峰命名
- 方法和变量使用小驼峰命名
- 常量使用全大写下划线分隔

### 日志规范

```java
// 使用SLF4J
private static final Logger log = LoggerFactory.getLogger(YourClass.class);

// 日志级别
log.trace("详细的调试信息");
log.debug("调试信息");
log.info("重要的业务流程");
log.warn("警告信息");
log.error("错误信息", exception);
```

### 异常处理

```java
// 提供有意义的错误信息
throw new SourceException("Failed to connect to database: " + dbUrl, cause);

// 使用特定的异常类型
try {
    // ...
} catch (IOException e) {
    throw new SourceException("I/O error while reading file", e);
}
```

### 资源管理

```java
// 使用try-with-resources
try (Connection conn = getConnection()) {
    // use connection
}

// 或在finally中清理
try {
    // use resource
} finally {
    cleanup();
}
```

## 测试规范

### 单元测试

```java
@Test
public void testMapOperator() {
    // Given
    MapOperator<Integer, String> operator = new MapOperator<>(i -> "value-" + i);
    Flux<Integer> input = Flux.just(1, 2, 3);
    
    // When
    Flux<String> output = operator.apply(input);
    
    // Then
    StepVerifier.create(output)
        .expectNext("value-1", "value-2", "value-3")
        .verifyComplete();
}
```

### 集成测试

使用`@SpringBootTest`进行集成测试。

## 文档规范

### JavaDoc

```java
/**
 * 数据源接口，所有Source实现必须实现此接口。
 * <p>
 * DataSource负责从外部系统读取数据并转换为响应式流。
 * </p>
 *
 * @param <T> 输出数据类型
 * @author Your Name
 * @since 1.0.0
 */
public interface DataSource<T> {
    // ...
}
```

### Markdown文档

- 使用清晰的标题层级
- 添加代码示例
- 包含必要的图表

## 设计模式

必须使用的模式：

1. **Builder模式**: 复杂对象构建
2. **Factory模式**: 组件创建
3. **Strategy模式**: 算法选择
4. **Observer模式**: 状态通知
5. **Template方法**: 流程定义

## 提交前检查清单

- [ ] 代码遵循项目规范
- [ ] 添加了必要的测试
- [ ] 所有测试通过
- [ ] 更新了相关文档
- [ ] 提交信息清晰明确
- [ ] 没有引入不必要的依赖
- [ ] 代码通过了静态分析

## 联系方式

如有问题，请通过以下方式联系：

- GitHub Issues
- 邮件: etl-framework-team@example.com

感谢你的贡献！
