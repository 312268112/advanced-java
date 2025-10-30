# 企业级CDP数据Schema设计

## 目录

- [一、Schema设计原则](#一schema设计原则)
- [二、标准化数据模型(DWD层)](#二标准化数据模型dwd层)
  - [2.1 客户企业主题](#21-客户企业主题)
  - [2.2 联系人主题](#22-联系人主题)
  - [2.3 行为事件主题](#23-行为事件主题)
  - [2.4 组织关系主题](#24-组织关系主题)
  - [2.5 OneID映射表](#25-oneid映射表)
- [三、数据源Schema(ODS层)](#三数据源schemaods层)
  - [3.1 企业微信数据Schema](#31-企业微信数据schema)
  - [3.2 CRM数据Schema](#32-crm数据schema)
  - [3.3 广告数据Schema](#33-广告数据schema)
- [四、数据映射规则](#四数据映射规则)
  - [4.1 企业微信→标准模型映射](#41-企业微信标准模型映射)
  - [4.2 CRM→标准模型映射](#42-crm标准模型映射)
  - [4.3 广告→标准模型映射](#43-广告标准模型映射)
- [五、扩展字段设计](#五扩展字段设计)

---

## 一、Schema设计原则

### 1.1 核心原则

1. **标准化优先**: DWD层采用统一的数据模型,不依赖于数据源
2. **可扩展性**: 使用JSON字段存储灵活的扩展属性
3. **时间有效性**: 关键实体支持有效期管理(start_time, end_time)
4. **数据溯源**: 记录数据来源(source_type, source_id)
5. **幂等性**: 支持重复加工,使用唯一键保证数据不重复

### 1.2 命名规范

- **表名**: 使用下划线命名法,如 `dwd_customer_account`
- **字段名**: 使用下划线命名法,如 `account_name`
- **枚举值**: 使用大写+下划线,如 `WEWORK`, `CRM`
- **时间字段**: 统一使用 `_time` 后缀,如 `create_time`
- **金额字段**: 统一使用分为单位,字段类型为 `BIGINT`

### 1.3 数据类型规范

| 业务类型 | 数据类型 | 说明 | 示例 |
|---------|---------|------|------|
| 主键ID | VARCHAR(64) | 使用雪花算法生成 | "123456789012345678" |
| 金额 | BIGINT | 以分为单位 | 100000 (表示1000.00元) |
| 枚举 | VARCHAR(32) | 大写字母+下划线 | "WEWORK", "CRM" |
| 时间戳 | BIGINT | Unix时间戳(毫秒) | 1698739200000 |
| 时间 | DATETIME | 标准时间格式 | 2024-10-30 12:00:00 |
| 布尔 | TINYINT | 0=false, 1=true | 0, 1 |
| JSON | TEXT | 存储非结构化数据 | {"key": "value"} |
| 手机号 | VARCHAR(32) | 加密后存储 | "AES_ENCRYPTED_DATA" |

---

## 二、标准化数据模型(DWD层)

### 2.1 客户企业主题

#### 2.1.1 客户企业主表 (dwd_customer_account)

存储客户企业的基本信息和业务属性。

```sql
CREATE TABLE dwd_customer_account (
    -- 主键与标识
    account_id VARCHAR(64) PRIMARY KEY COMMENT '客户企业ID(CDP统一ID)',
    account_name VARCHAR(256) NOT NULL COMMENT '企业名称',
    account_name_en VARCHAR(256) COMMENT '企业英文名称',
    
    -- 企业基本信息
    industry VARCHAR(64) COMMENT '所属行业: 互联网/金融/制造/教育等',
    sub_industry VARCHAR(64) COMMENT '细分行业',
    company_type VARCHAR(32) COMMENT '企业类型: 国企/民企/外企/合资',
    company_scale VARCHAR(32) COMMENT '企业规模: 0-50/50-150/150-500/500-2000/2000+',
    employee_count INT COMMENT '员工人数',
    
    -- 经营信息
    annual_revenue BIGINT COMMENT '年营收(分)',
    registered_capital BIGINT COMMENT '注册资本(分)',
    founded_date DATE COMMENT '成立日期',
    listing_status VARCHAR(32) COMMENT '上市状态: 未上市/新三板/A股/港股/美股',
    financing_stage VARCHAR(32) COMMENT '融资阶段: 未融资/天使轮/A轮/B轮/C轮/上市',
    
    -- 企业地址
    country VARCHAR(64) DEFAULT '中国' COMMENT '国家',
    province VARCHAR(64) COMMENT '省份',
    city VARCHAR(64) COMMENT '城市',
    district VARCHAR(64) COMMENT '区/县',
    address TEXT COMMENT '详细地址',
    
    -- 企业标识
    unified_social_credit_code VARCHAR(32) COMMENT '统一社会信用代码',
    business_license_number VARCHAR(32) COMMENT '营业执照号',
    
    -- 客户分类与状态
    customer_type VARCHAR(32) COMMENT '客户类型: LEAD(潜在客户)/OPPORTUNITY(商机)/CUSTOMER(成交客户)/LOST(流失客户)',
    customer_level VARCHAR(32) COMMENT '客户等级: S(战略)/A(重点)/B(普通)/C(小客户)',
    customer_status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '客户状态: ACTIVE(活跃)/INACTIVE(不活跃)/CLOSED(已关闭)',
    
    -- 客户关系
    parent_account_id VARCHAR(64) COMMENT '母公司ID(支持集团客户)',
    account_hierarchy_level INT DEFAULT 1 COMMENT '层级: 1=集团/2=子公司/3=分公司',
    
    -- 业务负责人
    owner_staff_id VARCHAR(64) COMMENT '客户负责人(销售)ID',
    owner_staff_name VARCHAR(128) COMMENT '客户负责人姓名',
    owner_dept_id VARCHAR(64) COMMENT '负责部门ID',
    owner_dept_name VARCHAR(128) COMMENT '负责部门名称',
    
    -- 业务数据
    first_contact_date DATE COMMENT '首次接触日期',
    last_contact_date DATE COMMENT '最近接触日期',
    total_opportunity_count INT DEFAULT 0 COMMENT '商机总数',
    total_opportunity_amount BIGINT DEFAULT 0 COMMENT '商机总额(分)',
    total_contract_count INT DEFAULT 0 COMMENT '合同总数',
    total_contract_amount BIGINT DEFAULT 0 COMMENT '合同总额(分)',
    
    -- 活跃度指标
    interaction_count_l7d INT DEFAULT 0 COMMENT '近7天互动次数',
    interaction_count_l30d INT DEFAULT 0 COMMENT '近30天互动次数',
    last_active_time DATETIME COMMENT '最后活跃时间',
    
    -- 数据溯源
    source_type VARCHAR(32) COMMENT '数据来源: CRM/WEWORK/IMPORT/API',
    source_id VARCHAR(128) COMMENT '来源系统中的ID',
    source_create_time DATETIME COMMENT '来源系统创建时间',
    
    -- 扩展字段
    extra_attributes TEXT COMMENT '扩展属性(JSON格式)',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0=否/1=是',
    
    INDEX idx_account_name (account_name),
    INDEX idx_owner_staff (owner_staff_id),
    INDEX idx_customer_type (customer_type),
    INDEX idx_source (source_type, source_id),
    INDEX idx_parent (parent_account_id)
) COMMENT='客户企业主表';
```

**核心字段说明**:

| 字段类别 | 核心字段 | 业务含义 |
|---------|---------|---------|
| 企业身份 | account_id, account_name | 唯一标识客户企业 |
| 企业画像 | industry, company_scale, annual_revenue | 用于客户分层和圈人 |
| 客户分类 | customer_type, customer_level | 销售管理的核心字段 |
| 组织关系 | parent_account_id, account_hierarchy_level | 支持集团型客户 |
| 业务指标 | total_contract_amount, interaction_count_l30d | 客户价值评估 |
| 数据溯源 | source_type, source_id | 追溯数据来源 |

#### 2.1.2 客户企业组织架构表 (dwd_customer_org)

存储客户企业内部的组织架构信息。

```sql
CREATE TABLE dwd_customer_org (
    -- 主键
    org_id VARCHAR(64) PRIMARY KEY COMMENT '组织ID',
    
    -- 所属企业
    account_id VARCHAR(64) NOT NULL COMMENT '所属客户企业ID',
    
    -- 组织信息
    org_name VARCHAR(256) NOT NULL COMMENT '组织名称',
    org_name_en VARCHAR(256) COMMENT '组织英文名称',
    org_code VARCHAR(64) COMMENT '组织编码',
    org_type VARCHAR(32) COMMENT '组织类型: DEPT(部门)/CENTER(中心)/TEAM(小组)/BRANCH(分公司)',
    
    -- 层级关系
    parent_org_id VARCHAR(64) COMMENT '父组织ID',
    org_level INT DEFAULT 1 COMMENT '组织层级: 1/2/3/...',
    org_path TEXT COMMENT '组织路径: /root/dept1/team1',
    
    -- 负责人
    leader_cdp_id VARCHAR(64) COMMENT '组织负责人CDP ID',
    leader_name VARCHAR(128) COMMENT '负责人姓名',
    
    -- 状态
    org_status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '组织状态: ACTIVE/INACTIVE',
    
    -- 数据溯源
    source_type VARCHAR(32) COMMENT '数据来源: CRM/WEWORK/IMPORT',
    source_id VARCHAR(128) COMMENT '来源系统中的ID',
    
    -- 扩展字段
    extra_attributes TEXT COMMENT '扩展属性(JSON)',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    
    INDEX idx_account (account_id),
    INDEX idx_parent (parent_org_id),
    INDEX idx_leader (leader_cdp_id)
) COMMENT='客户企业组织架构表';
```

---

### 2.2 联系人主题

#### 2.2.1 联系人主表 (dwd_contact)

存储客户企业的联系人(决策人、影响者等)信息。

```sql
CREATE TABLE dwd_contact (
    -- 主键与标识
    cdp_id VARCHAR(64) PRIMARY KEY COMMENT '联系人CDP统一ID',
    
    -- 所属企业
    account_id VARCHAR(64) NOT NULL COMMENT '所属客户企业ID',
    account_name VARCHAR(256) COMMENT '所属企业名称(冗余)',
    
    -- 基本信息
    contact_name VARCHAR(128) NOT NULL COMMENT '姓名',
    contact_name_en VARCHAR(128) COMMENT '英文名',
    gender VARCHAR(8) COMMENT '性别: MALE/FEMALE/UNKNOWN',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    
    -- 职位信息
    job_title VARCHAR(128) COMMENT '职位名称',
    job_level VARCHAR(32) COMMENT '职级: STAFF(员工)/SUPERVISOR(主管)/MANAGER(经理)/DIRECTOR(总监)/VP(副总裁)/CXO(C级高管)',
    seniority VARCHAR(32) COMMENT '资历: JUNIOR(初级)/MIDDLE(中级)/SENIOR(高级)/EXPERT(专家)',
    department VARCHAR(256) COMMENT '部门',
    
    -- 联系方式(敏感信息)
    mobile_encrypted VARCHAR(256) COMMENT '手机号(加密)',
    mobile_hash VARCHAR(64) COMMENT '手机号哈希(用于匹配)',
    email_encrypted VARCHAR(256) COMMENT '邮箱(加密)',
    email_hash VARCHAR(64) COMMENT '邮箱哈希(用于匹配)',
    wechat VARCHAR(128) COMMENT '微信号',
    
    -- 组织关系
    org_id VARCHAR(64) COMMENT '所属组织ID',
    org_name VARCHAR(256) COMMENT '所属组织名称(冗余)',
    report_to_cdp_id VARCHAR(64) COMMENT '直属上级CDP ID',
    report_to_name VARCHAR(128) COMMENT '直属上级姓名(冗余)',
    
    -- 决策角色
    decision_role VARCHAR(32) COMMENT '决策角色: DECISION_MAKER(决策者)/INFLUENCER(影响者)/USER(使用者)/GATEKEEPER(把关者)',
    importance_level VARCHAR(32) COMMENT '重要程度: HIGH(高)/MEDIUM(中)/LOW(低)',
    
    -- 客户关系
    relationship_status VARCHAR(32) DEFAULT 'UNCONNECTED' COMMENT '关系状态: UNCONNECTED(未触达)/CONNECTED(已触达)/ACTIVE(活跃)/INACTIVE(不活跃)',
    owner_staff_id VARCHAR(64) COMMENT '对接销售ID',
    owner_staff_name VARCHAR(128) COMMENT '对接销售姓名',
    
    -- 活跃度
    first_contact_date DATE COMMENT '首次接触日期',
    last_contact_date DATE COMMENT '最近接触日期',
    interaction_count_total INT DEFAULT 0 COMMENT '总互动次数',
    interaction_count_l7d INT DEFAULT 0 COMMENT '近7天互动次数',
    interaction_count_l30d INT DEFAULT 0 COMMENT '近30天互动次数',
    last_active_time DATETIME COMMENT '最后活跃时间',
    
    -- 数据质量
    data_quality_score DECIMAL(5,2) COMMENT '数据质量分数(0-100)',
    data_completeness DECIMAL(5,2) COMMENT '数据完整度(0-100)',
    
    -- 数据溯源
    primary_source_type VARCHAR(32) COMMENT '主数据源: CRM/WEWORK/IMPORT',
    primary_source_id VARCHAR(128) COMMENT '主数据源ID',
    
    -- 扩展字段
    extra_attributes TEXT COMMENT '扩展属性(JSON)',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    
    INDEX idx_account (account_id),
    INDEX idx_name (contact_name),
    INDEX idx_job_level (job_level),
    INDEX idx_mobile_hash (mobile_hash),
    INDEX idx_email_hash (email_hash),
    INDEX idx_owner (owner_staff_id),
    INDEX idx_report_to (report_to_cdp_id)
) COMMENT='联系人主表';
```

**核心字段说明**:

| 字段类别 | 核心字段 | 业务含义 |
|---------|---------|---------|
| 身份标识 | cdp_id, account_id | 唯一标识联系人及其所属企业 |
| 职位信息 | job_title, job_level | 判断决策权重的核心字段 |
| 敏感信息 | mobile_encrypted, email_encrypted | 加密存储,用hash做匹配 |
| 组织关系 | report_to_cdp_id, org_id | 支持组织穿透查询 |
| 决策角色 | decision_role, importance_level | 销售策略制定的关键 |
| 活跃度 | interaction_count_l30d, last_active_time | 客户意向判断 |

#### 2.2.2 联系人组织关系表 (dwd_contact_org_relation)

支持一个联系人在多个组织中的情况(矩阵式管理、兼职等)。

```sql
CREATE TABLE dwd_contact_org_relation (
    -- 主键
    relation_id VARCHAR(64) PRIMARY KEY COMMENT '关系ID',
    
    -- 联系人与组织
    cdp_id VARCHAR(64) NOT NULL COMMENT '联系人CDP ID',
    account_id VARCHAR(64) NOT NULL COMMENT '客户企业ID',
    org_id VARCHAR(64) NOT NULL COMMENT '组织ID',
    
    -- 职位信息
    job_title VARCHAR(128) COMMENT '在该组织的职位',
    job_level VARCHAR(32) COMMENT '职级',
    job_type VARCHAR(32) COMMENT '岗位类型: PRIMARY(主职)/PART_TIME(兼职)/DOTTED_LINE(虚线汇报)',
    
    -- 汇报关系
    report_to_cdp_id VARCHAR(64) COMMENT '在该组织的直属上级',
    report_to_name VARCHAR(128) COMMENT '上级姓名',
    
    -- 有效期
    start_date DATE COMMENT '生效日期',
    end_date DATE COMMENT '失效日期(NULL表示仍有效)',
    is_current TINYINT DEFAULT 1 COMMENT '是否当前有效: 0=否/1=是',
    
    -- 数据溯源
    source_type VARCHAR(32) COMMENT '数据来源',
    source_id VARCHAR(128) COMMENT '来源系统ID',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_contact_org_type (cdp_id, org_id, job_type, is_current),
    INDEX idx_cdp (cdp_id),
    INDEX idx_org (org_id),
    INDEX idx_report_to (report_to_cdp_id),
    INDEX idx_current (is_current)
) COMMENT='联系人组织关系表';
```

---

### 2.3 行为事件主题

#### 2.3.1 行为事件明细表 (dwd_contact_event)

存储联系人的所有行为事件明细,支持按日期分区。

```sql
CREATE TABLE dwd_contact_event (
    -- 事件标识
    event_id VARCHAR(64) PRIMARY KEY COMMENT '事件ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    event_time DATETIME NOT NULL COMMENT '事件发生时间',
    event_date DATE NOT NULL COMMENT '事件日期(分区字段)',
    
    -- 事件主体
    cdp_id VARCHAR(64) NOT NULL COMMENT '联系人CDP ID',
    account_id VARCHAR(64) COMMENT '所属企业ID',
    
    -- 事件对象(如果有)
    target_type VARCHAR(32) COMMENT '目标类型: STAFF(员工)/PAGE(页面)/DOCUMENT(文档)/PRODUCT(产品)',
    target_id VARCHAR(64) COMMENT '目标ID',
    target_name VARCHAR(256) COMMENT '目标名称',
    
    -- 事件属性
    event_properties TEXT COMMENT '事件属性(JSON格式)',
    
    -- 渠道信息
    channel VARCHAR(32) COMMENT '事件渠道: WEWORK(企微)/WEB(网站)/EMAIL(邮件)/PHONE(电话)/MEETING(会议)',
    device_type VARCHAR(32) COMMENT '设备类型: PC/MOBILE/PAD',
    
    -- 位置信息
    ip_address VARCHAR(64) COMMENT 'IP地址',
    country VARCHAR(64) COMMENT '国家',
    province VARCHAR(64) COMMENT '省份',
    city VARCHAR(64) COMMENT '城市',
    
    -- 数据溯源
    source_type VARCHAR(32) NOT NULL COMMENT '数据来源: WEWORK/WEB/CRM/EMAIL',
    source_id VARCHAR(128) COMMENT '来源系统事件ID',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    
    INDEX idx_cdp_time (cdp_id, event_date),
    INDEX idx_account_time (account_id, event_date),
    INDEX idx_event_type (event_type, event_date),
    INDEX idx_channel (channel, event_date)
) COMMENT='行为事件明细表'
PARTITION BY RANGE (TO_DAYS(event_date)) (
    PARTITION p_202410 VALUES LESS THAN (TO_DAYS('2024-11-01')),
    PARTITION p_202411 VALUES LESS THAN (TO_DAYS('2024-12-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

**常见事件类型定义**:

| 事件类型 | 说明 | event_properties示例 |
|---------|------|---------------------|
| wework_add_contact | 企微添加外部联系人 | {"add_way": "扫码", "state": "渠道A"} |
| wework_chat | 企微聊天消息 | {"msg_type": "text", "direction": "in"} |
| wework_group_join | 加入企微群聊 | {"group_id": "xxx", "group_name": "产品交流群"} |
| page_view | 页面浏览 | {"page_url": "/product/detail", "duration": 120} |
| document_view | 文档查看 | {"doc_id": "123", "doc_name": "产品方案.pdf"} |
| email_open | 邮件打开 | {"campaign_id": "xxx", "subject": "新产品发布"} |
| email_click | 邮件链接点击 | {"link_url": "https://..."} |
| meeting_attend | 参加会议 | {"meeting_id": "xxx", "duration": 60} |
| form_submit | 表单提交 | {"form_type": "试用申请", "fields": {...}} |
| crm_call | CRM电话记录 | {"duration": 300, "call_result": "有意向"} |
| crm_visit | CRM拜访记录 | {"visit_type": "现场", "result": "洽谈顺利"} |

---

### 2.4 组织关系主题

#### 2.4.1 组织关系图谱 (dwd_org_graph)

针对复杂的组织关系查询,建议使用图数据库(Neo4j),但也可以用关系表存储。

```sql
CREATE TABLE dwd_org_graph (
    -- 主键
    relation_id VARCHAR(64) PRIMARY KEY COMMENT '关系ID',
    
    -- 关系类型
    relation_type VARCHAR(32) NOT NULL COMMENT '关系类型: REPORT_TO(汇报)/COLLEAGUE(同事)/SAME_ORG(同部门)',
    
    -- 关系双方
    from_cdp_id VARCHAR(64) NOT NULL COMMENT '起点联系人CDP ID',
    to_cdp_id VARCHAR(64) NOT NULL COMMENT '终点联系人CDP ID',
    
    -- 关系属性
    relation_strength DECIMAL(5,2) COMMENT '关系强度(0-1)',
    relation_distance INT COMMENT '关系距离(如几级汇报关系)',
    
    -- 所属企业
    account_id VARCHAR(64) NOT NULL COMMENT '客户企业ID',
    
    -- 有效期
    start_date DATE COMMENT '生效日期',
    end_date DATE COMMENT '失效日期',
    is_current TINYINT DEFAULT 1 COMMENT '是否当前有效',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_from_to_type (from_cdp_id, to_cdp_id, relation_type, is_current),
    INDEX idx_from (from_cdp_id),
    INDEX idx_to (to_cdp_id),
    INDEX idx_account (account_id),
    INDEX idx_type (relation_type)
) COMMENT='组织关系图谱表';
```

**Neo4j图模型(推荐)**:

```cypher
// 联系人节点
(c:Contact {
    cdp_id: "CDP_123456",
    name: "张三",
    job_title: "技术总监",
    job_level: "DIRECTOR",
    account_id: "ACCT_001"
})

// 组织节点
(o:Organization {
    org_id: "ORG_123",
    org_name: "技术部",
    account_id: "ACCT_001"
})

// 汇报关系
(c1:Contact)-[:REPORT_TO {since: "2024-01-01"}]->(c2:Contact)

// 所属关系
(c:Contact)-[:BELONG_TO {job_type: "PRIMARY"}]->(o:Organization)

// 同事关系
(c1:Contact)-[:COLLEAGUE {org_id: "ORG_123"}]->(c2:Contact)
```

---

### 2.5 OneID映射表

#### 2.5.1 ID映射关系表 (dwd_id_mapping)

存储CDP统一ID与各来源系统ID的映射关系。

```sql
CREATE TABLE dwd_id_mapping (
    -- 主键
    mapping_id VARCHAR(64) PRIMARY KEY COMMENT '映射记录ID',
    
    -- CDP统一ID
    cdp_id VARCHAR(64) NOT NULL COMMENT '联系人CDP统一ID',
    entity_type VARCHAR(32) NOT NULL DEFAULT 'CONTACT' COMMENT '实体类型: CONTACT/ACCOUNT',
    
    -- 来源系统ID
    source_type VARCHAR(32) NOT NULL COMMENT '来源系统: CRM/WEWORK/IMPORT/WEB',
    source_id VARCHAR(128) NOT NULL COMMENT '来源系统中的ID',
    source_name VARCHAR(256) COMMENT '来源系统中的名称',
    
    -- 匹配信息
    match_field VARCHAR(32) COMMENT '匹配字段: MOBILE/EMAIL/UNIONID/NAME_COMPANY',
    match_value_hash VARCHAR(128) COMMENT '匹配值哈希',
    match_confidence DECIMAL(5,2) DEFAULT 1.00 COMMENT '匹配置信度(0-1)',
    match_method VARCHAR(32) COMMENT '匹配方式: AUTO(自动)/MANUAL(人工)',
    
    -- 状态
    mapping_status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT '映射状态: ACTIVE(有效)/CONFLICT(冲突)/MERGED(已合并)',
    conflict_reason TEXT COMMENT '冲突原因',
    
    -- 审计信息
    created_by VARCHAR(64) COMMENT '创建人',
    reviewed_by VARCHAR(64) COMMENT '审核人',
    review_time DATETIME COMMENT '审核时间',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_source (source_type, source_id),
    INDEX idx_cdp (cdp_id),
    INDEX idx_match (match_field, match_value_hash),
    INDEX idx_status (mapping_status)
) COMMENT='ID映射关系表';
```

**核心业务逻辑**:

1. **新数据进入时**: 根据 `match_field` (手机号/邮箱等)查询是否已存在映射
2. **存在映射**: 直接使用已有的 `cdp_id`
3. **不存在映射**: 生成新的 `cdp_id`,创建映射记录
4. **冲突检测**: 一个 `source_id` 对应多个 `cdp_id` 时,标记为 `CONFLICT`,需人工审核

---

## 三、数据源Schema(ODS层)

### 3.1 企业微信数据Schema

#### 3.1.1 企微外部联系人表 (ods_wework_external_contact)

存储从企业微信获取的外部联系人原始数据。

```sql
CREATE TABLE ods_wework_external_contact (
    -- 主键
    id VARCHAR(64) PRIMARY KEY COMMENT '自增ID',
    
    -- 企微标识
    external_userid VARCHAR(128) NOT NULL COMMENT '企微外部联系人ID',
    unionid VARCHAR(128) COMMENT '微信unionid(跨应用唯一)',
    
    -- 基本信息
    name VARCHAR(128) COMMENT '外部联系人名称',
    avatar VARCHAR(512) COMMENT '头像URL',
    type TINYINT COMMENT '外部联系人类型: 1=微信用户/2=企微用户',
    gender TINYINT COMMENT '性别: 0=未知/1=男/2=女',
    
    -- 企业信息
    corp_name VARCHAR(256) COMMENT '企业名称',
    corp_full_name VARCHAR(512) COMMENT '企业全称',
    position VARCHAR(128) COMMENT '职位',
    
    -- 添加信息
    add_way INT COMMENT '添加方式',
    state VARCHAR(256) COMMENT '渠道参数',
    userid VARCHAR(128) NOT NULL COMMENT '添加该外部联系人的企业员工userid',
    
    -- 标签
    tag_ids TEXT COMMENT '企微标签ID列表(JSON数组)',
    
    -- 备注
    remark VARCHAR(512) COMMENT '备注',
    description VARCHAR(1024) COMMENT '描述',
    remark_corp_name VARCHAR(256) COMMENT '备注企业名称',
    remark_mobiles TEXT COMMENT '备注手机号(JSON数组)',
    
    -- 原始数据
    raw_data TEXT COMMENT '原始JSON数据',
    
    -- 数据采集信息
    corp_id VARCHAR(128) COMMENT '企业微信企业ID',
    data_source VARCHAR(32) DEFAULT 'WEWORK_API' COMMENT '数据来源',
    sync_time DATETIME COMMENT '同步时间',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_external_userid (external_userid),
    INDEX idx_unionid (unionid),
    INDEX idx_userid (userid),
    INDEX idx_corp_name (corp_name),
    INDEX idx_sync_time (sync_time)
) COMMENT='企微外部联系人原始表';
```

#### 3.1.2 企微聊天事件表 (ods_wework_chat_event)

存储企微聊天互动原始数据。

```sql
CREATE TABLE ods_wework_chat_event (
    -- 主键
    event_id VARCHAR(64) PRIMARY KEY COMMENT '事件ID',
    
    -- 消息标识
    msgid VARCHAR(128) COMMENT '消息ID',
    seq BIGINT COMMENT '消息序列号',
    
    -- 时间
    msg_time DATETIME NOT NULL COMMENT '消息时间',
    event_date DATE NOT NULL COMMENT '事件日期',
    
    -- 消息主体
    from_userid VARCHAR(128) COMMENT '发送者userid(员工)',
    tolist TEXT COMMENT '接收者列表(JSON数组)',
    roomid VARCHAR(128) COMMENT '群聊ID',
    
    -- 外部联系人
    external_userid VARCHAR(128) COMMENT '外部联系人ID',
    
    -- 消息内容
    msgtype VARCHAR(32) COMMENT '消息类型: text/image/voice/video/file/link/emotion/weapp',
    content TEXT COMMENT '消息内容(根据类型不同格式不同)',
    
    -- 原始数据
    raw_data TEXT COMMENT '原始JSON数据',
    
    -- 数据采集信息
    corp_id VARCHAR(128) COMMENT '企业微信企业ID',
    sync_time DATETIME COMMENT '同步时间',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_external_user (external_userid, event_date),
    INDEX idx_from_user (from_userid, event_date),
    INDEX idx_room (roomid, event_date),
    INDEX idx_msg_time (msg_time)
) COMMENT='企微聊天事件原始表'
PARTITION BY RANGE (TO_DAYS(event_date)) (
    PARTITION p_202410 VALUES LESS THAN (TO_DAYS('2024-11-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

#### 3.1.3 企微客户群表 (ods_wework_group_chat)

存储企微客户群信息。

```sql
CREATE TABLE ods_wework_group_chat (
    -- 主键
    chat_id VARCHAR(128) PRIMARY KEY COMMENT '群聊ID',
    
    -- 群信息
    name VARCHAR(256) COMMENT '群名称',
    owner VARCHAR(128) COMMENT '群主userid',
    notice VARCHAR(1024) COMMENT '群公告',
    
    -- 成员信息
    member_list TEXT COMMENT '群成员列表(JSON数组)',
    member_count INT COMMENT '成员数量',
    
    -- 群状态
    status TINYINT COMMENT '群状态: 0=正常/1=已解散',
    
    -- 创建时间
    create_time_ts BIGINT COMMENT '群创建时间戳',
    
    -- 原始数据
    raw_data TEXT COMMENT '原始JSON数据',
    
    -- 数据采集
    corp_id VARCHAR(128) COMMENT '企业微信企业ID',
    sync_time DATETIME COMMENT '同步时间',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_owner (owner),
    INDEX idx_status (status)
) COMMENT='企微客户群原始表';
```

---

### 3.2 CRM数据Schema

#### 3.2.1 CRM客户表 (ods_crm_account)

存储从CRM系统同步的客户企业原始数据。

```sql
CREATE TABLE ods_crm_account (
    -- 主键
    id VARCHAR(64) PRIMARY KEY COMMENT '自增ID',
    
    -- CRM标识
    crm_account_id VARCHAR(128) NOT NULL COMMENT 'CRM系统客户ID',
    
    -- 客户基本信息
    account_name VARCHAR(256) COMMENT '客户名称',
    account_name_en VARCHAR(256) COMMENT '英文名称',
    industry VARCHAR(64) COMMENT '行业',
    company_type VARCHAR(32) COMMENT '企业类型',
    employee_count INT COMMENT '员工数',
    annual_revenue DECIMAL(20,2) COMMENT '年营收',
    
    -- 地址信息
    country VARCHAR(64) COMMENT '国家',
    province VARCHAR(64) COMMENT '省份',
    city VARCHAR(64) COMMENT '城市',
    address TEXT COMMENT '详细地址',
    
    -- 客户分类
    customer_type VARCHAR(32) COMMENT '客户类型',
    customer_level VARCHAR(32) COMMENT '客户等级',
    customer_status VARCHAR(32) COMMENT '客户状态',
    
    -- 业务负责人
    owner_id VARCHAR(64) COMMENT '负责人ID',
    owner_name VARCHAR(128) COMMENT '负责人姓名',
    
    -- 业务数据
    opportunity_count INT COMMENT '商机数量',
    contract_count INT COMMENT '合同数量',
    total_amount DECIMAL(20,2) COMMENT '合同总额',
    
    -- CRM时间字段
    crm_create_time DATETIME COMMENT 'CRM创建时间',
    crm_update_time DATETIME COMMENT 'CRM更新时间',
    
    -- 原始数据
    raw_data TEXT COMMENT '原始JSON数据',
    
    -- 数据采集信息
    crm_source VARCHAR(32) COMMENT 'CRM系统来源: SALESFORCE/DYNAMICS/自建',
    sync_time DATETIME COMMENT '同步时间',
    sync_type VARCHAR(16) COMMENT '同步类型: FULL/INCR',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_crm_id (crm_account_id),
    INDEX idx_account_name (account_name),
    INDEX idx_owner (owner_id),
    INDEX idx_sync_time (sync_time)
) COMMENT='CRM客户原始表';
```

#### 3.2.2 CRM联系人表 (ods_crm_contact)

```sql
CREATE TABLE ods_crm_contact (
    -- 主键
    id VARCHAR(64) PRIMARY KEY COMMENT '自增ID',
    
    -- CRM标识
    crm_contact_id VARCHAR(128) NOT NULL COMMENT 'CRM联系人ID',
    crm_account_id VARCHAR(128) COMMENT 'CRM客户ID',
    
    -- 基本信息
    contact_name VARCHAR(128) COMMENT '姓名',
    gender VARCHAR(8) COMMENT '性别',
    
    -- 职位信息
    title VARCHAR(128) COMMENT '职位',
    department VARCHAR(256) COMMENT '部门',
    
    -- 联系方式
    mobile VARCHAR(32) COMMENT '手机号',
    email VARCHAR(128) COMMENT '邮箱',
    wechat VARCHAR(128) COMMENT '微信',
    phone VARCHAR(32) COMMENT '座机',
    
    -- 汇报关系
    report_to_id VARCHAR(128) COMMENT '上级CRM ID',
    report_to_name VARCHAR(128) COMMENT '上级姓名',
    
    -- 决策角色
    decision_role VARCHAR(32) COMMENT '决策角色',
    
    -- CRM时间字段
    crm_create_time DATETIME COMMENT 'CRM创建时间',
    crm_update_time DATETIME COMMENT 'CRM更新时间',
    
    -- 原始数据
    raw_data TEXT COMMENT '原始JSON数据',
    
    -- 数据采集
    crm_source VARCHAR(32) COMMENT 'CRM系统来源',
    sync_time DATETIME COMMENT '同步时间',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_crm_id (crm_contact_id),
    INDEX idx_account (crm_account_id),
    INDEX idx_mobile (mobile),
    INDEX idx_email (email)
) COMMENT='CRM联系人原始表';
```

#### 3.2.3 CRM活动记录表 (ods_crm_activity)

存储CRM中的客户互动活动(电话、拜访、邮件等)。

```sql
CREATE TABLE ods_crm_activity (
    -- 主键
    activity_id VARCHAR(64) PRIMARY KEY COMMENT '活动ID',
    
    -- CRM标识
    crm_activity_id VARCHAR(128) NOT NULL COMMENT 'CRM活动ID',
    crm_account_id VARCHAR(128) COMMENT '关联客户ID',
    crm_contact_id VARCHAR(128) COMMENT '关联联系人ID',
    
    -- 活动信息
    activity_type VARCHAR(32) COMMENT '活动类型: CALL(电话)/VISIT(拜访)/EMAIL(邮件)/MEETING(会议)',
    activity_subject VARCHAR(256) COMMENT '活动主题',
    activity_description TEXT COMMENT '活动描述',
    activity_result VARCHAR(128) COMMENT '活动结果',
    
    -- 时间信息
    activity_date DATE COMMENT '活动日期',
    activity_time DATETIME COMMENT '活动时间',
    duration INT COMMENT '持续时长(分钟)',
    
    -- 参与人
    owner_id VARCHAR(64) COMMENT '负责人ID',
    owner_name VARCHAR(128) COMMENT '负责人姓名',
    participants TEXT COMMENT '参与人列表(JSON)',
    
    -- 原始数据
    raw_data TEXT COMMENT '原始JSON数据',
    
    -- 数据采集
    crm_source VARCHAR(32) COMMENT 'CRM系统来源',
    sync_time DATETIME COMMENT '同步时间',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_account (crm_account_id, activity_date),
    INDEX idx_contact (crm_contact_id, activity_date),
    INDEX idx_type (activity_type, activity_date),
    INDEX idx_owner (owner_id, activity_date)
) COMMENT='CRM活动记录原始表'
PARTITION BY RANGE (TO_DAYS(activity_date)) (
    PARTITION p_202410 VALUES LESS THAN (TO_DAYS('2024-11-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

---

### 3.3 广告数据Schema

#### 3.3.1 广告投放数据表 (ods_ad_campaign)

存储广告平台的投放数据(腾讯广告、字节广告等)。

```sql
CREATE TABLE ods_ad_campaign (
    -- 主键
    record_id VARCHAR(64) PRIMARY KEY COMMENT '记录ID',
    
    -- 广告平台标识
    platform VARCHAR(32) NOT NULL COMMENT '广告平台: TENCENT(腾讯)/BYTEDANCE(字节)/BAIDU(百度)',
    account_id VARCHAR(128) COMMENT '广告账户ID',
    account_name VARCHAR(256) COMMENT '广告账户名称',
    
    -- 广告层级
    campaign_id VARCHAR(128) COMMENT '广告计划ID',
    campaign_name VARCHAR(256) COMMENT '广告计划名称',
    ad_group_id VARCHAR(128) COMMENT '广告组ID',
    ad_group_name VARCHAR(256) COMMENT '广告组名称',
    ad_id VARCHAR(128) COMMENT '广告创意ID',
    ad_name VARCHAR(256) COMMENT '广告创意名称',
    
    -- 投放信息
    targeting_type VARCHAR(32) COMMENT '定向类型: KEYWORD(关键词)/INTEREST(兴趣)/BEHAVIOR(行为)/LOOKALIKE(相似)',
    targeting_config TEXT COMMENT '定向配置(JSON)',
    
    -- 数据统计(按天)
    stat_date DATE NOT NULL COMMENT '统计日期',
    
    -- 曝光与点击
    impression_count INT DEFAULT 0 COMMENT '曝光量',
    click_count INT DEFAULT 0 COMMENT '点击量',
    ctr DECIMAL(10,4) COMMENT '点击率',
    
    -- 成本数据
    cost_amount BIGINT DEFAULT 0 COMMENT '消耗金额(分)',
    cpc BIGINT COMMENT '点击单价(分)',
    cpm BIGINT COMMENT '千次展示成本(分)',
    
    -- 转化数据
    conversion_count INT DEFAULT 0 COMMENT '转化量',
    conversion_cost BIGINT COMMENT '转化成本(分)',
    conversion_rate DECIMAL(10,4) COMMENT '转化率',
    
    -- 线索数据
    lead_count INT DEFAULT 0 COMMENT '线索量',
    lead_cost BIGINT COMMENT '线索成本(分)',
    valid_lead_count INT DEFAULT 0 COMMENT '有效线索量',
    
    -- 原始数据
    raw_data TEXT COMMENT '原始JSON数据',
    
    -- 数据采集
    sync_time DATETIME COMMENT '同步时间',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_ad_date (platform, campaign_id, ad_id, stat_date),
    INDEX idx_campaign (campaign_id, stat_date),
    INDEX idx_ad (ad_id, stat_date),
    INDEX idx_date (stat_date)
) COMMENT='广告投放数据原始表';
```

#### 3.3.2 广告线索数据表 (ods_ad_lead)

存储从广告平台获取的线索数据。

```sql
CREATE TABLE ods_ad_lead (
    -- 主键
    lead_id VARCHAR(64) PRIMARY KEY COMMENT '线索ID',
    
    -- 广告平台标识
    platform VARCHAR(32) NOT NULL COMMENT '广告平台',
    platform_lead_id VARCHAR(128) COMMENT '平台线索ID',
    
    -- 广告来源
    campaign_id VARCHAR(128) COMMENT '广告计划ID',
    campaign_name VARCHAR(256) COMMENT '广告计划名称',
    ad_id VARCHAR(128) COMMENT '广告创意ID',
    ad_name VARCHAR(256) COMMENT '广告创意名称',
    
    -- 线索信息
    company_name VARCHAR(256) COMMENT '企业名称',
    contact_name VARCHAR(128) COMMENT '联系人姓名',
    mobile VARCHAR(32) COMMENT '手机号',
    email VARCHAR(128) COMMENT '邮箱',
    position VARCHAR(128) COMMENT '职位',
    
    -- 留资表单
    form_id VARCHAR(128) COMMENT '表单ID',
    form_name VARCHAR(256) COMMENT '表单名称',
    form_data TEXT COMMENT '表单数据(JSON)',
    
    -- 线索质量
    lead_status VARCHAR(32) COMMENT '线索状态: NEW(新)/VALID(有效)/INVALID(无效)',
    lead_quality VARCHAR(32) COMMENT '线索质量: HIGH/MEDIUM/LOW',
    invalid_reason VARCHAR(256) COMMENT '无效原因',
    
    -- 时间信息
    lead_time DATETIME COMMENT '线索产生时间',
    lead_date DATE COMMENT '线索日期',
    
    -- 原始数据
    raw_data TEXT COMMENT '原始JSON数据',
    
    -- 数据采集
    sync_time DATETIME COMMENT '同步时间',
    
    -- 系统字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_platform_lead (platform, platform_lead_id),
    INDEX idx_mobile (mobile),
    INDEX idx_company (company_name),
    INDEX idx_campaign (campaign_id, lead_date),
    INDEX idx_status (lead_status)
) COMMENT='广告线索数据原始表';
```

---

## 四、数据映射规则

### 4.1 企业微信→标准模型映射

#### 4.1.1 外部联系人→联系人主表

| 目标字段(dwd_contact) | 来源字段(ods_wework_external_contact) | 映射逻辑 |
|---------------------|--------------------------------------|---------|
| cdp_id | - | 根据unionid或手机号查询或生成新ID |
| account_id | - | 根据corp_name查询或创建客户企业 |
| contact_name | name | 直接映射 |
| gender | gender | 1→MALE, 2→FEMALE, 0→UNKNOWN |
| avatar_url | avatar | 直接映射 |
| job_title | position | 直接映射 |
| department | - | 从corp_full_name中提取 |
| mobile_encrypted | remark_mobiles | 取第一个手机号,AES加密 |
| mobile_hash | remark_mobiles | 取第一个手机号,SHA256哈希 |
| relationship_status | - | 固定为 CONNECTED |
| primary_source_type | - | 固定为 WEWORK |
| primary_source_id | external_userid | 直接映射 |

**映射SQL示例**:

```sql
INSERT INTO dwd_contact (
    cdp_id,
    account_id,
    contact_name,
    gender,
    avatar_url,
    job_title,
    mobile_encrypted,
    mobile_hash,
    relationship_status,
    primary_source_type,
    primary_source_id,
    create_time
)
SELECT
    get_or_create_cdp_id(unionid, mobile) AS cdp_id,
    get_or_create_account_id(corp_name) AS account_id,
    name,
    CASE gender 
        WHEN 1 THEN 'MALE'
        WHEN 2 THEN 'FEMALE'
        ELSE 'UNKNOWN'
    END AS gender,
    avatar,
    position,
    aes_encrypt(JSON_EXTRACT(remark_mobiles, '$[0]')) AS mobile_encrypted,
    sha256(JSON_EXTRACT(remark_mobiles, '$[0]')) AS mobile_hash,
    'CONNECTED',
    'WEWORK',
    external_userid,
    NOW()
FROM ods_wework_external_contact
WHERE sync_time >= '2024-10-30 00:00:00';
```

#### 4.1.2 聊天事件→行为事件表

| 目标字段(dwd_contact_event) | 来源字段(ods_wework_chat_event) | 映射逻辑 |
|----------------------------|--------------------------------|---------|
| event_id | event_id | 直接映射 |
| event_type | msgtype | "wework_chat_" + msgtype |
| event_time | msg_time | 直接映射 |
| event_date | event_date | 直接映射 |
| cdp_id | external_userid | 查询id_mapping表获取cdp_id |
| channel | - | 固定为 WEWORK |
| event_properties | content + raw_data | 组装JSON |
| source_type | - | 固定为 WEWORK |
| source_id | msgid | 直接映射 |

---

### 4.2 CRM→标准模型映射

#### 4.2.1 CRM客户→客户企业表

| 目标字段(dwd_customer_account) | 来源字段(ods_crm_account) | 映射逻辑 |
|-------------------------------|--------------------------|---------|
| account_id | crm_account_id | 查询或生成CDP account_id |
| account_name | account_name | 直接映射 |
| industry | industry | 直接映射 |
| company_scale | employee_count | 按区间转换 |
| annual_revenue | annual_revenue | 转换为分(×100) |
| customer_type | customer_type | 映射枚举值 |
| customer_level | customer_level | 直接映射 |
| source_type | - | 固定为 CRM |
| source_id | crm_account_id | 直接映射 |

#### 4.2.2 CRM联系人→联系人主表

| 目标字段(dwd_contact) | 来源字段(ods_crm_contact) | 映射逻辑 |
|---------------------|--------------------------|---------|
| cdp_id | - | 根据mobile/email查询或生成 |
| account_id | crm_account_id | 查询account映射 |
| contact_name | contact_name | 直接映射 |
| gender | gender | MALE/FEMALE/UNKNOWN |
| job_title | title | 直接映射 |
| department | department | 直接映射 |
| mobile_encrypted | mobile | AES加密 |
| mobile_hash | mobile | SHA256哈希 |
| email_encrypted | email | AES加密 |
| email_hash | email | SHA256哈希 |
| report_to_cdp_id | report_to_id | 查询联系人映射 |
| decision_role | decision_role | 直接映射 |
| primary_source_type | - | 固定为 CRM |
| primary_source_id | crm_contact_id | 直接映射 |

#### 4.2.3 CRM活动→行为事件表

| 目标字段(dwd_contact_event) | 来源字段(ods_crm_activity) | 映射逻辑 |
|----------------------------|---------------------------|---------|
| event_id | activity_id | 直接映射 |
| event_type | activity_type | "crm_" + activity_type |
| event_time | activity_time | 直接映射 |
| event_date | activity_date | 直接映射 |
| cdp_id | crm_contact_id | 查询映射获取cdp_id |
| account_id | crm_account_id | 查询映射获取account_id |
| channel | - | 固定为 CRM |
| event_properties | activity_subject + activity_description | 组装JSON |

---

### 4.3 广告→标准模型映射

#### 4.3.1 广告线索→联系人表

广告线索需要特殊处理,因为可能是全新的潜在客户。

```sql
-- Step 1: 创建或查询客户企业
INSERT INTO dwd_customer_account (account_id, account_name, customer_type, ...)
SELECT 
    get_or_create_account_id(company_name),
    company_name,
    'LEAD',  -- 来自广告的都是潜在客户
    ...
FROM ods_ad_lead
WHERE lead_status = 'VALID';

-- Step 2: 创建或更新联系人
INSERT INTO dwd_contact (cdp_id, account_id, contact_name, mobile_encrypted, ...)
SELECT
    get_or_create_cdp_id_by_mobile(mobile),
    get_account_id_by_name(company_name),
    contact_name,
    aes_encrypt(mobile),
    ...
FROM ods_ad_lead
WHERE lead_status = 'VALID';

-- Step 3: 记录线索事件
INSERT INTO dwd_contact_event (event_id, event_type, cdp_id, event_properties, ...)
SELECT
    CONCAT('ad_lead_', lead_id),
    'ad_lead_submit',
    get_cdp_id_by_mobile(mobile),
    JSON_OBJECT(
        'platform', platform,
        'campaign_id', campaign_id,
        'campaign_name', campaign_name,
        'form_data', form_data
    ),
    ...
FROM ods_ad_lead;
```

#### 4.3.2 广告投放数据→汇总表

广告数据通常不需要映射到DWD明细层,直接在DWS汇总层使用。

```sql
-- 创建广告效果汇总表
CREATE TABLE dws_ad_performance_daily (
    stat_date DATE,
    platform VARCHAR(32),
    campaign_id VARCHAR(128),
    campaign_name VARCHAR(256),
    impression_count INT,
    click_count INT,
    cost_amount BIGINT,
    lead_count INT,
    valid_lead_count INT,
    ...
) COMMENT='广告效果日汇总表';
```

---

## 五、扩展字段设计

### 5.1 扩展字段(extra_attributes)的使用

由于不同企业的业务需求差异较大,标准schema无法覆盖所有场景,因此设计了`extra_attributes`扩展字段。

#### 5.1.1 扩展字段结构

```json
{
  "custom_fields": {
    "preferred_contact_time": "14:00-16:00",
    "hobby": ["打篮球", "阅读"],
    "wechat_remark": "重点客户"
  },
  "integration_data": {
    "salesforce_sync_time": "2024-10-30 12:00:00",
    "external_system_id": "EXT_12345"
  },
  "business_metrics": {
    "last_order_amount": 50000,
    "customer_lifetime_value": 200000
  }
}
```

#### 5.1.2 扩展字段查询

MySQL 5.7+ 支持JSON字段查询:

```sql
-- 查询扩展字段
SELECT 
    contact_name,
    JSON_EXTRACT(extra_attributes, '$.custom_fields.hobby') AS hobby,
    JSON_EXTRACT(extra_attributes, '$.business_metrics.customer_lifetime_value') AS clv
FROM dwd_contact
WHERE JSON_EXTRACT(extra_attributes, '$.custom_fields.preferred_contact_time') IS NOT NULL;

-- 更新扩展字段
UPDATE dwd_contact
SET extra_attributes = JSON_SET(
    extra_attributes,
    '$.custom_fields.wechat_remark',
    'VIP客户'
)
WHERE cdp_id = 'CDP_123456';
```

### 5.2 行业特定字段扩展示例

#### 5.2.1 金融行业扩展

```json
{
  "financial_info": {
    "risk_level": "LOW",
    "credit_score": 750,
    "aum": 5000000,
    "investment_preference": ["股票", "基金"],
    "kyc_status": "PASSED"
  }
}
```

#### 5.2.2 教育行业扩展

```json
{
  "education_info": {
    "student_count": 500,
    "school_type": "K12",
    "has_online_course": true,
    "teaching_platform": "钉钉"
  }
}
```

#### 5.2.3 制造业扩展

```json
{
  "manufacturing_info": {
    "production_capacity": "10000台/月",
    "main_products": ["工业机器人", "自动化设备"],
    "certification": ["ISO9001", "ISO14001"],
    "factory_count": 3
  }
}
```

---

## 六、Schema设计最佳实践

### 6.1 数据质量保障

1. **必填字段约束**: 关键字段设置NOT NULL约束
2. **唯一性约束**: 为业务唯一键建立UNIQUE索引
3. **外键关系**: 逻辑外键(不建物理外键,保证性能)
4. **默认值**: 为状态字段设置合理的默认值
5. **枚举值校验**: 应用层校验枚举值的合法性

### 6.2 性能优化

1. **索引设计**: 为常用查询条件建立合适的索引
2. **分区表**: 大表按日期分区(如事件表)
3. **冷热分离**: 历史数据归档,保持活跃表精简
4. **宽表设计**: DWS层适当做宽表,减少JOIN
5. **缓存策略**: 热点数据缓存到Redis

### 6.3 数据安全

1. **敏感字段加密**: 手机号、邮箱等AES加密存储
2. **哈希索引**: 用哈希值做匹配和查询
3. **权限控制**: 数据库层+应用层双重权限控制
4. **审计日志**: 记录敏感数据的访问和修改
5. **数据脱敏**: 非生产环境使用脱敏数据

### 6.4 可扩展性

1. **预留扩展字段**: 每个核心表预留extra_attributes
2. **版本管理**: Schema变更需要版本管理
3. **兼容性**: 新增字段,不删除已有字段
4. **平滑迁移**: 大表结构变更采用影子表方式

---

## 七、总结

### 7.1 Schema设计关键点

1. **分层清晰**: ODS保持原始/DWD标准统一/DWS面向应用
2. **OneID核心**: 所有数据以CDP统一ID为中心
3. **组织关系**: 支持复杂的企业组织架构
4. **数据溯源**: 记录数据来源,支持追溯
5. **可扩展性**: 通过JSON字段支持灵活扩展

### 7.2 下一步工作

1. **数据采集开发**: 开发各数据源的采集Connector
2. **ID-Mapping实现**: 实现OneID匹配算法
3. **ETL开发**: 开发ODS→DWD→DWS的数据加工流程
4. **数据质量监控**: 建立数据质量监控体系
5. **性能优化**: 根据实际数据量进行性能调优

---

**文档版本**: v1.0  
**最后更新**: 2024-10-30  
**作者**: CDP数据团队
