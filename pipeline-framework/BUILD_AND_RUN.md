# 构建和运行指南

## 快速开始

### 1. 构建项目

```bash
# 进入项目目录
cd /workspace/pipeline-framework

# 编译整个项目（跳过测试）
mvn clean install -DskipTests

# 或者编译并运行测试
mvn clean install
```

### 2. 使用Docker Compose启动（推荐）

```bash
# 启动所有服务（包括MySQL、Kafka、Redis、应用）
docker-compose up -d

# 查看日志
docker-compose logs -f etl-framework

# 查看所有容器状态
docker-compose ps

# 停止所有服务
docker-compose down
```

### 3. 本地开发模式

#### 3.1 启动依赖服务

```bash
# 只启动MySQL、Kafka、Redis
docker-compose up -d mysql kafka redis zookeeper

# 等待服务启动完成
docker-compose ps
```

#### 3.2 初始化数据库

```bash
# 方式1: 使用Docker exec
docker exec -i etl-mysql mysql -uroot -proot123 etl_framework < docs/database-schema.sql

# 方式2: 使用本地MySQL客户端
mysql -h localhost -P 3306 -u root -proot123 etl_framework < docs/database-schema.sql
```

#### 3.3 启动应用

```bash
# 方式1: 使用Maven
cd etl-starter
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式2: 直接运行JAR
java -jar etl-starter/target/etl-starter-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### 4. 验证服务

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 查看信息
curl http://localhost:8080/actuator/info

# 查看Prometheus指标
curl http://localhost:8080/actuator/prometheus
```

## 开发调试

### 使用IDE运行

#### IntelliJ IDEA

1. 导入项目：File → Open → 选择项目根目录的pom.xml
2. 等待Maven导入完成
3. 找到`EtlFrameworkApplication.java`
4. 右键 → Run 'EtlFrameworkApplication'

#### VS Code

1. 安装Java Extension Pack
2. 打开项目文件夹
3. 按F5启动调试

### 配置开发环境

编辑 `etl-starter/src/main/resources/application-dev.yml`：

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://localhost:3306/etl_framework
    username: root
    password: root123

logging:
  level:
    com.etl.framework: DEBUG
```

### 热重载

```bash
# 启用Spring Boot DevTools进行热重载
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 测试

### 运行单元测试

```bash
# 运行所有测试
mvn test

# 运行特定模块的测试
mvn test -pl etl-api

# 运行特定测试类
mvn test -Dtest=DataSourceTest
```

### 运行集成测试

```bash
# 运行集成测试
mvn verify

# 跳过单元测试，只运行集成测试
mvn verify -DskipUnitTests
```

## 打包部署

### 构建Docker镜像

```bash
# 构建镜像
docker build -t etl-framework:1.0.0 .

# 查看镜像
docker images | grep etl-framework

# 运行镜像
docker run -d \
  --name etl-framework \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=host.docker.internal \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=password \
  etl-framework:1.0.0
```

### 生产环境部署

```bash
# 1. 编译生产版本
mvn clean package -Pprod -DskipTests

# 2. 复制JAR文件
cp etl-starter/target/etl-starter-1.0.0-SNAPSHOT.jar /opt/etl-framework/

# 3. 创建systemd服务（Linux）
sudo cat > /etc/systemd/system/etl-framework.service <<EOF
[Unit]
Description=ETL Framework Service
After=network.target

[Service]
Type=simple
User=etl
WorkingDirectory=/opt/etl-framework
ExecStart=/usr/bin/java \
  -Xms1g -Xmx4g \
  -XX:+UseG1GC \
  -Dspring.profiles.active=prod \
  -jar /opt/etl-framework/etl-starter-1.0.0-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 4. 启动服务
sudo systemctl daemon-reload
sudo systemctl enable etl-framework
sudo systemctl start etl-framework

# 5. 查看状态
sudo systemctl status etl-framework

# 6. 查看日志
sudo journalctl -u etl-framework -f
```

## 监控

### Prometheus + Grafana

```bash
# 访问Prometheus
open http://localhost:9090

# 访问Grafana
open http://localhost:3000
# 默认账号: admin/admin
```

### 查看日志

```bash
# Docker方式
docker-compose logs -f etl-framework

# 本地方式
tail -f /var/log/etl-framework/application.log

# 查看错误日志
tail -f /var/log/etl-framework/error.log
```

## 故障排查

### 常见问题

#### 1. 数据库连接失败

```bash
# 检查MySQL是否启动
docker-compose ps mysql

# 检查连接
mysql -h localhost -P 3306 -u root -proot123 -e "SELECT 1"

# 查看应用日志
docker-compose logs etl-framework | grep -i "database\|mysql\|r2dbc"
```

#### 2. Kafka连接失败

```bash
# 检查Kafka是否启动
docker-compose ps kafka

# 测试Kafka连接
docker exec -it etl-kafka kafka-topics --list --bootstrap-server localhost:9092
```

#### 3. 应用启动失败

```bash
# 查看详细日志
docker-compose logs --tail=100 etl-framework

# 检查端口占用
lsof -i :8080
netstat -an | grep 8080
```

#### 4. 内存不足

```bash
# 增加堆内存
export JAVA_OPTS="-Xms1g -Xmx4g"
mvn spring-boot:run

# 或修改Dockerfile中的JAVA_OPTS
```

## 清理

```bash
# 停止所有容器
docker-compose down

# 删除所有数据卷（警告：会删除所有数据！）
docker-compose down -v

# 清理Maven构建
mvn clean

# 清理Docker镜像
docker rmi etl-framework:1.0.0
```

## 性能调优

### JVM参数

```bash
# 推荐的生产环境JVM参数
JAVA_OPTS="
  -Xms2g
  -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/var/log/etl-framework/heapdump.hprof
  -XX:+PrintGCDetails
  -XX:+PrintGCDateStamps
  -Xloggc:/var/log/etl-framework/gc.log
  -XX:+UseGCLogFileRotation
  -XX:NumberOfGCLogFiles=10
  -XX:GCLogFileSize=100M
"
```

### 数据库连接池

编辑 `application-prod.yml`:

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: 30m
      max-create-connection-time: 30s
```

## 更多信息

- [项目结构说明](PROJECT_STRUCTURE.md)
- [开发文档](docs/pipeline-framework-design.md)
- [贡献指南](CONTRIBUTING.md)
- [README](README.md)

---

**文档版本**: 1.0  
**最后更新**: 2025-11-09
