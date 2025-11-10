# Reactive ETL Framework

基于Spring Boot和Project Reactor的响应式ETL数据处理框架。

## 项目简介

本项目是一个轻量级的ETL（Extract-Transform-Load）数据采集框架，借鉴Apache Flink的设计理念，采用Source、Operator、Sink的经典数据处理模型，并基于Project Reactor实现完全响应式的数据流处理。

### 核心特性

- ✅ **响应式流处理**: 基于Reactor实现非阻塞、背压支持的数据流处理
- ✅ **模块化设计**: 清晰的任务调度、图转换、执行引擎分层架构
- ✅ **高性能**: 充分利用响应式编程的优势，支持高吞吐量数据处理
- ✅ **易用性**: 提供简洁的API，支持声明式任务定义
- ✅ **可观测性**: 内置监控指标和日志，方便运维调试
- ✅ **可扩展性**: 基于Connectors的插件化扩展机制

## 技术栈

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Project Reactor**: 3.6.0
- **数据库**: MySQL 8.0 (R2DBC)
- **消息队列**: Apache Kafka
- **缓存**: Redis
- **监控**: Micrometer + Prometheus + Grafana
- **构建工具**: Maven 3.9+

## 项目结构

```
pipeline-framework/
├── etl-api/              # 核心API定义
├── etl-core/             # 核心运行时实现
├── etl-connectors/       # 连接器实现（JDBC、Kafka等）
├── etl-operators/        # 算子实现（Map、Filter等）
├── etl-scheduler/        # 任务调度
├── etl-executor/         # 任务执行引擎
├── etl-state/            # 状态管理
├── etl-checkpoint/       # 检查点机制
├── etl-metrics/          # 监控指标
├── etl-web/              # Web API
├── etl-starter/          # Spring Boot启动模块
├── docs/                 # 设计文档
├── Dockerfile            # Docker镜像构建
└── docker-compose.yml    # Docker Compose配置
```

## 快速开始

### 前置要求

- Java 17+
- Maven 3.9+
- Docker & Docker Compose (可选)

### 本地开发

1. **克隆项目**

```bash
git clone <repository-url>
cd pipeline-framework
```

2. **编译项目**

```bash
mvn clean install
```

3. **启动数据库**

```bash
# 使用Docker Compose启动MySQL
docker-compose up -d mysql

# 初始化数据库
mysql -h localhost -u root -p < docs/database-schema.sql
```

4. **启动应用**

```bash
cd etl-starter
mvn spring-boot:run
```

5. **访问应用**

- Web UI: http://localhost:8080
- Actuator: http://localhost:8080/actuator
- Health Check: http://localhost:8080/actuator/health

### Docker部署

1. **构建并启动所有服务**

```bash
docker-compose up -d
```

2. **查看日志**

```bash
docker-compose logs -f etl-framework
```

3. **停止服务**

```bash
docker-compose down
```

## 开发指南

### 添加自定义Connector

1. 在`etl-connectors`模块创建新的Connector类
2. 实现`DataSource`或`DataSink`接口
3. 使用`@Component`注解注册到Spring容器

```java
@Component
public class CustomSource implements DataSource<MyData> {
    @Override
    public Flux<MyData> getDataStream() {
        // 实现数据读取逻辑
    }
    // ... 其他方法实现
}
```

### 添加自定义Operator

1. 在`etl-operators`模块创建新的Operator类
2. 实现`Operator`接口
3. 使用`@Component`注解注册

```java
@Component
public class CustomOperator implements Operator<Input, Output> {
    @Override
    public Flux<Output> apply(Flux<Input> input) {
        return input.map(this::transform);
    }
    // ... 其他方法实现
}
```

### 代码规范

- 遵循Google Java Style
- 所有公共方法必须有JavaDoc
- 使用SLF4J进行日志记录
- 使用泛型提高代码复用性
- 资源必须正确关闭和清理

## 配置说明

### application.yml

主要配置项：

```yaml
spring:
  application:
    name: pipeline-framework
  r2dbc:
    url: r2dbc:mysql://localhost:3306/etl_framework
    username: root
    password: password

etl:
  framework:
    executor:
      thread-pool:
        core-size: 10
        max-size: 50
    checkpoint:
      enabled: true
      interval-seconds: 60
    metrics:
      enabled: true
```

更多配置请参考 `etl-starter/src/main/resources/application-dev.yml`

## 监控

### Prometheus指标

访问 http://localhost:8080/actuator/prometheus 查看所有指标

### Grafana Dashboard

1. 访问 http://localhost:3000 (默认账号: admin/admin)
2. 添加Prometheus数据源: http://prometheus:9090
3. 导入Dashboard配置

## 测试

### 运行单元测试

```bash
mvn test
```

### 运行集成测试

```bash
mvn verify
```

## 文档

详细文档请查看 `docs/` 目录：

- [系统架构设计](docs/pipeline-framework-design.md)
- [数据库设计](docs/database-design.md)
- [StreamGraph配置](docs/graph-definition-examples.md)
- [JSON示例](docs/graph-definition-json-examples.json)

## 贡献指南

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

## 许可证

[MIT License](LICENSE)

## 联系方式

- 问题反馈: [GitHub Issues](<repository-url>/issues)
- 邮件: etl-framework-team@example.com

---

**版本**: 1.0.0-SNAPSHOT  
**最后更新**: 2025-11-09
