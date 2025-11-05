# Flink 系统架构图

## Flink 完整系统架构

```mermaid
graph TB
    subgraph 应用层
        APP[Flink Application<br/>DataStream API / Table API / SQL]
    end
    
    subgraph 运行时层JobManager
        JM[JobManager]
        DISPATCHER[Dispatcher<br/>接收作业提交]
        RM[ResourceManager<br/>资源管理]
        JOBMASTER[JobMaster<br/>作业协调]
        CHECKPOINT[CheckpointCoordinator<br/>检查点协调器]
    end
    
    subgraph 运行时层TaskManager
        TM1[TaskManager 1]
        TM2[TaskManager 2]
        TM3[TaskManager N]
        
        SLOT1[Task Slot 1<br/>执行线程]
        SLOT2[Task Slot 2<br/>执行线程]
        
        NETWORK[Network Buffers<br/>数据交换缓冲区]
        MEMORY[Managed Memory<br/>托管内存]
    end
    
    subgraph 状态管理层
        STATE[State Backend]
        MEMORY_STATE[Memory]
        FS_STATE[FileSystem]
        ROCKS_STATE[RocksDB]
    end
    
    subgraph 部署层
        STANDALONE[Standalone<br/>独立部署]
        YARN[YARN<br/>资源调度]
        K8S[Kubernetes<br/>容器编排]
        MESOS[Mesos<br/>资源调度]
    end
    
    APP -->|提交作业| DISPATCHER
    DISPATCHER --> JOBMASTER
    JOBMASTER --> RM
    JOBMASTER --> CHECKPOINT
    
    RM -->|申请资源| YARN
    RM -->|申请资源| K8S
    
    RM -->|分配Slot| TM1
    RM -->|分配Slot| TM2
    RM -->|分配Slot| TM3
    
    JOBMASTER -->|部署Task| SLOT1
    JOBMASTER -->|部署Task| SLOT2
    
    SLOT1 --> NETWORK
    SLOT2 --> NETWORK
    
    SLOT1 -.->|读写状态| STATE
    SLOT2 -.->|读写状态| STATE
    
    STATE --> MEMORY_STATE
    STATE --> FS_STATE
    STATE --> ROCKS_STATE
    
    CHECKPOINT -.->|触发检查点| TM1
    CHECKPOINT -.->|触发检查点| TM2
```

## Flink 核心组件说明

### 1. JobManager（主节点）
- **Dispatcher**: 接收客户端提交的作业
- **ResourceManager**: 管理TaskManager资源和Slot分配
- **JobMaster**: 每个作业一个，负责作业执行协调
- **CheckpointCoordinator**: 协调分布式快照

### 2. TaskManager（工作节点）
- **Task Slot**: 任务执行槽位，隔离CPU和内存
- **Network Buffers**: 任务间数据交换缓冲区
- **Managed Memory**: 排序、哈希表等操作的托管内存

### 3. State Backend（状态后端）
- **MemoryStateBackend**: 内存存储（开发测试）
- **FsStateBackend**: 文件系统存储（HDFS/S3）
- **RocksDBStateBackend**: RocksDB存储（大状态场景）

### 4. 部署模式
- **Standalone**: 独立集群部署
- **YARN**: Hadoop资源调度
- **Kubernetes**: 容器化部署
- **Mesos**: 资源调度框架

## Flink 数据流处理架构

```mermaid
graph TB
    subgraph 数据源层
        KAFKA[Kafka]
        DB[Database CDC]
        FILE[File System]
    end
    
    subgraph Flink处理层
        SOURCE[Source Operator<br/>数据接入]
        
        MAP[Map Operator<br/>数据转换]
        FILTER[Filter Operator<br/>数据过滤]
        FLATMAP[FlatMap Operator<br/>数据展开]
        
        KEYBY[KeyBy<br/>数据分组]
        
        WINDOW[Window Operator<br/>窗口聚合]
        
        PROCESS[Process Function<br/>自定义处理]
        
        SINK[Sink Operator<br/>结果输出]
    end
    
    subgraph 状态管理
        KEYED_STATE[Keyed State<br/>键控状态]
        OPERATOR_STATE[Operator State<br/>算子状态]
    end
    
    subgraph 输出层
        KAFKA_OUT[Kafka]
        DB_OUT[Database]
        REDIS_OUT[Redis]
        ES_OUT[Elasticsearch]
    end
    
    KAFKA --> SOURCE
    DB --> SOURCE
    FILE --> SOURCE
    
    SOURCE --> MAP
    MAP --> FILTER
    FILTER --> FLATMAP
    
    FLATMAP --> KEYBY
    KEYBY --> WINDOW
    WINDOW --> PROCESS
    
    PROCESS --> SINK
    
    KEYBY -.->|读写| KEYED_STATE
    WINDOW -.->|读写| KEYED_STATE
    PROCESS -.->|读写| KEYED_STATE
    
    SOURCE -.->|读写| OPERATOR_STATE
    SINK -.->|读写| OPERATOR_STATE
    
    SINK --> KAFKA_OUT
    SINK --> DB_OUT
    SINK --> REDIS_OUT
    SINK --> ES_OUT
```

## Flink Checkpoint 机制

```mermaid
sequenceDiagram
    participant JM as JobManager
    participant TM1 as TaskManager 1
    participant TM2 as TaskManager 2
    participant STATE as State Backend
    participant STORAGE as 持久化存储
    
    JM->>JM: 1.触发Checkpoint
    Note over JM: 定时触发<br/>间隔如10秒
    
    JM->>TM1: 2.发送Barrier<br/>checkpoint-123
    JM->>TM2: 2.发送Barrier<br/>checkpoint-123
    
    TM1->>TM1: 3.对齐Barrier
    Note over TM1: 等待所有输入流<br/>Barrier到达
    
    TM1->>STATE: 4.快照状态
    Note over STATE: 保存算子状态
    
    STATE->>STORAGE: 5.持久化
    Note over STORAGE: HDFS/S3
    
    TM1->>JM: 6.确认完成
    
    TM2->>TM2: 3.对齐Barrier
    TM2->>STATE: 4.快照状态
    STATE->>STORAGE: 5.持久化
    TM2->>JM: 6.确认完成
    
    JM->>JM: 7.Checkpoint完成
    Note over JM: 所有TaskManager<br/>都确认完成
```

## Checkpoint 机制说明

### Barrier对齐
- Source算子接收到Checkpoint触发信号后插入Barrier
- Barrier随数据流向下游传播
- 算子等待所有输入流的Barrier都到达后才执行快照

### 状态快照
- 保存Keyed State（键控状态）
- 保存Operator State（算子状态）
- 异步写入持久化存储

### 故障恢复
- 从最近成功的Checkpoint恢复
- 重放Checkpoint之后的数据
- 保证Exactly-Once语义
