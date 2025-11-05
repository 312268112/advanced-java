# B2B CDP 生产级系统架构设计

## 目录
- [系统整体架构](#系统整体架构)
- [核心实体设计](#核心实体设计)
- [关键时序流程](#关键时序流程)
- [数据流转架构](#数据流转架构)
- [技术实现方案](#技术实现方案)

---

## 系统整体架构

### 整体架构分层图

```mermaid
graph TB
    subgraph DataSource["数据源层 Data Source"]
        CRM[CRM系统<br/>Salesforce/SAP]
        WEWORK[企业微信<br/>API/Webhook]
        WECHAT[微信公众号<br/>表单提交]
        INTERNAL[内部系统<br/>ERP/OA]
    end
    
    subgraph Ingestion["数据接入层 Ingestion Gateway"]
        API[API Gateway<br/>统一接入]
        ADAPTER1[CRM适配器]
        ADAPTER2[企微适配器]
        ADAPTER3[公众号适配器]
    end
    
    subgraph MQ["消息队列层 Message Queue"]
        KAFKA[Kafka Cluster<br/>3 Broker]
        T1[data-ingestion]
        T2[identity-resolution]
        T3[summary-calculation]
        T4[notification]
    end
    
    subgraph Processing["数据处理层 Processing Engine"]
        CLEANSING[数据清洗服务]
        IDENTITY[身份识别引擎]
        SCORING[评分引擎]
        TAGGING[标签引擎]
        SUMMARY[汇总计算服务]
    end
    
    subgraph Storage["数据存储层 Storage"]
        PG[(PostgreSQL集群<br/>主从分库分表)]
        CH[(ClickHouse集群<br/>行为数据)]
        REDIS[(Redis Cluster<br/>缓存)]
        ES[(Elasticsearch<br/>搜索)]
    end
    
    subgraph Application["应用服务层 Application"]
        LEAD_SVC[Lead服务]
        CONTACT_SVC[Contact服务]
        ACCOUNT_SVC[Account服务]
        OPP_SVC[Opportunity服务]
        QUERY_SVC[查询服务]
    end
    
    subgraph Client["客户端 Client"]
        WEB[Web Portal]
        MOBILE[Mobile App]
        OPENAPI[Open API]
    end
    
    CRM --> API
    WEWORK --> API
    WECHAT --> API
    INTERNAL --> API
    
    API --> ADAPTER1
    API --> ADAPTER2
    API --> ADAPTER3
    
    ADAPTER1 --> KAFKA
    ADAPTER2 --> KAFKA
    ADAPTER3 --> KAFKA
    
    KAFKA --> T1
    KAFKA --> T2
    KAFKA --> T3
    KAFKA --> T4
    
    T1 --> CLEANSING
    CLEANSING --> IDENTITY
    T2 --> IDENTITY
    IDENTITY --> LEAD_SVC
    IDENTITY --> CONTACT_SVC
    
    T3 --> SUMMARY
    T4 --> SUMMARY
    
    LEAD_SVC --> PG
    CONTACT_SVC --> PG
    ACCOUNT_SVC --> PG
    OPP_SVC --> PG
    
    PG --> REDIS
    PG --> ES
    CH --> REDIS
    
    QUERY_SVC --> REDIS
    QUERY_SVC --> PG
    QUERY_SVC --> CH
    QUERY_SVC --> ES
    
    WEB --> Application
    MOBILE --> Application
    OPENAPI --> Application
```

---

### 技术架构详图

```mermaid
graph TB
    subgraph HA["高可用架构 High Availability"]
        direction TB
        LB[负载均衡<br/>Nginx/ALB]
        
        subgraph AppCluster["应用集群"]
            APP1[应用节点1]
            APP2[应用节点2]
            APP3[应用节点3]
        end
        
        subgraph DBCluster["数据库集群"]
            PG_MASTER[PostgreSQL主库]
            PG_SLAVE1[PostgreSQL从库1]
            PG_SLAVE2[PostgreSQL从库2]
        end
        
        subgraph CacheCluster["缓存集群"]
            REDIS_M1[Redis Master1]
            REDIS_S1[Redis Slave1]
            REDIS_M2[Redis Master2]
            REDIS_S2[Redis Slave2]
        end
        
        subgraph CHCluster["ClickHouse集群"]
            CH1[CH节点1<br/>Shard1 Replica1]
            CH2[CH节点2<br/>Shard1 Replica2]
            CH3[CH节点3<br/>Shard2 Replica1]
        end
        
        LB --> APP1
        LB --> APP2
        LB --> APP3
        
        APP1 --> PG_MASTER
        APP2 --> PG_SLAVE1
        APP3 --> PG_SLAVE2
        
        PG_MASTER --> PG_SLAVE1
        PG_MASTER --> PG_SLAVE2
        
        APP1 --> REDIS_M1
        APP2 --> REDIS_M2
        
        REDIS_M1 --> REDIS_S1
        REDIS_M2 --> REDIS_S2
    end
```

---

## 核心实体设计

### Account实体ER图（完整版）

```mermaid
erDiagram
    Account ||--|| AccountSummary : aggregates
    Account ||--o{ AccountChannelIdentity : has
    Account ||--o{ Contact : owns
    Account ||--o{ Opportunity : has
    
    Account {
        varchar account_id PK "分片键"
        varchar account_name "企业名称"
        varchar unified_social_credit_code UK "信用代码"
        varchar account_type "类型"
        varchar account_status "状态"
        int shard_id "分片ID"
        datetime created_at "创建时间"
        datetime updated_at "更新时间"
    }
    
    AccountSummary {
        varchar summary_id PK
        varchar account_id FK UK "关联账户"
        int total_contacts "联系人数"
        int total_opportunities "商机数"
        decimal total_revenue "累计收入"
        int health_score "健康度"
        datetime calculated_at "计算时间"
    }
    
    AccountChannelIdentity {
        varchar identity_id PK
        varchar account_id FK "关联账户"
        varchar channel_id FK "渠道"
        varchar channel_account_id "渠道ID"
        boolean is_verified "已验证"
        datetime first_seen_at "首次发现"
        datetime last_seen_at "最后活跃"
    }
    
    Contact {
        varchar contact_id PK "分片键"
        varchar contact_name "姓名"
        varchar mobile_phone UK "手机号"
        varchar email UK "邮箱"
        varchar primary_account_id FK "主账户"
        int shard_id "分片ID"
        datetime created_at "创建时间"
    }
    
    Opportunity {
        varchar opportunity_id PK
        varchar account_id FK "关联账户"
        decimal amount "金额"
        varchar stage "阶段"
        boolean is_won "已赢单"
        datetime created_at "创建时间"
    }
```

---

### Contact实体ER图（完整版）

```mermaid
erDiagram
    Contact ||--|| ContactSummary : aggregates
    Contact ||--o{ ContactChannelIdentity : has
    Contact ||--o{ AccountContactRelation : belongs_to
    Contact ||--o{ Touchpoint : receives
    
    Contact {
        varchar contact_id PK "分片键"
        varchar contact_name "姓名"
        varchar mobile_phone UK "手机号"
        varchar email UK "邮箱"
        varchar wechat_id "微信ID"
        varchar primary_account_id FK "主账户"
        int shard_id "分片ID-hash(mobile_phone)"
        datetime created_at "创建时间"
    }
    
    ContactSummary {
        varchar summary_id PK
        varchar contact_id FK UK "关联联系人"
        int total_touchpoints "触点数"
        int email_opens "邮件打开"
        int engagement_score "参与度"
        datetime last_activity_at "最后活跃"
        datetime calculated_at "计算时间"
    }
    
    ContactChannelIdentity {
        varchar identity_id PK
        varchar contact_id FK "关联联系人"
        varchar channel_id FK "渠道"
        varchar channel_user_id "渠道用户ID"
        varchar identity_type "身份类型"
        boolean is_verified "已验证"
        datetime first_seen_at "首次发现"
    }
    
    AccountContactRelation {
        varchar relation_id PK
        varchar account_id FK "企业"
        varchar contact_id FK "联系人"
        varchar role_in_account "角色"
        varchar decision_level "决策级别"
        boolean is_primary_contact "主联系人"
    }
    
    Touchpoint {
        varchar touchpoint_id PK
        varchar contact_id FK "联系人"
        varchar channel_id FK "渠道"
        datetime touchpoint_time "触点时间"
        varchar touchpoint_type "类型"
    }
```

---

### Lead实体ER图（完整版）

```mermaid
erDiagram
    Lead ||--|| LeadSummary : aggregates
    Lead ||--o{ LeadChannelIdentity : has
    Lead ||--o| Contact : converts_to
    Lead ||--o| Opportunity : converts_to
    
    Lead {
        varchar lead_id PK "分片键"
        varchar lead_name "姓名"
        varchar company_name "公司"
        varchar mobile_phone "手机号"
        varchar email "邮箱"
        varchar channel_id FK "来源渠道"
        varchar campaign_id FK "来源活动"
        varchar lead_status "状态"
        int lead_score "评分"
        varchar lead_grade "等级"
        int shard_id "分片ID"
        datetime created_at "创建时间"
        datetime converted_at "转化时间"
        varchar converted_contact_id FK "转化联系人"
    }
    
    LeadSummary {
        varchar summary_id PK
        varchar lead_id FK UK "关联线索"
        int total_touchpoints "触点数"
        int total_events "事件数"
        int form_submissions "表单提交"
        int days_in_pipeline "管道天数"
        datetime last_activity_at "最后活跃"
        datetime calculated_at "计算时间"
    }
    
    LeadChannelIdentity {
        varchar identity_id PK
        varchar lead_id FK "关联线索"
        varchar channel_id FK "渠道"
        varchar channel_user_id "渠道用户ID"
        datetime captured_at "捕获时间"
        varchar utm_source "UTM来源"
        varchar utm_campaign "UTM活动"
    }
```

---

## 关键时序流程

### 1. 多渠道数据采集与身份识别流程

```mermaid
sequenceDiagram
    participant Source as 数据源<br/>CRM/企微/公众号
    participant Gateway as API Gateway<br/>数据接入网关
    participant Kafka as Kafka<br/>data-ingestion
    participant Cleansing as 数据清洗服务
    participant Kafka2 as Kafka<br/>identity-resolution
    participant Identity as 身份识别引擎
    participant PG as PostgreSQL<br/>主库
    participant Redis as Redis<br/>缓存
    participant ES as Elasticsearch<br/>搜索引擎
    participant Kafka3 as Kafka<br/>summary-calculation
    participant Summary as 汇总计算服务
    
    Source->>Gateway: 1.推送数据<br/>Webhook/API
    Note over Gateway: 数据格式转换<br/>统一数据模型
    Gateway->>Kafka: 2.写入消息队列<br/>Topic: data-ingestion
    Note over Kafka: 数据缓冲<br/>峰值削峰
    
    Kafka->>Cleansing: 3.消费消息
    Note over Cleansing: 数据清洗<br/>- 格式规范化<br/>- 必填字段验证<br/>- 数据去重
    
    Cleansing->>Kafka2: 4.发送到身份识别队列<br/>Topic: identity-resolution
    
    Kafka2->>Identity: 5.消费身份识别消息
    
    Identity->>PG: 6.查询是否存在<br/>按手机号/邮箱/企业名称
    PG-->>Identity: 7.返回查询结果
    
    alt 找到唯一匹配
        Identity->>PG: 8a.更新现有实体<br/>合并数据
        Identity->>PG: 8b.新增ChannelIdentity<br/>记录渠道身份
        Note over Identity: 更新模式<br/>保留历史数据
    else 未找到匹配
        Identity->>PG: 8c.创建新实体<br/>Lead/Contact/Account
        Identity->>PG: 8d.创建ChannelIdentity<br/>初始渠道身份
        Note over Identity: 创建模式<br/>生成全局唯一ID
    else 找到多个匹配（疑似重复）
        Identity->>PG: 8e.写入待审核队列<br/>人工确认
        Note over Identity: 风控模式<br/>防止误合并
    end
    
    Identity->>Redis: 9.更新缓存<br/>写入最新数据
    Identity->>ES: 10.更新搜索索引<br/>异步同步
    
    Identity->>Kafka3: 11.触发汇总计算<br/>Topic: summary-calculation
    Note over Identity: 异步触发<br/>不阻塞主流程
    
    Kafka3->>Summary: 12.消费汇总消息
    Summary->>PG: 13.查询关联数据<br/>聚合统计
    Summary->>PG: 14.更新Summary表<br/>写入汇总结果
    Summary->>Redis: 15.更新汇总缓存
    
    Gateway-->>Source: 16.返回响应<br/>同步快速响应
    Note over Gateway: 响应时间 < 100ms<br/>不等待后续处理
```

**关键设计说明：**

1. **同步响应快速返回**：API Gateway在写入Kafka后立即返回，响应时间 < 100ms
2. **异步处理解耦**：身份识别、汇总计算都是异步处理，互不影响
3. **消息队列削峰**：Kafka缓冲高峰流量，保护下游服务
4. **多级缓存**：Redis缓存热点数据，减少数据库压力
5. **最终一致性**：汇总数据允许30秒内延迟，保证高可用

---

### 2. Lead转化为Contact/Opportunity流程

```mermaid
sequenceDiagram
    participant Sales as 销售人员
    participant App as 应用服务
    participant LeadSvc as Lead服务
    participant ContactSvc as Contact服务
    participant AccountSvc as Account服务
    participant OppSvc as Opportunity服务
    participant PG as PostgreSQL
    participant Kafka as Kafka
    participant Summary as 汇总计算服务
    participant Notification as 通知服务
    
    Sales->>App: 1.确认Lead转化<br/>lead_id + 转化信息
    App->>LeadSvc: 2.调用Lead转化接口
    
    LeadSvc->>PG: 3.查询Lead详情<br/>验证状态
    PG-->>LeadSvc: 4.返回Lead数据
    
    alt Lead已转化
        LeadSvc-->>App: 5a.返回错误<br/>Lead已转化
    else Lead可转化
        LeadSvc->>PG: 5b.开启事务<br/>BEGIN TRANSACTION
        
        LeadSvc->>ContactSvc: 6.查询是否存在Contact<br/>按手机号/邮箱
        ContactSvc->>PG: 7.查询Contact表
        PG-->>ContactSvc: 8.返回查询结果
        
        alt Contact不存在
            ContactSvc->>PG: 9a.创建新Contact<br/>INSERT Contact
            ContactSvc->>PG: 9b.创建ContactChannelIdentity
            Note over ContactSvc: 新建Contact<br/>继承Lead的渠道信息
        else Contact已存在
            ContactSvc->>PG: 9c.更新Contact<br/>UPDATE Contact
            ContactSvc->>PG: 9d.补充ContactChannelIdentity
            Note over ContactSvc: 更新Contact<br/>合并Lead数据
        end
        
        ContactSvc->>AccountSvc: 10.关联或创建Account
        AccountSvc->>PG: 11.查询Account<br/>按企业名称/信用代码
        PG-->>AccountSvc: 12.返回Account
        
        alt Account不存在
            AccountSvc->>PG: 13a.创建Account
            AccountSvc->>PG: 13b.创建AccountChannelIdentity
        else Account存在
            AccountSvc->>PG: 13c.关联Contact到Account<br/>AccountContactRelation
        end
        
        LeadSvc->>OppSvc: 14.创建Opportunity<br/>可选步骤
        OppSvc->>PG: 15.创建Opportunity记录
        
        LeadSvc->>PG: 16.更新Lead状态<br/>lead_status=CONVERTED
        LeadSvc->>PG: 17.记录转化关系<br/>converted_contact_id等
        
        LeadSvc->>PG: 18.提交事务<br/>COMMIT
        
        LeadSvc->>Kafka: 19.发送转化事件<br/>Topic: summary-calculation
        Note over Kafka: 触发汇总重算<br/>- LeadSummary<br/>- ContactSummary<br/>- AccountSummary
        
        Kafka->>Summary: 20.消费转化事件
        Summary->>PG: 21.重新计算汇总数据<br/>批量更新Summary表
        
        LeadSvc->>Kafka: 22.发送通知事件<br/>Topic: notification
        Kafka->>Notification: 23.消费通知事件
        Notification->>Sales: 24.发送通知<br/>邮件/站内信/企微
        
        LeadSvc-->>App: 25.返回转化结果<br/>contact_id, account_id
        App-->>Sales: 26.展示转化成功
    end
```

**关键设计说明：**

1. **事务保证一致性**：Lead转化涉及多表操作，使用数据库事务保证原子性
2. **幂等性设计**：检查Lead状态，防止重复转化
3. **异步汇总计算**：转化完成后，异步触发汇总重算，不阻塞主流程
4. **通知解耦**：通过消息队列发送通知，失败可重试

---

### 3. 实时汇总数据计算流程

```mermaid
sequenceDiagram
    participant Trigger as 触发源<br/>业务事件/定时任务
    participant Kafka as Kafka<br/>summary-calculation
    participant Summary as 汇总计算服务
    participant PG as PostgreSQL
    participant Redis as Redis
    participant Lock as 分布式锁<br/>Redis
    
    Trigger->>Kafka: 1.发送汇总任务<br/>account_id/contact_id
    Note over Trigger: 触发场景：<br/>- 新增Contact<br/>- 商机赢单<br/>- 新增Touchpoint<br/>- 定时全量刷新
    
    Kafka->>Summary: 2.消费汇总消息<br/>并发消费
    
    Summary->>Lock: 3.尝试获取分布式锁<br/>key: summary:account:ACC001
    Note over Lock: 防止并发重复计算<br/>锁超时时间: 30秒
    
    alt 获取锁失败
        Lock-->>Summary: 4a.锁已被占用
        Summary->>Summary: 4b.跳过本次计算<br/>等待下次触发
    else 获取锁成功
        Lock-->>Summary: 4c.锁获取成功
        
        Summary->>PG: 5.查询Account基础信息
        PG-->>Summary: 6.返回Account数据
        
        par 并行查询统计数据
            Summary->>PG: 7a.统计Contact数量<br/>SELECT COUNT FROM Contact
            Summary->>PG: 7b.统计Opportunity数据<br/>SELECT SUM/COUNT FROM Opportunity
            Summary->>PG: 7c.统计Lead数量<br/>SELECT COUNT FROM Lead
            Summary->>PG: 7d.统计Touchpoint数据<br/>SELECT COUNT FROM Touchpoint
            Summary->>PG: 7e.查询最后活跃时间<br/>SELECT MAX FROM Touchpoint
        end
        
        PG-->>Summary: 8.返回所有统计结果
        
        Summary->>Summary: 9.计算汇总指标<br/>- total_contacts<br/>- total_revenue<br/>- win_rate<br/>- health_score等
        Note over Summary: 计算逻辑：<br/>win_rate = won / (won + lost)<br/>health_score = f(last_activity)
        
        Summary->>PG: 10.写入AccountSummary表<br/>INSERT ON DUPLICATE KEY UPDATE
        Note over Summary: Upsert操作<br/>记录calculated_at时间戳
        
        Summary->>Redis: 11.更新缓存<br/>SET account:summary:ACC001
        Note over Redis: 缓存TTL: 300秒<br/>下次查询直接从缓存读取
        
        Summary->>Lock: 12.释放分布式锁
        Lock-->>Summary: 13.锁释放成功
        
        Summary->>Kafka: 14.发送计算完成事件<br/>可选通知下游
    end
```

**关键设计说明：**

1. **分布式锁防重**：使用Redis分布式锁，防止同一Account并发重复计算
2. **并行查询优化**：统计数据可以并行查询，提升性能
3. **Upsert操作**：使用`INSERT ON DUPLICATE KEY UPDATE`，支持增量更新
4. **缓存策略**：计算完成后立即更新缓存，保证数据新鲜度
5. **异步非阻塞**：整个计算过程异步进行，不影响主业务

---

### 4. 客户360度查询流程（高性能）

```mermaid
sequenceDiagram
    participant User as 用户
    participant App as 应用服务
    participant Cache as Redis<br/>L1缓存
    participant Query as 查询服务
    participant PG as PostgreSQL<br/>主数据
    participant CH as ClickHouse<br/>行为数据
    participant ES as Elasticsearch<br/>搜索引擎
    
    User->>App: 1.查询客户360度视图<br/>account_id: ACC001
    App->>Cache: 2.查询L1缓存<br/>GET account:full:ACC001
    
    alt 缓存命中
        Cache-->>App: 3a.返回完整数据<br/>响应时间 < 10ms
        App-->>User: 4a.返回结果
        Note over App: 缓存命中率 > 90%<br/>热点数据实时响应
    else 缓存未命中
        Cache-->>App: 3b.缓存MISS
        
        par 并行查询多数据源
            App->>Query: 4b.查询基础信息
            Query->>PG: 5a.查询Account基础表
            Query->>PG: 5b.查询AccountSummary汇总表
            Query->>PG: 5c.查询Contact列表<br/>LIMIT 10
            Query->>PG: 5d.查询Opportunity列表<br/>LIMIT 10
            
            App->>Query: 4c.查询行为数据
            Query->>CH: 6a.查询最近30天Event<br/>GROUP BY event_type
            Query->>CH: 6b.查询最近Touchpoint<br/>ORDER BY time DESC LIMIT 20
            
            App->>Query: 4d.查询多渠道身份
            Query->>PG: 7.查询AccountChannelIdentity
        end
        
        PG-->>Query: 8.返回主数据
        CH-->>Query: 9.返回行为数据
        
        Query->>Query: 10.数据聚合<br/>组装360度视图
        Note over Query: 组装数据结构：<br/>- 基础信息<br/>- 汇总数据<br/>- 关联实体<br/>- 行为统计<br/>- 多渠道身份
        
        Query-->>App: 11.返回完整数据
        
        App->>Cache: 12.写入缓存<br/>SET account:full:ACC001<br/>TTL: 300秒
        Note over Cache: 缓存策略：<br/>- 热点数据长期缓存<br/>- 冷数据短期缓存<br/>- 更新时主动失效
        
        App-->>User: 13.返回结果<br/>响应时间 < 200ms
    end
```

**关键设计说明：**

1. **多级缓存**：Redis L1缓存 + 应用内存缓存（可选），缓存命中率 > 90%
2. **并行查询**：同时查询PostgreSQL、ClickHouse、ES，减少总响应时间
3. **查询优化**：
   - Contact/Opportunity只查前10条，避免大结果集
   - ClickHouse查询带时间范围，利用分区裁剪
4. **缓存失效策略**：
   - 写操作主动失效缓存（Cache Aside模式）
   - 设置合理的TTL（5分钟）
5. **降级策略**：
   - Redis宕机 → 直接查数据库
   - ClickHouse超时 → 降级返回部分数据

---

### 5. Event事件采集流程（高吞吐）

```mermaid
sequenceDiagram
    participant User as 用户行为
    participant SDK as 埋点SDK<br/>Web/Mobile
    participant Gateway as API Gateway
    participant Kafka as Kafka<br/>event-stream
    participant Flink as Flink<br/>实时计算
    participant CH as ClickHouse<br/>行为数据库
    participant Redis as Redis<br/>实时统计
    
    User->>SDK: 1.触发行为事件<br/>页面访问/按钮点击
    SDK->>SDK: 2.批量打包<br/>100条/批或1秒/批
    Note over SDK: 客户端批量上报<br/>减少网络请求
    
    SDK->>Gateway: 3.批量上报事件<br/>HTTP POST
    Note over Gateway: 接收批量事件<br/>QPS: 10000
    
    Gateway->>Gateway: 4.快速验证<br/>签名/限流/格式
    
    Gateway->>Kafka: 5.写入Kafka<br/>Topic: event-stream<br/>Partition: hash(user_id)
    Note over Kafka: 分区策略：<br/>按user_id哈希<br/>保证同一用户有序
    
    Gateway-->>SDK: 6.快速响应<br/>响应时间 < 50ms
    
    Kafka->>Flink: 7.Flink消费事件流<br/>并行度: 16
    
    Flink->>Flink: 8.实时计算<br/>- Session会话识别<br/>- 漏斗分析<br/>- 实时统计
    Note over Flink: 窗口计算：<br/>5秒tumbling window
    
    Flink->>Redis: 9.写入实时指标<br/>PV/UV/转化率等
    Note over Redis: 实时大屏展示<br/>秒级更新
    
    Flink->>CH: 10.批量写入ClickHouse<br/>10000条/批
    Note over CH: 批量插入优化<br/>减少写入次数
    
    par 异步关联分析
        Flink->>Flink: 11a.用户画像更新
        Flink->>Flink: 11b.行为标签计算
        Flink->>Kafka: 11c.触发下游任务
    end
```

**关键设计说明：**

1. **客户端批量上报**：SDK端批量打包，减少HTTP请求数
2. **API快速响应**：写入Kafka后立即返回，响应时间 < 50ms
3. **Kafka分区策略**：按user_id哈希分区，保证同一用户事件有序
4. **Flink实时计算**：
   - Session会话识别
   - 滑动窗口统计
   - 实时漏斗分析
5. **批量写入ClickHouse**：累积10000条或10秒批量插入，优化写入性能

---

## 数据流转架构

### 数据同步与流转总览

```mermaid
graph LR
    subgraph Sources["数据源"]
        CRM[CRM]
        WEWORK[企微]
        WECHAT[公众号]
    end
    
    subgraph Ingestion["数据接入"]
        API[API Gateway]
    end
    
    subgraph MQ["消息队列"]
        K1[data-ingestion]
        K2[identity-resolution]
        K3[summary-calculation]
    end
    
    subgraph Processing["数据处理"]
        CLEAN[数据清洗]
        IDENTITY[身份识别]
        SUMMARY[汇总计算]
    end
    
    subgraph Storage["数据存储"]
        PG[(PostgreSQL)]
        CH[(ClickHouse)]
        REDIS[(Redis)]
        ES[(Elasticsearch)]
    end
    
    subgraph Sync["数据同步"]
        CANAL[Canal CDC]
        SYNC[数据同步]
    end
    
    CRM --> API
    WEWORK --> API
    WECHAT --> API
    
    API --> K1
    K1 --> CLEAN
    CLEAN --> K2
    K2 --> IDENTITY
    IDENTITY --> PG
    IDENTITY --> K3
    K3 --> SUMMARY
    SUMMARY --> PG
    
    PG --> CANAL
    CANAL --> ES
    CANAL --> REDIS
    
    PG --> SYNC
    SYNC --> CH
```

---

### PostgreSQL分库分表策略

```mermaid
graph TB
    subgraph App["应用层"]
        APP[应用服务]
    end
    
    subgraph Proxy["分库分表中间件 ShardingSphere"]
        SHARDING[分片路由<br/>按account_id/contact_id哈希]
    end
    
    subgraph DB0["数据库0 shard_0"]
        PG0_M[(PostgreSQL<br/>Master)]
        PG0_S1[(PostgreSQL<br/>Slave1)]
        PG0_S2[(PostgreSQL<br/>Slave2)]
    end
    
    subgraph DB1["数据库1 shard_1"]
        PG1_M[(PostgreSQL<br/>Master)]
        PG1_S1[(PostgreSQL<br/>Slave1)]
    end
    
    subgraph DB15["数据库15 shard_15"]
        PG15_M[(PostgreSQL<br/>Master)]
        PG15_S1[(PostgreSQL<br/>Slave1)]
    end
    
    APP --> SHARDING
    
    SHARDING -->|写操作| PG0_M
    SHARDING -->|写操作| PG1_M
    SHARDING -->|写操作| PG15_M
    
    SHARDING -->|读操作| PG0_S1
    SHARDING -->|读操作| PG0_S2
    SHARDING -->|读操作| PG1_S1
    SHARDING -->|读操作| PG15_S1
    
    PG0_M -.主从复制.-> PG0_S1
    PG0_M -.主从复制.-> PG0_S2
    PG1_M -.主从复制.-> PG1_S1
    PG15_M -.主从复制.-> PG15_S1
```

**分片策略：**

```sql
-- Account表分片键：account_id
-- 分片规则：hash(account_id) % 16
-- 分片数：16个库

-- Contact表分片键：contact_id（由mobile_phone生成）
-- 分片规则：hash(mobile_phone) % 16
-- 保证同一手机号的Contact在同一分片

-- 关联查询优化：
-- AccountContactRelation表按account_id分片
-- 保证Account与其Contact在同一分片，避免跨库JOIN
```

---

## 技术实现方案

### 1. 身份识别引擎实现

**身份匹配规则（优先级从高到低）：**

```python
class IdentityResolutionEngine:
    """身份识别引擎"""
    
    def resolve_account(self, data: dict) -> str:
        """Account身份识别"""
        # 1. 优先级1：统一社会信用代码精确匹配
        if data.get('unified_social_credit_code'):
            account = self.find_by_credit_code(data['unified_social_credit_code'])
            if account:
                return account.account_id
        
        # 2. 优先级2：企业全称精确匹配
        if data.get('account_name'):
            account = self.find_by_exact_name(data['account_name'])
            if account:
                return account.account_id
        
        # 3. 优先级3：企业简称+城市模糊匹配
        if data.get('account_name') and data.get('city'):
            candidates = self.fuzzy_match_by_name_and_city(
                data['account_name'], 
                data['city']
            )
            if len(candidates) == 1:
                return candidates[0].account_id
            elif len(candidates) > 1:
                # 多个匹配，进入人工审核
                self.send_to_manual_review(data, candidates)
                return None
        
        # 4. 未找到匹配，创建新Account
        return self.create_new_account(data)
    
    def resolve_contact(self, data: dict) -> str:
        """Contact身份识别"""
        # 1. 优先级1：手机号精确匹配
        if data.get('mobile_phone'):
            contact = self.find_by_phone(data['mobile_phone'])
            if contact:
                return contact.contact_id
        
        # 2. 优先级2：邮箱精确匹配
        if data.get('email'):
            contact = self.find_by_email(data['email'])
            if contact:
                return contact.contact_id
        
        # 3. 优先级3：企业微信UserID
        if data.get('wework_user_id'):
            contact = self.find_by_wework_id(data['wework_user_id'])
            if contact:
                return contact.contact_id
        
        # 4. 未找到匹配，创建新Contact
        return self.create_new_contact(data)
```

---

### 2. 汇总计算优化方案

**增量计算 vs 全量计算：**

```python
class SummaryCalculationService:
    """汇总计算服务"""
    
    def calculate_account_summary(self, account_id: str, mode: str = 'incremental'):
        """计算Account汇总数据"""
        if mode == 'incremental':
            # 增量计算：只计算变化的部分
            return self._incremental_calculate(account_id)
        else:
            # 全量计算：重新统计所有数据
            return self._full_calculate(account_id)
    
    def _incremental_calculate(self, account_id: str):
        """增量计算（推荐）"""
        # 1. 获取上次计算结果
        last_summary = self.get_last_summary(account_id)
        
        # 2. 只统计增量数据（自上次计算后的新增数据）
        last_calc_time = last_summary.calculated_at
        
        # 3. 统计增量
        new_contacts = self.count_new_contacts(account_id, since=last_calc_time)
        new_opportunities = self.count_new_opportunities(account_id, since=last_calc_time)
        new_revenue = self.sum_new_revenue(account_id, since=last_calc_time)
        
        # 4. 累加到原有数据
        new_summary = AccountSummary(
            account_id=account_id,
            total_contacts=last_summary.total_contacts + new_contacts,
            total_opportunities=last_summary.total_opportunities + new_opportunities,
            total_revenue=last_summary.total_revenue + new_revenue,
            # ... 其他字段
            calculated_at=datetime.now()
        )
        
        return new_summary
    
    def _full_calculate(self, account_id: str):
        """全量计算（定时任务使用）"""
        # 重新统计所有数据
        return self._calculate_from_scratch(account_id)
```

---

### 3. 缓存策略实现

**多级缓存架构：**

```python
class CacheManager:
    """缓存管理器"""
    
    def get_account_full(self, account_id: str) -> dict:
        """获取Account完整数据（多级缓存）"""
        # L1: 本地内存缓存（可选，使用LRU）
        # data = self.local_cache.get(f'account:{account_id}')
        # if data:
        #     return data
        
        # L2: Redis缓存
        cache_key = f'account:full:{account_id}'
        cached = self.redis.get(cache_key)
        if cached:
            return json.loads(cached)
        
        # L3: 数据库查询
        account = self.db.query_account_with_summary(account_id)
        
        # 写入缓存
        self.redis.setex(
            cache_key, 
            300,  # TTL: 5分钟
            json.dumps(account)
        )
        
        return account
    
    def invalidate_account_cache(self, account_id: str):
        """使Account缓存失效"""
        # 删除所有相关缓存
        self.redis.delete(f'account:full:{account_id}')
        self.redis.delete(f'account:basic:{account_id}')
        self.redis.delete(f'account:summary:{account_id}')
```

**缓存更新策略（Cache Aside模式）：**

```python
# 写操作：先更新数据库，再删除缓存
def update_account(account_id: str, data: dict):
    # 1. 更新数据库
    db.update(account_id, data)
    
    # 2. 删除缓存（让下次读取时重新加载）
    cache.delete(f'account:full:{account_id}')

# 读操作：先查缓存，缓存miss则查数据库并写缓存
def get_account(account_id: str):
    # 1. 查缓存
    data = cache.get(f'account:full:{account_id}')
    if data:
        return data
    
    # 2. 查数据库
    data = db.query(account_id)
    
    # 3. 写缓存
    cache.set(f'account:full:{account_id}', data, ttl=300)
    
    return data
```

---

### 4. 消息队列Topic设计

```yaml
Kafka Topic设计:

1. data-ingestion (数据采集)
   - Partition: 16个分区
   - Replication: 3副本
   - Retention: 7天
   - 用途: 接收所有渠道的原始数据
   
2. identity-resolution (身份识别)
   - Partition: 16个分区
   - Replication: 3副本
   - Retention: 3天
   - 用途: 身份识别任务队列
   
3. summary-calculation (汇总计算)
   - Partition: 16个分区
   - Replication: 3副本
   - Retention: 3天
   - 用途: 汇总计算任务队列
   
4. notification (通知事件)
   - Partition: 8个分区
   - Replication: 3副本
   - Retention: 7天
   - 用途: 通知任务（邮件、短信、企微）
   
5. event-stream (事件流)
   - Partition: 32个分区
   - Replication: 3副本
   - Retention: 30天
   - 用途: 用户行为事件流（高吞吐）
```

---

### 5. 监控与告警

**核心监控指标：**

```yaml
业务指标:
  - 数据采集QPS
  - 身份识别成功率
  - 汇总计算延迟
  - 查询P99响应时间
  - 缓存命中率

技术指标:
  - Kafka消息积压
  - PostgreSQL慢查询
  - Redis内存使用率
  - ClickHouse查询延迟
  - 服务健康检查

告警规则:
  - Kafka消息积压 > 100万 → P1告警
  - 数据采集失败率 > 1% → P2告警
  - 汇总计算延迟 > 5分钟 → P2告警
  - 查询P99 > 1秒 → P3告警
  - 服务不可用 → P0告警
```

---

## 总结

### 核心技术特点

1. **高可用架构**
   - PostgreSQL主从复制 + 读写分离
   - Redis Cluster 3主3从
   - ClickHouse 3副本
   - Kafka 3 broker集群

2. **高性能设计**
   - PostgreSQL分库分表（16分片）
   - 多级缓存（Redis + 本地）
   - 异步处理（Kafka解耦）
   - 批量写入优化

3. **实时性保障**
   - 数据采集响应 < 100ms
   - 身份识别延迟 < 5秒
   - 汇总计算延迟 < 30秒
   - 查询P99 < 200ms

4. **数据一致性**
   - 事务保证原子性
   - 消息队列保证可靠性
   - 分布式锁防重复
   - 最终一致性设计

5. **扩展性**
   - 水平扩展（加节点）
   - 垂直扩展（加资源）
   - 服务解耦（微服务）
   - 存储分离（冷热分离）

### 容量评估

```
数据规模：
- Account: 1000万
- Contact: 5000万
- Lead: 2000万/年
- Event: 10亿/年
- Touchpoint: 5000万/年

存储容量：
- PostgreSQL: 2TB (SSD)
- ClickHouse: 10TB (SSD)
- Redis: 256GB (内存)

服务器配置：
- 应用服务器: 8核16G * 5台
- PostgreSQL: 16核64G * 16台（主） + 32台（从）
- ClickHouse: 32核128G * 3台
- Redis: 16核64G * 6台
- Kafka: 16核32G * 3台
```

### 后续优化方向

1. **短期优化（3个月内）**
   - 完善监控告警
   - 优化慢查询
   - 调整缓存策略
   - 压测验证性能

2. **中期优化（6个月内）**
   - 引入本地缓存（Caffeine）
   - 实现数据归档
   - 优化分库分表策略
   - 引入ES辅助查询

3. **长期优化（1年内）**
   - 冷热数据分离
   - 引入数据湖
   - AI/ML模型集成
   - 实时OLAP分析
