# B2B CDP 实体设计详细方案

## 📋 目录
- [整体架构](#整体架构)
- [核心实体设计](#核心实体设计)
- [实体关系图](#实体关系图)
- [业务流程](#业务流程)
- [数据模型详细设计](#数据模型详细设计)

---

## 整体架构

### 实体分层架构图

```mermaid
graph TB
    subgraph "数据源层"
        DS1[官网]
        DS2[微信生态]
        DS3[抖音]
        DS4[线下活动]
        DS5[电话/邮件]
        DS6[CRM系统]
        DS7[第三方平台]
    end
    
    subgraph "渠道层"
        Channel[Channel 渠道]
    end
    
    subgraph "交互层"
        Touchpoint[Touchpoint 触点]
        Event[Event 事件]
        Campaign[Campaign 营销活动]
    end
    
    subgraph "客户主体层"
        Contact[Contact 联系人]
        Lead[Lead 线索]
        Account[Account 企业账户]
    end
    
    subgraph "业务层"
        Opportunity[Opportunity 商机]
        Product[Product 产品]
        Order[Order 订单]
    end
    
    subgraph "洞察层"
        Segment[Segment 客户分群]
        Tag[Tag 标签]
        Score[Score 评分]
        Journey[Journey 客户旅程]
        Attribution[Attribution 归因]
    end
    
    DS1 & DS2 & DS3 & DS4 & DS5 & DS6 & DS7 --> Channel
    Channel --> Touchpoint
    Channel --> Event
    Campaign --> Touchpoint
    Touchpoint --> Contact
    Touchpoint --> Lead
    Event --> Contact
    Contact --> Account
    Lead --> Contact
    Lead --> Opportunity
    Account --> Opportunity
    Opportunity --> Product
    Opportunity --> Order
    Contact --> Segment
    Account --> Segment
    Lead --> Tag
    Contact --> Tag
    Account --> Tag
    Contact --> Score
    Account --> Score
    Lead --> Score
    Contact --> Journey
    Campaign --> Attribution
    Opportunity --> Attribution
```

---

## 核心实体设计

### 1. Account（企业账户）- 核心实体

```mermaid
classDiagram
    class Account {
        +String account_id PK
        +String account_name
        +String unified_social_credit_code
        +String account_type
        +String industry_id FK
        +String account_status
        +String account_level
        +Decimal annual_revenue
        +Integer employee_count
        +String company_website
        +String company_address
        +String province
        +String city
        +String district
        +String account_source
        +String primary_channel_id FK
        +String owner_user_id
        +DateTime created_at
        +DateTime updated_at
        +JSON custom_fields
        +String[] tags
        +Integer health_score
        +String lifecycle_stage
    }
    
    class AccountChannelIdentity {
        +String identity_id PK
        +String account_id FK
        +String channel_id FK
        +String channel_account_id
        +String identity_type
        +Boolean is_verified
        +DateTime first_seen_at
        +DateTime last_seen_at
        +JSON additional_info
    }
    
    class AccountRelation {
        +String relation_id PK
        +String parent_account_id FK
        +String child_account_id FK
        +String relation_type
        +DateTime created_at
    }
    
    Account "1" --> "*" AccountChannelIdentity
    Account "1" --> "*" AccountRelation : parent
    Account "1" --> "*" AccountRelation : child
```

**字段说明：**
- `account_id`: 全局唯一账户ID
- `unified_social_credit_code`: 统一社会信用代码（企业唯一标识）
- `account_type`: 客户类型（潜在客户/现有客户/合作伙伴/竞争对手）
- `account_status`: 账户状态（活跃/休眠/流失/黑名单）
- `account_level`: 客户等级（战略级/重要级/普通级）
- `lifecycle_stage`: 生命周期阶段（认知期/考虑期/决策期/留存期/扩展期）

---

### 2. Contact（联系人）- 核心实体

```mermaid
classDiagram
    class Contact {
        +String contact_id PK
        +String contact_name
        +String mobile_phone
        +String email
        +String wechat_id
        +String job_title
        +String department
        +String contact_status
        +String primary_account_id FK
        +String contact_source
        +String primary_channel_id FK
        +String owner_user_id
        +DateTime created_at
        +DateTime updated_at
        +JSON custom_fields
        +String[] tags
        +Integer engagement_score
        +String lifecycle_stage
        +Boolean is_decision_maker
        +Boolean is_verified
    }
    
    class ContactChannelIdentity {
        +String identity_id PK
        +String contact_id FK
        +String channel_id FK
        +String channel_user_id
        +String identity_type
        +Boolean is_primary
        +Boolean is_verified
        +DateTime first_seen_at
        +DateTime last_seen_at
        +JSON additional_info
    }
    
    class AccountContactRelation {
        +String relation_id PK
        +String account_id FK
        +String contact_id FK
        +String role_in_account
        +String decision_level
        +Boolean is_primary_contact
        +String relationship_status
        +DateTime relation_start_date
        +DateTime relation_end_date
        +DateTime created_at
        +DateTime updated_at
    }
    
    Contact "1" --> "*" ContactChannelIdentity
    Contact "1" --> "*" AccountContactRelation
```

**字段说明：**
- `decision_level`: 决策层级（决策者/影响者/使用者/把关者）
- `engagement_score`: 参与度评分（基于互动频率和深度）
- `role_in_account`: 在企业中的角色（CEO/CTO/采购经理等）

---

### 3. Lead（线索）- 核心实体

```mermaid
classDiagram
    class Lead {
        +String lead_id PK
        +String lead_name
        +String company_name
        +String mobile_phone
        +String email
        +String wechat_id
        +String job_title
        +String lead_source
        +String channel_id FK
        +String campaign_id FK
        +String lead_status
        +Integer lead_score
        +String lead_grade
        +String industry_id FK
        +String province
        +String city
        +String owner_user_id
        +DateTime created_at
        +DateTime updated_at
        +DateTime last_contacted_at
        +DateTime converted_at
        +String converted_contact_id FK
        +String converted_account_id FK
        +String converted_opportunity_id FK
        +JSON custom_fields
        +String[] tags
        +Boolean is_qualified
    }
    
    class LeadChannelIdentity {
        +String identity_id PK
        +String lead_id FK
        +String channel_id FK
        +String channel_user_id
        +DateTime captured_at
        +JSON utm_params
        +JSON additional_info
    }
    
    Lead "1" --> "*" LeadChannelIdentity
```

**字段说明：**
- `lead_status`: 线索状态（新建/联系中/已限定/已转化/无效）
- `lead_score`: 线索评分（基于行为和画像的综合评分）
- `lead_grade`: 线索等级（A/B/C/D）
- `is_qualified`: 是否为合格线索（MQL/SQL）

---

### 4. Opportunity（商机）- 核心实体

```mermaid
classDiagram
    class Opportunity {
        +String opportunity_id PK
        +String opportunity_name
        +String account_id FK
        +String primary_contact_id FK
        +String lead_id FK
        +String opportunity_type
        +String opportunity_source
        +Decimal amount
        +String currency
        +String stage
        +Integer probability
        +Date expected_close_date
        +Date actual_close_date
        +String close_reason
        +String owner_user_id
        +String[] product_ids FK
        +String campaign_id FK
        +DateTime created_at
        +DateTime updated_at
        +JSON custom_fields
        +String[] tags
        +Integer days_in_stage
        +Boolean is_won
        +Boolean is_lost
    }
    
    class OpportunityStageHistory {
        +String history_id PK
        +String opportunity_id FK
        +String from_stage
        +String to_stage
        +DateTime changed_at
        +String changed_by_user_id
        +String change_reason
        +Integer duration_days
    }
    
    class OpportunityProduct {
        +String opp_product_id PK
        +String opportunity_id FK
        +String product_id FK
        +Integer quantity
        +Decimal unit_price
        +Decimal total_price
        +Decimal discount
        +String product_description
    }
    
    Opportunity "1" --> "*" OpportunityStageHistory
    Opportunity "1" --> "*" OpportunityProduct
```

**字段说明：**
- `stage`: 阶段（线索/需求确认/方案设计/商务谈判/合同签订/已赢单/已输单）
- `probability`: 赢单概率（0-100）
- `opportunity_type`: 商机类型（新客户/追加销售/续约/交叉销售）

---

### 5. Channel（渠道）- 核心实体

```mermaid
classDiagram
    class Channel {
        +String channel_id PK
        +String channel_name
        +String channel_type
        +String channel_category
        +String parent_channel_id FK
        +String channel_status
        +JSON channel_config
        +Decimal cost
        +DateTime created_at
        +DateTime updated_at
        +JSON custom_fields
    }
    
    class ChannelPerformance {
        +String performance_id PK
        +String channel_id FK
        +Date stat_date
        +Integer lead_count
        +Integer contact_count
        +Integer opportunity_count
        +Decimal revenue
        +Decimal cost
        +Decimal roi
        +Integer conversion_count
        +Decimal conversion_rate
    }
    
    Channel "1" --> "*" ChannelPerformance
```

**渠道类型枚举：**
- 线上渠道：官网、SEO、SEM、社交媒体、内容营销
- 社交渠道：微信、企业微信、抖音、快手、小红书
- 线下渠道：展会、研讨会、地推活动
- 合作渠道：合作伙伴、代理商、分销商
- 直销渠道：电话、邮件、销售拜访

---

### 6. Campaign（营销活动）- 核心实体

```mermaid
classDiagram
    class Campaign {
        +String campaign_id PK
        +String campaign_name
        +String campaign_type
        +String campaign_status
        +String[] channel_ids FK
        +Date start_date
        +Date end_date
        +Decimal budget
        +Decimal actual_cost
        +String target_audience
        +String owner_user_id
        +DateTime created_at
        +DateTime updated_at
        +JSON custom_fields
        +String[] tags
    }
    
    class CampaignPerformance {
        +String performance_id PK
        +String campaign_id FK
        +Date stat_date
        +Integer impressions
        +Integer clicks
        +Integer leads_generated
        +Integer opportunities_generated
        +Decimal revenue
        +Decimal roi
        +Decimal cpl
        +Decimal cpa
    }
    
    class CampaignMember {
        +String member_id PK
        +String campaign_id FK
        +String member_type
        +String member_ref_id
        +String member_status
        +DateTime joined_at
        +DateTime responded_at
        +String response_status
        +JSON response_data
    }
    
    Campaign "1" --> "*" CampaignPerformance
    Campaign "1" --> "*" CampaignMember
```

**活动类型：**
- 网络研讨会（Webinar）
- 线下会议/展会
- 邮件营销
- 内容营销（白皮书、案例分享）
- 产品试用活动
- 行业峰会

---

### 7. Touchpoint（触点/互动记录）- 核心实体

```mermaid
classDiagram
    class Touchpoint {
        +String touchpoint_id PK
        +String touchpoint_type
        +String channel_id FK
        +String campaign_id FK
        +String contact_id FK
        +String lead_id FK
        +String account_id FK
        +DateTime touchpoint_time
        +String touchpoint_direction
        +String touchpoint_status
        +String content_type
        +String content_id FK
        +String subject
        +Text description
        +Integer duration_seconds
        +String owner_user_id
        +JSON metadata
        +JSON utm_params
        +DateTime created_at
    }
    
    class TouchpointAttachment {
        +String attachment_id PK
        +String touchpoint_id FK
        +String file_name
        +String file_url
        +String file_type
        +Integer file_size
        +DateTime uploaded_at
    }
    
    Touchpoint "1" --> "*" TouchpointAttachment
```

**触点类型：**
- 网站浏览
- 表单提交
- 内容下载
- 邮件互动（打开/点击）
- 电话沟通
- 会议/拜访
- 在线聊天
- 社交媒体互动

---

### 8. Event（行为事件）- 核心实体

```mermaid
classDiagram
    class Event {
        +String event_id PK
        +String event_name
        +String event_type
        +String channel_id FK
        +String contact_id FK
        +String lead_id FK
        +String account_id FK
        +DateTime event_time
        +String session_id
        +String device_type
        +String browser
        +String os
        +String ip_address
        +String page_url
        +String referrer_url
        +JSON event_properties
        +JSON utm_params
        +DateTime created_at
    }
```

**事件类型：**
- 页面浏览（page_view）
- 按钮点击（button_click）
- 表单开始（form_start）
- 表单提交（form_submit）
- 文件下载（file_download）
- 视频播放（video_play）
- 产品试用（product_trial）
- 搜索（search）

---

### 9. Product（产品/解决方案）- 核心实体

```mermaid
classDiagram
    class Product {
        +String product_id PK
        +String product_name
        +String product_code
        +String product_category
        +String product_type
        +String product_status
        +Text description
        +Decimal list_price
        +String currency
        +String pricing_model
        +String[] feature_list
        +DateTime created_at
        +DateTime updated_at
        +JSON custom_fields
    }
    
    class ProductCategory {
        +String category_id PK
        +String category_name
        +String parent_category_id FK
        +Integer sort_order
    }
    
    Product "*" --> "1" ProductCategory
```

---

### 10. Tag（标签）- 核心实体

```mermaid
classDiagram
    class Tag {
        +String tag_id PK
        +String tag_name
        +String tag_category
        +String tag_type
        +String description
        +String color
        +DateTime created_at
        +String created_by_user_id
    }
    
    class TagRelation {
        +String relation_id PK
        +String tag_id FK
        +String entity_type
        +String entity_id
        +DateTime tagged_at
        +String tagged_by_user_id
        +Boolean is_auto_tagged
        +String tag_source
    }
    
    Tag "1" --> "*" TagRelation
```

**标签类型：**
- 行为标签（高活跃度、近期浏览过产品A）
- 画像标签（互联网行业、大型企业、决策者）
- 业务标签（重点客户、流失风险、高价值）
- 兴趣标签（关注AI、关注云计算）

---

### 11. Segment（客户分群）- 核心实体

```mermaid
classDiagram
    class Segment {
        +String segment_id PK
        +String segment_name
        +String segment_type
        +Text description
        +JSON segment_rules
        +String target_entity_type
        +Integer member_count
        +Boolean is_dynamic
        +DateTime last_calculated_at
        +String created_by_user_id
        +DateTime created_at
        +DateTime updated_at
    }
    
    class SegmentMember {
        +String member_id PK
        +String segment_id FK
        +String entity_type
        +String entity_id
        +DateTime joined_at
        +DateTime left_at
        +Boolean is_active
    }
    
    Segment "1" --> "*" SegmentMember
```

---

### 12. Score（评分模型）- 核心实体

```mermaid
classDiagram
    class ScoreModel {
        +String model_id PK
        +String model_name
        +String model_type
        +String target_entity_type
        +JSON scoring_rules
        +Integer max_score
        +String status
        +DateTime created_at
        +DateTime updated_at
    }
    
    class ScoreRecord {
        +String record_id PK
        +String model_id FK
        +String entity_type
        +String entity_id
        +Integer score
        +String grade
        +JSON score_details
        +DateTime calculated_at
    }
    
    class ScoreHistory {
        +String history_id PK
        +String entity_type
        +String entity_id
        +String model_id FK
        +Integer score
        +DateTime recorded_at
    }
    
    ScoreModel "1" --> "*" ScoreRecord
```

**评分类型：**
- Lead评分：基于行为和画像的线索评分
- Account评分：企业健康度评分
- Contact评分：联系人参与度评分

---

### 13. Industry（行业）- 核心实体

```mermaid
classDiagram
    class Industry {
        +String industry_id PK
        +String industry_name
        +String industry_code
        +String parent_industry_id FK
        +Integer level
        +Integer sort_order
        +DateTime created_at
    }
```

---

### 14. Attribution（归因）- 核心实体

```mermaid
classDiagram
    class Attribution {
        +String attribution_id PK
        +String entity_type
        +String entity_id
        +String attribution_model
        +JSON touchpoint_sequence
        +JSON attribution_weights
        +DateTime created_at
        +DateTime updated_at
    }
    
    class TouchpointAttribution {
        +String ta_id PK
        +String attribution_id FK
        +String touchpoint_id FK
        +String campaign_id FK
        +String channel_id FK
        +Decimal attribution_weight
        +Integer position_in_journey
        +DateTime touchpoint_time
    }
    
    Attribution "1" --> "*" TouchpointAttribution
```

**归因模型：**
- 首次触点归因
- 末次触点归因
- 线性归因
- 时间衰减归因
- U型归因
- W型归因

---

### 15. CustomerJourney（客户旅程）- 核心实体

```mermaid
classDiagram
    class CustomerJourney {
        +String journey_id PK
        +String journey_name
        +String entity_type
        +String entity_id
        +String journey_stage
        +DateTime journey_start_at
        +DateTime journey_end_at
        +Integer total_touchpoints
        +Integer journey_duration_days
        +JSON stage_history
        +DateTime created_at
        +DateTime updated_at
    }
    
    class JourneyStage {
        +String stage_id PK
        +String stage_name
        +Integer stage_order
        +String stage_category
        +JSON milestone_criteria
    }
```

---

## 实体关系图

### 核心实体关系总览

```mermaid
erDiagram
    Account ||--o{ AccountContactRelation : has
    Contact ||--o{ AccountContactRelation : belongs
    Account ||--o{ Opportunity : has
    Contact ||--o{ Opportunity : has
    Lead ||--o| Contact : converts_to
    Lead ||--o| Account : converts_to
    Lead ||--o| Opportunity : converts_to
    
    Channel ||--o{ Touchpoint : generates
    Campaign ||--o{ Touchpoint : generates
    Contact ||--o{ Touchpoint : receives
    Lead ||--o{ Touchpoint : receives
    Account ||--o{ Touchpoint : receives
    
    Contact ||--o{ Event : generates
    Lead ||--o{ Event : generates
    Channel ||--o{ Event : tracks
    
    Opportunity ||--o{ OpportunityProduct : contains
    Product ||--o{ OpportunityProduct : included_in
    
    Campaign ||--o{ Lead : generates
    Campaign ||--o{ CampaignMember : has
    
    Account ||--o{ AccountChannelIdentity : has
    Contact ||--o{ ContactChannelIdentity : has
    Lead ||--o{ LeadChannelIdentity : has
    Channel ||--o{ AccountChannelIdentity : identifies
    Channel ||--o{ ContactChannelIdentity : identifies
    Channel ||--o{ LeadChannelIdentity : identifies
    
    Tag ||--o{ TagRelation : applies_to
    Segment ||--o{ SegmentMember : contains
    
    ScoreModel ||--o{ ScoreRecord : calculates
    
    Industry ||--o{ Account : categorizes
    Industry ||--o{ Lead : categorizes
    
    Attribution ||--o{ TouchpointAttribution : analyzes
    Touchpoint ||--o{ TouchpointAttribution : contributes_to
    Campaign ||--o{ TouchpointAttribution : contributes_to
    
    CustomerJourney ||--o{ Touchpoint : tracks
```

---

### 全渠道身份关联图

```mermaid
graph TB
    subgraph "渠道身份体系"
        WX[微信渠道身份]
        WEB[官网渠道身份]
        DY[抖音渠道身份]
        EMAIL[邮件渠道身份]
        PHONE[电话渠道身份]
        OFFLINE[线下活动身份]
    end
    
    subgraph "统一Contact"
        Contact[Contact 联系人<br/>统一ID: C001]
    end
    
    subgraph "统一Account"
        Account[Account 企业<br/>统一ID: A001]
    end
    
    WX --> |Identity Mapping| Contact
    WEB --> |Identity Mapping| Contact
    DY --> |Identity Mapping| Contact
    EMAIL --> |Identity Mapping| Contact
    PHONE --> |Identity Mapping| Contact
    OFFLINE --> |Identity Mapping| Contact
    
    Contact --> |Belongs To| Account
    
    style Contact fill:#ff9999
    style Account fill:#99ccff
```

---

## 业务流程

### 线索到商机转化流程

```mermaid
stateDiagram-v2
    [*] --> NewLead: 捕获线索
    NewLead --> Contacted: 首次联系
    Contacted --> Qualified: 资格验证
    Qualified --> Converted: 转化
    
    Converted --> Contact: 创建联系人
    Converted --> Account: 创建/关联企业
    Converted --> Opportunity: 创建商机
    
    Contact --> [*]
    Account --> [*]
    Opportunity --> [*]
    
    NewLead --> Invalid: 标记无效
    Contacted --> Invalid: 标记无效
    Invalid --> [*]
```

---

### 商机阶段流转流程

```mermaid
stateDiagram-v2
    [*] --> Lead: 线索阶段
    Lead --> Qualification: 需求确认
    Qualification --> SolutionDesign: 方案设计
    SolutionDesign --> Negotiation: 商务谈判
    Negotiation --> Contract: 合同签订
    Contract --> Won: 赢单
    Contract --> Lost: 输单
    
    Lead --> Lost: 丢失
    Qualification --> Lost: 丢失
    SolutionDesign --> Lost: 丢失
    Negotiation --> Lost: 丢失
    
    Won --> [*]
    Lost --> [*]
```

---

### 客户生命周期流程

```mermaid
stateDiagram-v2
    [*] --> Awareness: 认知阶段
    Awareness --> Consideration: 考虑阶段
    Consideration --> Decision: 决策阶段
    Decision --> Retention: 留存阶段
    Retention --> Expansion: 扩展阶段
    
    Retention --> Churn: 流失
    Expansion --> Churn: 流失
    
    Churn --> Winback: 召回
    Winback --> Retention: 成功召回
    Winback --> [*]: 永久流失
    
    Expansion --> [*]: 持续合作
```

---

### 全渠道数据流转流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Channel as 渠道
    participant Event as 事件系统
    participant Identity as 身份识别
    participant CDP as CDP核心
    participant Lead as Lead管理
    participant Contact as Contact管理
    participant Account as Account管理
    
    User->>Channel: 1. 访问/互动
    Channel->>Event: 2. 记录事件
    Event->>Identity: 3. 身份识别
    
    alt 新用户
        Identity->>Lead: 4a. 创建Lead
        Lead->>CDP: 5a. 保存Lead数据
    else 已识别用户
        Identity->>Contact: 4b. 关联Contact
        Contact->>Account: 5b. 关联Account
    end
    
    CDP->>Event: 6. 触发规则引擎
    Event->>Channel: 7. 个性化响应
    Channel->>User: 8. 返回内容
```

---

## 数据模型详细设计

### Account 详细字段设计

| 字段名 | 类型 | 长度 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|------|
| account_id | VARCHAR | 64 | ✓ | 账户唯一ID（PK） | ACC_20231105001 |
| account_name | VARCHAR | 200 | ✓ | 企业名称 | 阿里巴巴网络技术有限公司 |
| unified_social_credit_code | VARCHAR | 18 |  | 统一社会信用代码 | 91330000MA27XYZ123 |
| account_type | VARCHAR | 50 | ✓ | 客户类型 | CUSTOMER（客户）/PARTNER（合作伙伴）/COMPETITOR（竞争对手）/PROSPECT（潜在客户） |
| industry_id | VARCHAR | 64 |  | 行业ID（FK） | IND_001 |
| account_status | VARCHAR | 50 | ✓ | 账户状态 | ACTIVE（活跃）/DORMANT（休眠）/CHURNED（流失）/BLACKLIST（黑名单） |
| account_level | VARCHAR | 50 |  | 客户等级 | STRATEGIC（战略级）/IMPORTANT（重要级）/NORMAL（普通级） |
| annual_revenue | DECIMAL | (18,2) |  | 年营收（万元） | 50000.00 |
| employee_count | INT |  |  | 员工人数 | 5000 |
| company_website | VARCHAR | 500 |  | 公司网站 | https://www.alibaba.com |
| company_address | VARCHAR | 500 |  | 公司地址 | 浙江省杭州市余杭区文一西路969号 |
| province | VARCHAR | 50 |  | 省份 | 浙江省 |
| city | VARCHAR | 50 |  | 城市 | 杭州市 |
| district | VARCHAR | 50 |  | 区县 | 余杭区 |
| account_source | VARCHAR | 100 |  | 来源 | WEBSITE/EXHIBITION/PARTNER/COLD_CALL |
| primary_channel_id | VARCHAR | 64 |  | 主渠道ID（FK） | CH_001 |
| owner_user_id | VARCHAR | 64 |  | 负责人ID | USER_001 |
| created_at | DATETIME |  | ✓ | 创建时间 | 2023-11-05 10:30:00 |
| updated_at | DATETIME |  | ✓ | 更新时间 | 2023-11-05 10:30:00 |
| custom_fields | JSON |  |  | 自定义字段 | {"crm_id": "CRM001"} |
| tags | JSON |  |  | 标签数组 | ["高价值客户","AI行业"] |
| health_score | INT |  |  | 健康度评分（0-100） | 85 |
| lifecycle_stage | VARCHAR | 50 |  | 生命周期阶段 | AWARENESS/CONSIDERATION/DECISION/RETENTION/EXPANSION |

---

### Contact 详细字段设计

| 字段名 | 类型 | 长度 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|------|
| contact_id | VARCHAR | 64 | ✓ | 联系人唯一ID（PK） | CNT_20231105001 |
| contact_name | VARCHAR | 100 | ✓ | 联系人姓名 | 张伟 |
| mobile_phone | VARCHAR | 20 |  | 手机号 | 13800138000 |
| email | VARCHAR | 200 |  | 邮箱 | zhangwei@company.com |
| wechat_id | VARCHAR | 100 |  | 微信ID | wx_zhangwei |
| job_title | VARCHAR | 100 |  | 职位 | CTO |
| department | VARCHAR | 100 |  | 部门 | 技术部 |
| contact_status | VARCHAR | 50 | ✓ | 联系人状态 | ACTIVE（活跃）/INACTIVE（不活跃）/BOUNCED（退订）/UNSUBSCRIBED（取消订阅） |
| primary_account_id | VARCHAR | 64 |  | 主要关联企业ID（FK） | ACC_20231105001 |
| contact_source | VARCHAR | 100 |  | 来源 | WEBSITE/FORM/IMPORT/API |
| primary_channel_id | VARCHAR | 64 |  | 主渠道ID（FK） | CH_001 |
| owner_user_id | VARCHAR | 64 |  | 负责人ID | USER_001 |
| created_at | DATETIME |  | ✓ | 创建时间 | 2023-11-05 10:30:00 |
| updated_at | DATETIME |  | ✓ | 更新时间 | 2023-11-05 10:30:00 |
| custom_fields | JSON |  |  | 自定义字段 | {"birthday": "1985-01-01"} |
| tags | JSON |  |  | 标签数组 | ["决策者","技术背景"] |
| engagement_score | INT |  |  | 参与度评分（0-100） | 75 |
| lifecycle_stage | VARCHAR | 50 |  | 生命周期阶段 | SUBSCRIBER/LEAD/MQL/SQL/OPPORTUNITY/CUSTOMER |
| is_decision_maker | BOOLEAN |  |  | 是否决策者 | true |
| is_verified | BOOLEAN |  |  | 是否已验证 | true |

---

### Lead 详细字段设计

| 字段名 | 类型 | 长度 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|------|
| lead_id | VARCHAR | 64 | ✓ | 线索唯一ID（PK） | LEAD_20231105001 |
| lead_name | VARCHAR | 100 | ✓ | 线索姓名 | 李明 |
| company_name | VARCHAR | 200 |  | 公司名称 | XX科技有限公司 |
| mobile_phone | VARCHAR | 20 |  | 手机号 | 13900139000 |
| email | VARCHAR | 200 |  | 邮箱 | liming@company.com |
| wechat_id | VARCHAR | 100 |  | 微信ID | wx_liming |
| job_title | VARCHAR | 100 |  | 职位 | 产品经理 |
| lead_source | VARCHAR | 100 | ✓ | 线索来源 | WEBSITE/FORM/CAMPAIGN/COLD_CALL/REFERRAL |
| channel_id | VARCHAR | 64 |  | 渠道ID（FK） | CH_001 |
| campaign_id | VARCHAR | 64 |  | 营销活动ID（FK） | CMP_001 |
| lead_status | VARCHAR | 50 | ✓ | 线索状态 | NEW（新建）/CONTACTED（已联系）/QUALIFIED（已限定）/CONVERTED（已转化）/DISQUALIFIED（无效） |
| lead_score | INT |  |  | 线索评分（0-100） | 80 |
| lead_grade | VARCHAR | 10 |  | 线索等级 | A/B/C/D |
| industry_id | VARCHAR | 64 |  | 行业ID（FK） | IND_001 |
| province | VARCHAR | 50 |  | 省份 | 广东省 |
| city | VARCHAR | 50 |  | 城市 | 深圳市 |
| owner_user_id | VARCHAR | 64 |  | 负责人ID | USER_001 |
| created_at | DATETIME |  | ✓ | 创建时间 | 2023-11-05 10:30:00 |
| updated_at | DATETIME |  | ✓ | 更新时间 | 2023-11-05 10:30:00 |
| last_contacted_at | DATETIME |  |  | 最后联系时间 | 2023-11-05 14:00:00 |
| converted_at | DATETIME |  |  | 转化时间 | 2023-11-10 09:00:00 |
| converted_contact_id | VARCHAR | 64 |  | 转化后联系人ID（FK） | CNT_20231110001 |
| converted_account_id | VARCHAR | 64 |  | 转化后企业ID（FK） | ACC_20231110001 |
| converted_opportunity_id | VARCHAR | 64 |  | 转化后商机ID（FK） | OPP_20231110001 |
| custom_fields | JSON |  |  | 自定义字段 | {"product_interest": "AI"} |
| tags | JSON |  |  | 标签数组 | ["高意向","下载过白皮书"] |
| is_qualified | BOOLEAN |  |  | 是否为合格线索 | true |

---

### Opportunity 详细字段设计

| 字段名 | 类型 | 长度 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|------|
| opportunity_id | VARCHAR | 64 | ✓ | 商机唯一ID（PK） | OPP_20231105001 |
| opportunity_name | VARCHAR | 200 | ✓ | 商机名称 | XX公司-AI平台采购项目 |
| account_id | VARCHAR | 64 | ✓ | 关联企业ID（FK） | ACC_20231105001 |
| primary_contact_id | VARCHAR | 64 |  | 主要联系人ID（FK） | CNT_20231105001 |
| lead_id | VARCHAR | 64 |  | 来源线索ID（FK） | LEAD_20231105001 |
| opportunity_type | VARCHAR | 50 |  | 商机类型 | NEW_BUSINESS（新客户）/UPSELL（追加销售）/RENEWAL（续约）/CROSS_SELL（交叉销售） |
| opportunity_source | VARCHAR | 100 |  | 商机来源 | LEAD_CONVERSION/DIRECT_SALES/PARTNER |
| amount | DECIMAL | (18,2) |  | 金额 | 1000000.00 |
| currency | VARCHAR | 10 |  | 货币 | CNY |
| stage | VARCHAR | 50 | ✓ | 阶段 | QUALIFICATION/NEEDS_ANALYSIS/PROPOSAL/NEGOTIATION/CLOSED_WON/CLOSED_LOST |
| probability | INT |  |  | 赢单概率（0-100） | 60 |
| expected_close_date | DATE |  |  | 预计成交日期 | 2023-12-31 |
| actual_close_date | DATE |  |  | 实际成交日期 | 2023-12-25 |
| close_reason | VARCHAR | 200 |  | 关闭原因 | 价格因素/竞争对手/预算不足/成功签约 |
| owner_user_id | VARCHAR | 64 |  | 负责人ID | USER_001 |
| product_ids | JSON |  |  | 产品ID数组 | ["PRD_001", "PRD_002"] |
| campaign_id | VARCHAR | 64 |  | 来源活动ID（FK） | CMP_001 |
| created_at | DATETIME |  | ✓ | 创建时间 | 2023-11-05 10:30:00 |
| updated_at | DATETIME |  | ✓ | 更新时间 | 2023-11-05 10:30:00 |
| custom_fields | JSON |  |  | 自定义字段 | {"contract_type": "annual"} |
| tags | JSON |  |  | 标签数组 | ["重点项目","Q4目标"] |
| days_in_stage | INT |  |  | 当前阶段停留天数 | 15 |
| is_won | BOOLEAN |  |  | 是否赢单 | false |
| is_lost | BOOLEAN |  |  | 是否输单 | false |

---

### Channel 详细字段设计

| 字段名 | 类型 | 长度 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|------|
| channel_id | VARCHAR | 64 | ✓ | 渠道唯一ID（PK） | CH_001 |
| channel_name | VARCHAR | 100 | ✓ | 渠道名称 | 官网-产品页 |
| channel_type | VARCHAR | 50 | ✓ | 渠道类型 | WEBSITE/WECHAT/DOUYIN/EMAIL/PHONE/OFFLINE/PARTNER |
| channel_category | VARCHAR | 50 |  | 渠道分类 | ONLINE（线上）/OFFLINE（线下）/SOCIAL（社交）/DIRECT（直销） |
| parent_channel_id | VARCHAR | 64 |  | 父渠道ID（FK） | CH_PARENT_001 |
| channel_status | VARCHAR | 50 | ✓ | 渠道状态 | ACTIVE（活跃）/INACTIVE（停用）/TESTING（测试中） |
| channel_config | JSON |  |  | 渠道配置 | {"api_key": "xxx", "webhook_url": "xxx"} |
| cost | DECIMAL | (18,2) |  | 成本 | 50000.00 |
| created_at | DATETIME |  | ✓ | 创建时间 | 2023-11-05 10:30:00 |
| updated_at | DATETIME |  | ✓ | 更新时间 | 2023-11-05 10:30:00 |
| custom_fields | JSON |  |  | 自定义字段 | {"partner_name": "XX合作伙伴"} |

---

### Touchpoint 详细字段设计

| 字段名 | 类型 | 长度 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|------|
| touchpoint_id | VARCHAR | 64 | ✓ | 触点唯一ID（PK） | TP_20231105001 |
| touchpoint_type | VARCHAR | 50 | ✓ | 触点类型 | PAGE_VIEW/FORM_SUBMIT/DOWNLOAD/EMAIL/CALL/MEETING/CHAT/SOCIAL |
| channel_id | VARCHAR | 64 |  | 渠道ID（FK） | CH_001 |
| campaign_id | VARCHAR | 64 |  | 活动ID（FK） | CMP_001 |
| contact_id | VARCHAR | 64 |  | 联系人ID（FK） | CNT_20231105001 |
| lead_id | VARCHAR | 64 |  | 线索ID（FK） | LEAD_20231105001 |
| account_id | VARCHAR | 64 |  | 企业ID（FK） | ACC_20231105001 |
| touchpoint_time | DATETIME | ✓ | ✓ | 触点时间 | 2023-11-05 14:30:00 |
| touchpoint_direction | VARCHAR | 20 |  | 触点方向 | INBOUND（入站）/OUTBOUND（出站） |
| touchpoint_status | VARCHAR | 50 |  | 触点状态 | COMPLETED（完成）/SCHEDULED（已安排）/CANCELLED（取消） |
| content_type | VARCHAR | 50 |  | 内容类型 | WHITEPAPER/CASE_STUDY/WEBINAR/DEMO/PROPOSAL |
| content_id | VARCHAR | 64 |  | 内容ID（FK） | CONTENT_001 |
| subject | VARCHAR | 200 |  | 主题 | 产品演示会议 |
| description | TEXT |  |  | 描述 | 讨论了AI平台的技术架构... |
| duration_seconds | INT |  |  | 持续时长（秒） | 3600 |
| owner_user_id | VARCHAR | 64 |  | 负责人ID | USER_001 |
| metadata | JSON |  |  | 元数据 | {"device": "mobile", "location": "Beijing"} |
| utm_params | JSON |  |  | UTM参数 | {"utm_source": "baidu", "utm_medium": "cpc"} |
| created_at | DATETIME |  | ✓ | 创建时间 | 2023-11-05 14:30:00 |

---

### Event 详细字段设计

| 字段名 | 类型 | 长度 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|------|
| event_id | VARCHAR | 64 | ✓ | 事件唯一ID（PK） | EVT_20231105001 |
| event_name | VARCHAR | 100 | ✓ | 事件名称 | page_view |
| event_type | VARCHAR | 50 | ✓ | 事件类型 | PAGE_VIEW/CLICK/FORM_START/FORM_SUBMIT/DOWNLOAD/VIDEO_PLAY/SEARCH |
| channel_id | VARCHAR | 64 |  | 渠道ID（FK） | CH_001 |
| contact_id | VARCHAR | 64 |  | 联系人ID（FK） | CNT_20231105001 |
| lead_id | VARCHAR | 64 |  | 线索ID（FK） | LEAD_20231105001 |
| account_id | VARCHAR | 64 |  | 企业ID（FK） | ACC_20231105001 |
| event_time | DATETIME |  | ✓ | 事件时间 | 2023-11-05 14:35:20 |
| session_id | VARCHAR | 64 |  | 会话ID | SESSION_20231105001 |
| device_type | VARCHAR | 50 |  | 设备类型 | DESKTOP/MOBILE/TABLET |
| browser | VARCHAR | 50 |  | 浏览器 | Chrome |
| os | VARCHAR | 50 |  | 操作系统 | Windows 10 |
| ip_address | VARCHAR | 50 |  | IP地址 | 192.168.1.1 |
| page_url | VARCHAR | 1000 |  | 页面URL | https://www.example.com/product/ai-platform |
| referrer_url | VARCHAR | 1000 |  | 来源URL | https://www.baidu.com/s?wd=AI平台 |
| event_properties | JSON |  |  | 事件属性 | {"button_text": "申请试用", "form_id": "trial_form"} |
| utm_params | JSON |  |  | UTM参数 | {"utm_source": "baidu", "utm_campaign": "Q4_campaign"} |
| created_at | DATETIME |  | ✓ | 创建时间 | 2023-11-05 14:35:20 |

---

## 全渠道身份映射方案

### 身份识别优先级

```mermaid
graph LR
    A[多渠道数据] --> B{身份识别}
    B --> C[1. 手机号匹配]
    B --> D[2. 邮箱匹配]
    B --> E[3. 企业微信ID]
    B --> F[4. 统一社会信用代码]
    B --> G[5. 自定义ID]
    
    C --> H[合并至统一Contact]
    D --> H
    E --> H
    F --> I[合并至统一Account]
    G --> H
    
    H --> J[建立ChannelIdentity关联]
    I --> K[建立AccountChannelIdentity关联]
```

### 身份合并规则

```mermaid
flowchart TD
    Start[接收新数据] --> Check{是否存在标识符}
    Check -->|有手机号| Phone[手机号匹配]
    Check -->|有邮箱| Email[邮箱匹配]
    Check -->|有微信ID| WeChat[微信ID匹配]
    
    Phone --> Match{找到匹配?}
    Email --> Match
    WeChat --> Match
    
    Match -->|是| Merge[合并到现有Contact]
    Match -->|否| Create[创建新Contact]
    
    Merge --> AddIdentity[添加渠道身份]
    Create --> AddIdentity
    
    AddIdentity --> UpdateScore[更新评分]
    UpdateScore --> End[完成]
```

---

## 数据字典总结

### 核心实体数量统计

| 实体类型 | 实体数量 | 说明 |
|---------|---------|------|
| 客户主体实体 | 3 | Account, Contact, Lead |
| 业务实体 | 2 | Opportunity, Product |
| 营销实体 | 2 | Campaign, Channel |
| 交互实体 | 2 | Touchpoint, Event |
| 关系实体 | 6 | AccountContactRelation, AccountRelation, OpportunityProduct, CampaignMember, TagRelation, SegmentMember |
| 身份实体 | 3 | AccountChannelIdentity, ContactChannelIdentity, LeadChannelIdentity |
| 分析实体 | 5 | Segment, Tag, Score, Attribution, CustomerJourney |
| 支撑实体 | 2 | Industry, ProductCategory |
| **合计** | **25** | 覆盖B2B CDP核心业务场景 |

---

## 扩展建议

### 可选扩展实体（根据业务需要）

1. **Content（内容资产）**
   - 营销内容管理
   - 内容效果追踪

2. **Order（订单）**
   - 如需管理订单详情
   - 支持电商场景

3. **Contract（合同）**
   - 合同管理
   - 续约提醒

4. **Partner（合作伙伴）**
   - 渠道伙伴管理
   - 分销体系

5. **Competitor（竞争对手）**
   - 竞争对手分析
   - 竞品情报

6. **Task（任务）**
   - 销售任务管理
   - 跟进提醒

7. **Note（备注）**
   - 客户备注
   - 沟通记录

---

## 技术实现建议

### 数据库选型建议

```mermaid
graph TB
    subgraph "主数据存储"
        PG[PostgreSQL<br/>关系型数据<br/>Account/Contact/Lead/Opportunity]
    end
    
    subgraph "行为数据存储"
        CH[ClickHouse<br/>海量事件数据<br/>Event/Touchpoint]
    end
    
    subgraph "搜索引擎"
        ES[Elasticsearch<br/>全文搜索<br/>客户搜索/标签搜索]
    end
    
    subgraph "缓存层"
        Redis[Redis<br/>热数据缓存<br/>评分/标签]
    end
    
    subgraph "数据仓库"
        DW[数据仓库<br/>分析报表<br/>BI分析]
    end
    
    PG --> ES
    PG --> Redis
    CH --> DW
    PG --> DW
```

### 关键索引建议

**Account表索引：**
```sql
-- 主键索引
PRIMARY KEY (account_id)

-- 业务索引
CREATE INDEX idx_account_name ON Account(account_name);
CREATE INDEX idx_unified_code ON Account(unified_social_credit_code);
CREATE INDEX idx_account_status ON Account(account_status);
CREATE INDEX idx_account_owner ON Account(owner_user_id);
CREATE INDEX idx_account_created ON Account(created_at);

-- 组合索引
CREATE INDEX idx_account_type_status ON Account(account_type, account_status);
CREATE INDEX idx_account_city ON Account(province, city);
```

**Contact表索引：**
```sql
-- 主键索引
PRIMARY KEY (contact_id)

-- 业务索引
CREATE INDEX idx_contact_phone ON Contact(mobile_phone);
CREATE INDEX idx_contact_email ON Contact(email);
CREATE INDEX idx_contact_wechat ON Contact(wechat_id);
CREATE INDEX idx_contact_account ON Contact(primary_account_id);
CREATE INDEX idx_contact_status ON Contact(contact_status);

-- 组合索引
CREATE INDEX idx_contact_phone_email ON Contact(mobile_phone, email);
```

**Event表索引（ClickHouse）：**
```sql
-- 主排序键
ORDER BY (event_time, contact_id, event_type)

-- 分区键
PARTITION BY toYYYYMM(event_time)
```

---

## 总结

本实体设计方案包含：

✅ **25个核心实体**，覆盖B2B CDP全业务场景  
✅ **全渠道身份映射**方案，支持跨渠道客户识别  
✅ **完整的客户生命周期**管理  
✅ **从线索到商机**的完整转化流程  
✅ **灵活的标签和分群**能力  
✅ **多维度的归因分析**能力  
✅ **详细的字段设计**和数据字典  

该方案可以支撑：
- 全渠道客户数据整合
- 客户360度画像
- 精准营销和客户分群
- 销售线索管理和转化
- 客户旅程分析
- 营销归因分析
- 客户价值评估

根据实际业务需要，可以选择性实现部分实体，并在后续迭代中逐步完善。
