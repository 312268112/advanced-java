# B2B CDP 核心架构设计方案

## 目录
- [系统架构总览](#系统架构总览)
- [核心实体设计](#核心实体设计)
- [关键业务流程](#关键业务流程)
- [技术实现方案](#技术实现方案)

---

## 系统架构总览

### 整体架构图

```mermaid
graph TB
    subgraph 数据源层
        CRM[CRM系统]
        WEWORK[企业微信]
        WECHAT[微信公众号]
        INTERNAL[内部系统]
    end
    
    subgraph 数据接入层
        GATEWAY[API网关]
        ADAPTER1[CRM适配器]
        ADAPTER2[企微适配器]
        ADAPTER3[公众号适配器]
    end
    
    subgraph 消息队列
        KAFKA[Kafka消息队列]
    end
    
    subgraph 数据处理层
        CLEAN[数据清洗服务]
        MAPPING[身份映射服务]
        MERGE[数据合并服务]
        TAG[标签引擎]
        SEGMENT[分群引擎]
    end
    
    subgraph 数据存储层
        PG[(PostgreSQL)]
        REDIS[(Redis缓存)]
        ES[(Elasticsearch)]
    end
    
    subgraph 应用层
        API[业务API]
        QUERY[查询服务]
    end
    
    CRM --> ADAPTER1
    WEWORK --> ADAPTER2
    WECHAT --> ADAPTER3
    INTERNAL --> GATEWAY
    
    ADAPTER1 --> GATEWAY
    ADAPTER2 --> GATEWAY
    ADAPTER3 --> GATEWAY
    
    GATEWAY --> KAFKA
    KAFKA --> CLEAN
    CLEAN --> MAPPING
    MAPPING --> MERGE
    MERGE --> PG
    
    PG --> TAG
    PG --> SEGMENT
    TAG --> PG
    SEGMENT --> PG
    
    PG --> REDIS
    PG --> ES
    
    API --> QUERY
    QUERY --> REDIS
    QUERY --> PG
    QUERY --> ES
```

### 核心流程说明

**六大核心流程：**
1. **数据采集**：多渠道数据统一接入
2. **数据清洗**：格式标准化、数据验证
3. **身份映射**：识别不同渠道的同一主体
4. **全渠道整合**：合并为统一的实体
5. **标签打标**：根据规则和行为打标签
6. **客户圈人**：基于条件创建客户分群

---

## 核心实体设计

### 1. Account账户实体模型

```mermaid
erDiagram
    Account {
        varchar account_id PK
        varchar account_name
        varchar unified_credit_code UK
        varchar account_status
        varchar province
        varchar city
        datetime created_at
        datetime updated_at
    }
    
    AccountSummary {
        varchar summary_id PK
        varchar account_id FK
        int total_contacts
        int total_opportunities
        decimal total_revenue
        int health_score
        datetime calculated_at
    }
    
    AccountChannelIdentity {
        varchar identity_id PK
        varchar account_id FK
        varchar channel_id FK
        varchar channel_account_id
        boolean is_verified
        datetime first_seen_at
    }
    
    Account ||--|| AccountSummary : 汇总
    Account ||--o{ AccountChannelIdentity : 多渠道身份
```

**表关系说明：**
- Account（1） : AccountSummary（1）- 一对一汇总关系
- Account（1） : AccountChannelIdentity（N）- 一个企业在多个渠道有多个身份

---

### 2. Contact联系人实体模型

```mermaid
erDiagram
    Contact {
        varchar contact_id PK
        varchar contact_name
        varchar mobile_phone UK
        varchar email UK
        varchar primary_account_id FK
        varchar contact_status
        datetime created_at
    }
    
    ContactSummary {
        varchar summary_id PK
        varchar contact_id FK
        int total_interactions
        int engagement_score
        datetime last_activity_at
        datetime calculated_at
    }
    
    ContactChannelIdentity {
        varchar identity_id PK
        varchar contact_id FK
        varchar channel_id FK
        varchar channel_user_id
        boolean is_verified
        datetime first_seen_at
    }
    
    AccountContactRelation {
        varchar relation_id PK
        varchar account_id FK
        varchar contact_id FK
        varchar role_in_account
        boolean is_primary
    }
    
    Contact ||--|| ContactSummary : 汇总
    Contact ||--o{ ContactChannelIdentity : 多渠道身份
    Contact ||--o{ AccountContactRelation : 企业关系
```

**表关系说明：**
- Contact（1） : ContactSummary（1）- 一对一汇总关系
- Contact（1） : ContactChannelIdentity（N）- 一个联系人在多个渠道有多个身份
- Contact（N） : Account（M）- 通过AccountContactRelation关联

---

### 3. Lead线索实体模型

```mermaid
erDiagram
    Lead {
        varchar lead_id PK
        varchar lead_name
        varchar company_name
        varchar mobile_phone
        varchar email
        varchar channel_id FK
        varchar lead_status
        int lead_score
        datetime created_at
        datetime converted_at
        varchar converted_contact_id FK
    }
    
    LeadSummary {
        varchar summary_id PK
        varchar lead_id FK
        int form_submissions
        int days_in_pipeline
        datetime last_activity_at
        datetime calculated_at
    }
    
    LeadChannelIdentity {
        varchar identity_id PK
        varchar lead_id FK
        varchar channel_id FK
        varchar channel_user_id
        datetime captured_at
    }
    
    Lead ||--|| LeadSummary : 汇总
    Lead ||--o{ LeadChannelIdentity : 多渠道身份
```

**表关系说明：**
- Lead（1） : LeadSummary（1）- 一对一汇总关系
- Lead（1） : LeadChannelIdentity（N）- 一个线索可能来自多个渠道
- Lead在转化时通过converted_contact_id关联到Contact

---

### 4. 标签系统实体模型

```mermaid
erDiagram
    Tag {
        varchar tag_id PK
        varchar tag_name UK
        varchar tag_category
        varchar tag_type
        varchar description
        datetime created_at
    }
    
    TagRelation {
        varchar relation_id PK
        varchar tag_id FK
        varchar entity_type
        varchar entity_id FK
        datetime tagged_at
        boolean is_auto
    }
    
    TagRule {
        varchar rule_id PK
        varchar tag_id FK
        varchar rule_type
        text rule_definition
        boolean is_active
        datetime created_at
    }
    
    Tag ||--o{ TagRelation : 应用于
    Tag ||--o{ TagRule : 规则
```

**表关系说明：**
- Tag（1） : TagRelation（N）- 一个标签可以打给多个实体
- Tag（1） : TagRule（N）- 一个标签可以有多个自动打标规则
- TagRelation通过entity_type和entity_id关联到Account/Contact/Lead

---

### 5. 分群系统实体模型

```mermaid
erDiagram
    Segment {
        varchar segment_id PK
        varchar segment_name
        varchar target_entity_type
        text segment_rules
        int member_count
        boolean is_dynamic
        datetime last_calculated_at
        datetime created_at
    }
    
    SegmentMember {
        varchar member_id PK
        varchar segment_id FK
        varchar entity_type
        varchar entity_id FK
        datetime joined_at
        boolean is_active
    }
    
    Segment ||--o{ SegmentMember : 包含
```

**表关系说明：**
- Segment（1） : SegmentMember（N）- 一个分群包含多个成员
- SegmentMember通过entity_type和entity_id关联到Account/Contact/Lead

---

### 6. 渠道实体模型

```mermaid
erDiagram
    Channel {
        varchar channel_id PK
        varchar channel_name
        varchar channel_type
        varchar channel_status
        datetime created_at
    }
    
    Campaign {
        varchar campaign_id PK
        varchar campaign_name
        varchar channel_id FK
        date start_date
        date end_date
        datetime created_at
    }
    
    Channel ||--o{ Campaign : 发起
```

**表关系说明：**
- Channel（1） : Campaign（N）- 一个渠道可以发起多个营销活动

---

## 关键业务流程

### 1. 数据采集与清洗流程

```mermaid
sequenceDiagram
    participant 数据源 as 数据源<br/>CRM/企微/公众号
    participant 适配器 as 渠道适配器
    participant 网关 as API网关
    participant Kafka as Kafka队列
    participant 清洗 as 数据清洗服务
    participant 数据库 as PostgreSQL
    
    数据源->>适配器: 1.推送原始数据
    Note over 适配器: 数据格式转换<br/>统一字段映射
    
    适配器->>网关: 2.发送标准格式数据
    Note over 网关: 数据验证<br/>签名校验<br/>限流控制
    
    网关->>Kafka: 3.写入消息队列<br/>快速响应
    网关-->>数据源: 4.返回接收成功
    
    Kafka->>清洗: 5.消费消息
    Note over 清洗: 数据清洗处理
    
    清洗->>清洗: 6.格式标准化
    Note over 清洗: - 手机号格式统一<br/>- 企业名称清洗<br/>- 地址标准化<br/>- 必填字段验证
    
    清洗->>清洗: 7.数据去重
    Note over 清洗: - 同批次去重<br/>- 24小时内去重
    
    清洗->>清洗: 8.数据验证
    Note over 清洗: - 手机号格式<br/>- 邮箱格式<br/>- 身份证格式<br/>- 企业信用代码
    
    alt 验证通过
        清洗->>数据库: 9a.写入原始数据表<br/>保留完整记录
        清洗->>Kafka: 9b.发送到身份映射队列
        Note over 清洗: 继续下一步处理
    else 验证失败
        清洗->>数据库: 9c.写入异常数据表<br/>人工处理
        Note over 清洗: 记录失败原因
    end
```

**关键点：**
- 适配器层：各渠道数据统一格式
- 网关层：快速响应，写入队列后立即返回
- 清洗层：格式标准化、去重、验证
- 异步处理：不阻塞数据采集主流程

---

### 2. 身份映射与全渠道整合流程

```mermaid
sequenceDiagram
    participant Kafka as Kafka队列
    participant 映射 as 身份映射服务
    participant 数据库 as PostgreSQL
    participant Redis as Redis缓存
    participant 合并 as 数据合并服务
    
    Kafka->>映射: 1.消费清洗后数据
    
    映射->>映射: 2.提取身份标识符
    Note over 映射: - 手机号<br/>- 邮箱<br/>- 企业信用代码<br/>- 企业名称
    
    映射->>Redis: 3.查询缓存<br/>是否已有映射
    
    alt 缓存命中
        Redis-->>映射: 4a.返回已有实体ID
        映射->>合并: 4b.直接进入合并流程
    else 缓存未命中
        Redis-->>映射: 4c.缓存MISS
        
        映射->>数据库: 5.按优先级查询匹配
        Note over 数据库: Contact查询优先级：<br/>1. 手机号精确匹配<br/>2. 邮箱精确匹配<br/>3. 企微UserID匹配
        
        数据库-->>映射: 6.返回查询结果
        
        alt 找到唯一匹配
            映射->>映射: 7a.确认为同一实体
            映射->>Redis: 7b.写入映射缓存
            映射->>合并: 7c.进入合并流程
        else 找到多个匹配
            映射->>数据库: 7d.写入待审核队列
            映射->>映射: 7e.触发人工审核流程
            Note over 映射: 疑似重复<br/>需人工确认
        else 未找到匹配
            映射->>数据库: 7f.创建新实体
            映射->>数据库: 7g.创建ChannelIdentity
            映射->>Redis: 7h.写入映射缓存
            Note over 映射: 新客户首次进入
        end
    end
    
    合并->>数据库: 8.读取现有数据
    合并->>合并: 9.数据合并策略
    Note over 合并: - 非空字段优先保留<br/>- 最新数据优先<br/>- 重要字段不覆盖
    
    合并->>数据库: 10.更新实体数据
    合并->>数据库: 11.新增ChannelIdentity记录
    Note over 数据库: 记录本次渠道来源
    
    合并->>Redis: 12.更新实体缓存
    合并->>Kafka: 13.发送更新事件<br/>触发标签和汇总
```

**关键点：**
- 缓存优先：减少数据库查询压力
- 匹配优先级：手机号 > 邮箱 > 企微ID
- 疑似重复：人工审核避免误合并
- 数据合并策略：保护重要字段不被覆盖

---

### 3. 标签打标流程

```mermaid
sequenceDiagram
    participant Kafka as Kafka队列
    participant 标签引擎 as 标签引擎
    participant 规则库 as 规则库
    participant 数据库 as PostgreSQL
    participant Redis as Redis缓存
    
    Kafka->>标签引擎: 1.消费实体更新事件
    Note over Kafka: 触发条件：<br/>- 实体创建<br/>- 实体更新<br/>- 定时批量
    
    标签引擎->>规则库: 2.加载标签规则
    规则库-->>标签引擎: 3.返回活跃规则列表
    
    loop 遍历每个标签规则
        标签引擎->>标签引擎: 4.评估规则条件
        Note over 标签引擎: 规则类型：<br/>- 属性规则<br/>- 行为规则<br/>- 统计规则<br/>- 时间规则
        
        alt 规则条件满足
            标签引擎->>数据库: 5a.查询是否已有该标签
            
            alt 标签不存在
                标签引擎->>数据库: 6a.新增标签关系
                Note over 数据库: INSERT TagRelation
            else 标签已存在
                标签引擎->>标签引擎: 6b.跳过（幂等性）
            end
        else 规则条件不满足
            标签引擎->>数据库: 5b.查询是否已有该标签
            
            alt 标签存在
                标签引擎->>数据库: 6c.删除标签关系
                Note over 数据库: DELETE TagRelation<br/>标签自动移除
            end
        end
    end
    
    标签引擎->>Redis: 7.更新标签缓存
    Note over Redis: 缓存实体的所有标签<br/>用于快速查询
    
    标签引擎->>数据库: 8.记录打标日志
    Note over 数据库: 审计追溯
```

**标签规则示例：**

```
属性规则：
- 行业 = "互联网" → 打标签"互联网行业"
- 年营收 > 1亿 → 打标签"大型企业"
- 省份 = "浙江" → 打标签"浙江客户"

行为规则：
- 30天内访问次数 > 10 → 打标签"高活跃"
- 下载过白皮书 → 打标签"内容营销线索"
- 参加过线下活动 → 打标签"线下活动客户"

统计规则：
- 商机总数 > 5 → 打标签"重点跟进客户"
- 成交金额 > 100万 → 打标签"高价值客户"
- 流失天数 > 180 → 打标签"流失预警"

时间规则：
- 注册时间 < 7天 → 打标签"新客户"
- 最后活跃 < 30天 → 打标签"活跃客户"
```

---

### 4. 客户圈人（分群）流程

```mermaid
sequenceDiagram
    participant 用户 as 营销人员
    participant 前端 as 分群界面
    participant 分群引擎 as 分群引擎
    participant 数据库 as PostgreSQL
    participant Redis as Redis缓存
    participant Kafka as Kafka队列
    
    用户->>前端: 1.创建分群<br/>设置筛选条件
    Note over 前端: 条件示例：<br/>- 行业=互联网<br/>- 省份=浙江<br/>- 有标签"高活跃"<br/>- 商机数>3
    
    前端->>分群引擎: 2.提交分群规则
    
    分群引擎->>分群引擎: 3.解析规则<br/>生成SQL查询
    Note over 分群引擎: 规则解析器<br/>转换为SQL WHERE条件
    
    分群引擎->>数据库: 4.执行分群查询
    Note over 数据库: 复杂查询示例：<br/>SELECT account_id<br/>FROM Account a<br/>JOIN AccountSummary s<br/>JOIN TagRelation t<br/>WHERE 条件...
    
    数据库-->>分群引擎: 5.返回符合条件的实体ID列表
    
    分群引擎->>分群引擎: 6.统计分群人数
    
    alt 动态分群
        分群引擎->>数据库: 7a.创建分群记录<br/>is_dynamic=true
        Note over 数据库: 不保存成员<br/>每次实时计算
    else 静态分群
        分群引擎->>数据库: 7b.创建分群记录<br/>is_dynamic=false
        
        loop 批量插入成员
            分群引擎->>数据库: 7c.插入SegmentMember<br/>批量1000条/次
        end
        
        Note over 数据库: 保存成员快照<br/>不随条件变化
    end
    
    分群引擎->>Redis: 8.缓存分群结果
    Note over Redis: 缓存成员ID列表<br/>提升查询性能
    
    分群引擎-->>前端: 9.返回分群结果
    前端-->>用户: 10.展示分群统计
    Note over 用户: - 分群人数<br/>- 分群规则<br/>- 最后更新时间
    
    opt 营销活动使用
        用户->>前端: 11.选择分群<br/>执行营销动作
        前端->>Kafka: 12.发送营销任务
        Note over Kafka: - 群发邮件<br/>- 推送消息<br/>- 分配线索
    end
```

**动态分群 vs 静态分群：**

| 对比项 | 动态分群 | 静态分群 |
|--------|---------|---------|
| 成员存储 | 不存储，实时计算 | 存储快照 |
| 成员变化 | 自动更新 | 固定不变 |
| 查询性能 | 较慢（每次计算） | 快速（直接查表） |
| 存储占用 | 小 | 大 |
| 适用场景 | 持续性营销 | 一次性活动 |
| 示例 | "活跃客户"群 | "双11参与客户"群 |

---

### 5. 汇总计算流程

```mermaid
sequenceDiagram
    participant Kafka as Kafka队列
    participant 汇总服务 as 汇总计算服务
    participant 分布式锁 as Redis分布式锁
    participant 数据库 as PostgreSQL
    participant Redis as Redis缓存
    
    Kafka->>汇总服务: 1.消费汇总触发事件
    Note over Kafka: 触发场景：<br/>- 新增Contact<br/>- 商机状态变化<br/>- 定时全量刷新
    
    汇总服务->>分布式锁: 2.尝试获取锁<br/>key: summary:ACC001
    
    alt 获取锁失败
        分布式锁-->>汇总服务: 3a.锁被占用
        汇总服务->>汇总服务: 3b.跳过本次计算
        Note over 汇总服务: 防止重复计算
    else 获取锁成功
        分布式锁-->>汇总服务: 3c.锁获取成功<br/>超时30秒
        
        par 并行查询统计数据
            汇总服务->>数据库: 4a.统计Contact数
            汇总服务->>数据库: 4b.统计Opportunity数据
            汇总服务->>数据库: 4c.统计Lead数
            汇总服务->>数据库: 4d.查询最后活跃时间
        end
        
        数据库-->>汇总服务: 5.返回所有统计结果
        
        汇总服务->>汇总服务: 6.计算汇总指标
        Note over 汇总服务: 计算：<br/>- total_contacts<br/>- total_revenue<br/>- win_rate<br/>- health_score
        
        汇总服务->>数据库: 7.更新Summary表<br/>记录calculated_at
        
        汇总服务->>Redis: 8.更新汇总缓存
        Note over Redis: 缓存TTL: 300秒
        
        汇总服务->>分布式锁: 9.释放锁
    end
```

**汇总计算优化策略：**

```
增量计算：
- 只计算变化部分
- 减少全表扫描
- 提升计算速度

并行查询：
- 统计数据并行执行
- 减少总耗时
- 提升吞吐量

分布式锁：
- 防止重复计算
- 保证数据一致性
- 避免资源浪费

定时 + 实时：
- 定时任务：每小时全量刷新
- 实时触发：关键事件立即计算
- 保证数据新鲜度
```

---

## 技术实现方案

### 1. 数据库表结构设计

#### Account核心表

```sql
-- Account基础表
CREATE TABLE account (
    account_id VARCHAR(64) PRIMARY KEY COMMENT '账户ID',
    account_name VARCHAR(200) NOT NULL COMMENT '企业名称',
    unified_social_credit_code VARCHAR(18) UNIQUE COMMENT '统一信用代码',
    account_type VARCHAR(50) NOT NULL COMMENT '客户类型',
    account_status VARCHAR(50) NOT NULL COMMENT '账户状态',
    industry_id VARCHAR(64) COMMENT '行业ID',
    province VARCHAR(50) COMMENT '省份',
    city VARCHAR(50) COMMENT '城市',
    account_source VARCHAR(100) COMMENT '来源',
    owner_user_id VARCHAR(64) COMMENT '负责人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (account_name),
    INDEX idx_status (account_status),
    INDEX idx_city (province, city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业账户表';

-- AccountSummary汇总表
CREATE TABLE account_summary (
    summary_id VARCHAR(64) PRIMARY KEY COMMENT '汇总ID',
    account_id VARCHAR(64) NOT NULL UNIQUE COMMENT '账户ID',
    total_contacts INT DEFAULT 0 COMMENT '联系人总数',
    total_opportunities INT DEFAULT 0 COMMENT '商机总数',
    total_leads INT DEFAULT 0 COMMENT '线索总数',
    total_revenue DECIMAL(18,2) DEFAULT 0 COMMENT '累计收入',
    won_opportunities INT DEFAULT 0 COMMENT '赢单数',
    lost_opportunities INT DEFAULT 0 COMMENT '输单数',
    win_rate DECIMAL(5,2) DEFAULT 0 COMMENT '赢单率',
    health_score INT DEFAULT 0 COMMENT '健康度评分',
    last_activity_at DATETIME COMMENT '最后活跃时间',
    calculated_at DATETIME NOT NULL COMMENT '计算时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES account(account_id),
    INDEX idx_health_score (health_score DESC),
    INDEX idx_revenue (total_revenue DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业账户汇总表';

-- AccountChannelIdentity渠道身份映射表
CREATE TABLE account_channel_identity (
    identity_id VARCHAR(64) PRIMARY KEY COMMENT '身份ID',
    account_id VARCHAR(64) NOT NULL COMMENT '账户ID',
    channel_id VARCHAR(64) NOT NULL COMMENT '渠道ID',
    channel_account_id VARCHAR(200) NOT NULL COMMENT '渠道内账户ID',
    identity_type VARCHAR(50) COMMENT '身份类型',
    is_verified BOOLEAN DEFAULT FALSE COMMENT '是否已验证',
    first_seen_at DATETIME NOT NULL COMMENT '首次发现时间',
    last_seen_at DATETIME NOT NULL COMMENT '最后活跃时间',
    UNIQUE KEY uk_account_channel (account_id, channel_id),
    INDEX idx_channel_account (channel_id, channel_account_id),
    FOREIGN KEY (account_id) REFERENCES account(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业渠道身份映射表';
```

#### Contact核心表

```sql
-- Contact基础表
CREATE TABLE contact (
    contact_id VARCHAR(64) PRIMARY KEY COMMENT '联系人ID',
    contact_name VARCHAR(100) NOT NULL COMMENT '姓名',
    mobile_phone VARCHAR(20) UNIQUE COMMENT '手机号',
    email VARCHAR(200) UNIQUE COMMENT '邮箱',
    wechat_id VARCHAR(100) COMMENT '微信ID',
    job_title VARCHAR(100) COMMENT '职位',
    contact_status VARCHAR(50) NOT NULL COMMENT '状态',
    primary_account_id VARCHAR(64) COMMENT '主要企业ID',
    contact_source VARCHAR(100) COMMENT '来源',
    owner_user_id VARCHAR(64) COMMENT '负责人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (mobile_phone),
    INDEX idx_email (email),
    INDEX idx_account (primary_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人表';

-- ContactSummary汇总表
CREATE TABLE contact_summary (
    summary_id VARCHAR(64) PRIMARY KEY COMMENT '汇总ID',
    contact_id VARCHAR(64) NOT NULL UNIQUE COMMENT '联系人ID',
    total_interactions INT DEFAULT 0 COMMENT '互动总数',
    email_opens INT DEFAULT 0 COMMENT '邮件打开数',
    email_clicks INT DEFAULT 0 COMMENT '邮件点击数',
    engagement_score INT DEFAULT 0 COMMENT '参与度评分',
    last_activity_at DATETIME COMMENT '最后活跃时间',
    calculated_at DATETIME NOT NULL COMMENT '计算时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contact_id) REFERENCES contact(contact_id),
    INDEX idx_engagement (engagement_score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人汇总表';

-- ContactChannelIdentity渠道身份映射表
CREATE TABLE contact_channel_identity (
    identity_id VARCHAR(64) PRIMARY KEY COMMENT '身份ID',
    contact_id VARCHAR(64) NOT NULL COMMENT '联系人ID',
    channel_id VARCHAR(64) NOT NULL COMMENT '渠道ID',
    channel_user_id VARCHAR(200) NOT NULL COMMENT '渠道用户ID',
    identity_type VARCHAR(50) COMMENT '身份类型',
    is_verified BOOLEAN DEFAULT FALSE COMMENT '是否已验证',
    first_seen_at DATETIME NOT NULL COMMENT '首次发现时间',
    last_seen_at DATETIME NOT NULL COMMENT '最后活跃时间',
    UNIQUE KEY uk_contact_channel (contact_id, channel_id),
    INDEX idx_channel_user (channel_id, channel_user_id),
    FOREIGN KEY (contact_id) REFERENCES contact(contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人渠道身份映射表';
```

#### 标签系统表

```sql
-- Tag标签表
CREATE TABLE tag (
    tag_id VARCHAR(64) PRIMARY KEY COMMENT '标签ID',
    tag_name VARCHAR(100) NOT NULL UNIQUE COMMENT '标签名称',
    tag_category VARCHAR(50) COMMENT '标签分类',
    tag_type VARCHAR(50) COMMENT '标签类型',
    description TEXT COMMENT '描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (tag_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- TagRelation标签关系表
CREATE TABLE tag_relation (
    relation_id VARCHAR(64) PRIMARY KEY COMMENT '关系ID',
    tag_id VARCHAR(64) NOT NULL COMMENT '标签ID',
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型',
    entity_id VARCHAR(64) NOT NULL COMMENT '实体ID',
    tagged_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打标时间',
    is_auto BOOLEAN DEFAULT FALSE COMMENT '是否自动打标',
    UNIQUE KEY uk_tag_entity (tag_id, entity_type, entity_id),
    INDEX idx_entity (entity_type, entity_id),
    FOREIGN KEY (tag_id) REFERENCES tag(tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签关系表';

-- TagRule标签规则表
CREATE TABLE tag_rule (
    rule_id VARCHAR(64) PRIMARY KEY COMMENT '规则ID',
    tag_id VARCHAR(64) NOT NULL COMMENT '标签ID',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型',
    rule_definition TEXT NOT NULL COMMENT '规则定义',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tag_id) REFERENCES tag(tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签规则表';
```

#### 分群系统表

```sql
-- Segment分群表
CREATE TABLE segment (
    segment_id VARCHAR(64) PRIMARY KEY COMMENT '分群ID',
    segment_name VARCHAR(200) NOT NULL COMMENT '分群名称',
    target_entity_type VARCHAR(50) NOT NULL COMMENT '目标实体类型',
    segment_rules TEXT NOT NULL COMMENT '分群规则',
    member_count INT DEFAULT 0 COMMENT '成员数量',
    is_dynamic BOOLEAN DEFAULT FALSE COMMENT '是否动态分群',
    last_calculated_at DATETIME COMMENT '最后计算时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entity_type (target_entity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分群表';

-- SegmentMember分群成员表（仅静态分群使用）
CREATE TABLE segment_member (
    member_id VARCHAR(64) PRIMARY KEY COMMENT '成员ID',
    segment_id VARCHAR(64) NOT NULL COMMENT '分群ID',
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型',
    entity_id VARCHAR(64) NOT NULL COMMENT '实体ID',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活跃',
    UNIQUE KEY uk_segment_entity (segment_id, entity_type, entity_id),
    INDEX idx_entity (entity_type, entity_id),
    FOREIGN KEY (segment_id) REFERENCES segment(segment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分群成员表';
```

---

### 2. Kafka Topic设计

```yaml
Topic设计:

1. data-ingestion (数据采集)
   Partition: 8
   Replication: 3
   Retention: 7天
   用途: 接收多渠道原始数据

2. data-cleaning (数据清洗)
   Partition: 8
   Replication: 3
   Retention: 3天
   用途: 清洗后的标准数据

3. identity-mapping (身份映射)
   Partition: 8
   Replication: 3
   Retention: 3天
   用途: 身份识别和映射任务

4. entity-merge (实体合并)
   Partition: 8
   Replication: 3
   Retention: 3天
   用途: 实体数据合并任务

5. tag-calculation (标签计算)
   Partition: 8
   Replication: 3
   Retention: 3天
   用途: 标签打标任务

6. summary-calculation (汇总计算)
   Partition: 8
   Replication: 3
   Retention: 3天
   用途: 汇总统计任务
```

---

### 3. 缓存策略设计

```yaml
Redis缓存设计:

1. 实体基础数据缓存
   Key: entity:{type}:{id}
   TTL: 300秒
   示例: entity:account:ACC001
   用途: 缓存Account/Contact基础信息

2. 汇总数据缓存
   Key: summary:{type}:{id}
   TTL: 300秒
   示例: summary:account:ACC001
   用途: 缓存Summary汇总数据

3. 标签缓存
   Key: tags:{type}:{id}
   TTL: 600秒
   示例: tags:account:ACC001
   Value: ["高价值客户", "互联网行业", "浙江"]
   用途: 缓存实体的所有标签

4. 分群成员缓存
   Key: segment:members:{segment_id}
   TTL: 1800秒
   Value: Set类型，存储entity_id
   用途: 缓存分群成员列表

5. 身份映射缓存
   Key: identity:{channel}:{channel_user_id}
   TTL: 3600秒
   示例: identity:wechat:openid_xxx
   Value: contact_id
   用途: 快速查询渠道身份对应的实体ID

6. 分布式锁
   Key: lock:summary:{entity_id}
   TTL: 30秒
   用途: 防止汇总计算重复执行
```

---

### 4. 核心服务接口设计

```yaml
数据采集服务:
  POST /api/ingestion/crm
    - 接收CRM数据
  POST /api/ingestion/wework
    - 接收企业微信数据
  POST /api/ingestion/wechat
    - 接收公众号数据

查询服务:
  GET /api/account/{id}
    - 查询Account详情
  GET /api/account/{id}/360
    - 查询Account 360度视图
  GET /api/contact/{id}
    - 查询Contact详情
  GET /api/contact/search
    - 搜索Contact

标签服务:
  GET /api/tag/list
    - 获取标签列表
  POST /api/tag/create
    - 创建标签
  POST /api/tag/apply
    - 手动打标签
  GET /api/tag/entity/{type}/{id}
    - 查询实体的所有标签

分群服务:
  POST /api/segment/create
    - 创建分群
  GET /api/segment/{id}/members
    - 获取分群成员
  POST /api/segment/calculate
    - 重新计算分群
  GET /api/segment/list
    - 获取分群列表
```

---

## 总结

### 核心能力

**1. 多渠道数据采集**
- 统一接入层，支持CRM、企微、公众号等多渠道
- 快速响应，异步处理
- 数据清洗和标准化

**2. 全渠道身份整合**
- 基于手机号、邮箱、企业信用代码等多维度匹配
- ChannelIdentity记录每个渠道身份
- 疑似重复人工审核

**3. 智能标签系统**
- 支持自动打标和手动打标
- 规则引擎驱动
- 标签分类管理

**4. 灵活分群能力**
- 动态分群：实时计算
- 静态分群：快照固化
- 支持复杂条件组合

**5. 实时汇总统计**
- 异步计算，不阻塞主流程
- 分布式锁防重
- 缓存优化查询性能

### 技术特点

```
简洁清晰：
- 专注核心业务流程
- 去除复杂行为数据
- 架构易于理解

高性能：
- 消息队列异步解耦
- 多级缓存策略
- 数据库索引优化

高可用：
- PostgreSQL主从复制
- Redis Cluster
- Kafka集群部署

可扩展：
- 微服务架构
- 水平扩展能力
- 插件化设计
```

### 适用场景

- 企业客户数：100万-1000万
- 联系人数：500万-5000万
- 线索数：200万-2000万/年
- 数据采集QPS：1000-10000
- 查询QPS：5000-50000
