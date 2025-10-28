# 企业级CDP（客户数据平台）设计方案

## 一、方案概述

### 1.1 背景与目标

本方案针对B2B场景下的企业级客户数据平台（CDP），核心特点是：
- **客户主体是企业**：不同于C端CDP，需要管理企业客户及其内部联系人
- **组织架构关系**：支持企业客户的组织架构管理，可追溯上下级、同事等关系
- **销售场景驱动**：销售人员通过企业微信等渠道与客户互动，需要完整的客户画像支持

### 1.2 核心能力

- **全渠道数据采集**：整合CRM、企业微信、第三方数据等多源数据
- **OneID统一身份**：打通多渠道的客户身份，形成360度客户视图
- **组织架构管理**：支持企业客户的组织层级关系，快速定位关键决策人
- **智能标签体系**：基于行为、属性、组织关系的多维标签
- **精准圈人能力**：支持基于标签、组织关系的客户筛选
- **数据安全合规**：符合个人信息保护法、数据安全法等监管要求

---

## 二、整体架构设计

### 2.1 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                       应用层 (Application)                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ 销售工作台│  │ 营销平台 │  │ BI分析   │  │ API网关  │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       服务层 (Service)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ 圈人服务 │  │ 标签服务 │  │ 画像服务 │  │ 组织服务 │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ OneID服务│  │ 埋点服务 │  │ 数据质量 │  │ 权限服务 │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     数据处理层 (Process)                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ 实时计算 │  │ 离线计算 │  │ 标签计算 │  │ 数据清洗 │    │
│  │(Flink)   │  │(Spark)   │  │          │  │          │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     数据存储层 (Storage)                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ MySQL    │  │ ES       │  │ HBase    │  │ Redis    │    │
│  │(元数据)  │  │(搜索)    │  │(宽表)    │  │(缓存)    │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │ Hive     │  │ ClickHouse│ │ Neo4j    │                  │
│  │(数据湖)  │  │(OLAP)     │ │(图数据库)│                  │
│  └──────────┘  └──────────┘  └──────────┘                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     数据采集层 (Collection)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ CRM      │  │ 企业微信 │  │ 第三方   │  │ 行为埋点 │    │
│  │ 数据同步 │  │ 事件采集 │  │ 数据接入 │  │ SDK      │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心数据流

1. **数据采集流程**：CRM/企微/第三方 → 消息队列(Kafka) → 数据清洗 → 存储
2. **OneID流程**：多源数据 → ID-Mapping → 统一客户视图
3. **标签计算流程**：原始数据 → 规则引擎 → 标签生成 → 标签存储
4. **圈人查询流程**：条件输入 → 查询引擎 → 结果集 → 导出/推送

---

## 三、数据采集平台

### 3.1 数据源接入

#### 3.1.1 CRM系统接入

**接入方式**：
- **全量同步**：定时任务(如每日凌晨)，同步客户主数据、联系人、商机等
- **增量同步**：实时或准实时(5-15分钟)，基于时间戳或CDC(Change Data Capture)
- **API接口**：提供双向API，支持CRM系统实时推送数据变更

**数据内容**：
```json
{
  "account": {
    "account_id": "A001",
    "account_name": "某科技有限公司",
    "industry": "互联网",
    "scale": "500-1000人",
    "annual_revenue": 50000000,
    "address": "北京市朝阳区",
    "created_at": "2024-01-01"
  },
  "contacts": [
    {
      "contact_id": "C001",
      "name": "张三",
      "title": "技术总监",
      "department": "技术部",
      "mobile": "13800138000",
      "email": "zhangsan@example.com",
      "report_to": "C002"
    }
  ],
  "opportunities": [...],
  "activities": [...]
}
```

#### 3.1.2 企业微信接入

**接入方式**：
- **企微API对接**：基于企业微信官方API，获取通讯录、客户、聊天记录等
- **事件订阅**：订阅企微事件(添加客户、聊天、标签变更等)，实时推送
- **侧边栏应用**：在企微侧边栏嵌入CDP能力，销售可直接查看客户画像

**采集数据类型**：
- **通讯录数据**：企业组织架构、员工信息
- **客户数据**：外部联系人、添加时间、添加来源
- **互动数据**：聊天记录(需客户授权)、会话次数、回复时长
- **群聊数据**：客户群信息、群成员、群消息
- **雷达数据**：客户浏览行为(如浏览官网链接、产品手册)

**数据格式示例**：
```json
{
  "event_type": "add_external_contact",
  "event_time": "2024-10-28 10:30:00",
  "staff_id": "sales001",
  "staff_name": "李四",
  "external_contact_id": "wx_ec_001",
  "external_name": "王五",
  "external_corp": "某集团",
  "add_way": "扫描二维码",
  "state": "campaign_001"
}
```

#### 3.1.3 第三方数据接入

**数据类型**：
- **企业征信数据**：企查查、天眼查等，企业工商信息、风险信息
- **行业数据**：行业报告、竞品信息
- **公开数据**：企业官网、招聘信息、新闻舆情

**接入方式**：
- **API调用**：定时或触发式调用第三方API
- **文件导入**：Excel/CSV批量导入
- **爬虫采集**：合规前提下的数据抓取

### 3.2 数据采集架构

```
┌─────────────────────────────────────────────────────────────┐
│                       数据源层                                │
│     CRM系统      企业微信      第三方API      行为埋点        │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     数据采集网关                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ CRM      │  │ 企微     │  │ API      │  │ SDK      │    │
│  │ Connector│  │ Connector│  │ Gateway  │  │ Collector│    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
│              数据格式转换、协议适配、限流熔断                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      消息队列 (Kafka)                         │
│    Topic: crm-data  |  wework-event  |  3rd-data  |  track   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     数据预处理层                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Flink Stream Processing                              │   │
│  │  - 数据格式标准化                                      │   │
│  │  - 数据清洗(去重、过滤、补全)                          │   │
│  │  - 数据脱敏(手机号、邮箱打码)                          │   │
│  │  - 数据校验(必填项检查、格式校验)                       │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      数据落地层                               │
│    ODS (原始数据)  →  DWD (明细数据)  →  DWS (汇总数据)       │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 数据采集SDK设计

对于官网、营销页面等场景，提供埋点SDK：

```javascript
// 初始化
CDP.init({
  appKey: 'your_app_key',
  serverUrl: 'https://cdp.example.com/track',
  autoTrack: true // 自动采集页面浏览
});

// 识别用户身份
CDP.identify({
  userId: 'contact_001',
  accountId: 'account_001',
  properties: {
    name: '张三',
    email: 'zhangsan@example.com',
    company: '某科技公司'
  }
});

// 追踪事件
CDP.track('PageView', {
  page_title: '产品介绍页',
  page_url: '/product/intro',
  referrer: 'https://google.com'
});

// 追踪企业组织信息
CDP.trackAccount({
  accountId: 'account_001',
  properties: {
    industry: '互联网',
    scale: '500-1000人'
  }
});
```

---

## 四、OneID服务（统一身份识别）

### 4.1 核心挑战

在企业级CDP中，同一个联系人可能在多个系统中有不同的标识：
- CRM中有联系人ID
- 企业微信中有external_user_id
- 官网浏览时通过cookie识别
- 第三方数据中通过手机号或邮箱标识

**OneID的目标**：将这些碎片化的身份统一为唯一的CDP ID，构建完整客户视图。

### 4.2 ID-Mapping策略

#### 4.2.1 匹配规则

**强匹配规则**（优先级从高到低）：
1. **手机号**：中国手机号唯一性强，作为首要匹配字段
2. **邮箱**：企业邮箱唯一性较强
3. **证件号**：如身份证（需脱敏处理）
4. **企业微信unionid**：跨应用唯一标识

**弱匹配规则**：
- 姓名 + 公司名称
- 姓名 + 部门 + 职位
- 设备ID（移动端）

#### 4.2.2 匹配算法

```sql
-- ID-Mapping表结构
CREATE TABLE cdp_id_mapping (
  cdp_id VARCHAR(64) PRIMARY KEY COMMENT 'CDP统一ID',
  source_type VARCHAR(32) COMMENT '数据源类型: CRM/WEWORK/3RD',
  source_id VARCHAR(128) COMMENT '来源系统中的ID',
  match_field VARCHAR(32) COMMENT '匹配字段: mobile/email/unionid',
  match_value VARCHAR(256) COMMENT '匹配值(已加密)',
  confidence DECIMAL(3,2) COMMENT '匹配置信度: 0.00-1.00',
  create_time TIMESTAMP,
  update_time TIMESTAMP,
  INDEX idx_source (source_type, source_id),
  INDEX idx_match (match_field, match_value)
);
```

**匹配流程**：
```
1. 新数据进入 → 提取匹配字段(手机/邮箱/unionid)
2. 查询ID-Mapping表，判断是否已存在匹配记录
3. 如果存在 → 使用已有CDP ID，更新source映射关系
4. 如果不存在 → 生成新CDP ID，创建映射记录
5. 如果有冲突(一个source_id对应多个CDP ID) → 触发人工审核或合并策略
```

### 4.3 OneID数据模型

#### 4.3.1 统一客户视图

```json
{
  "cdp_id": "CDP_000001",
  "account_info": {
    "account_id": "A001",
    "account_name": "某科技有限公司",
    "industry": "互联网",
    "scale": "500-1000人",
    "status": "客户"
  },
  "contact_info": {
    "name": "张三",
    "mobile": "138****8000",
    "email": "zha***@example.com",
    "title": "技术总监",
    "department": "技术部",
    "level": "决策者"
  },
  "org_relationship": {
    "report_to_cdp_id": "CDP_000002",
    "report_to_name": "李总",
    "direct_reports": ["CDP_000003", "CDP_000004"],
    "org_path": "/某科技/技术部/研发中心"
  },
  "source_ids": {
    "crm_contact_id": "C001",
    "wework_external_id": "wx_ec_001",
    "third_party_id": "TP_001"
  },
  "tags": ["高意向", "技术决策人", "关注AI产品"],
  "last_active_time": "2024-10-28 10:30:00",
  "create_time": "2024-01-01 00:00:00"
}
```

### 4.4 OneID服务API

```java
// OneID服务接口
public interface OneIdService {
    
    /**
     * 根据来源ID获取CDP ID
     */
    String getCdpId(String sourceType, String sourceId);
    
    /**
     * ID绑定/合并
     */
    void bindId(String sourceType, String sourceId, 
                String matchField, String matchValue);
    
    /**
     * 获取统一客户视图
     */
    UnifiedCustomerView getCustomerView(String cdpId);
    
    /**
     * ID合并(当发现多个CDP ID实际是同一人)
     */
    void mergeIds(String targetCdpId, List<String> sourceCdpIds);
}
```

---

## 五、组织架构模型设计

### 5.1 数据模型

企业级CDP的核心特色在于**组织架构关系**，需要建立两层模型：

#### 5.1.1 客户企业层级模型

```sql
-- 客户企业表
CREATE TABLE cdp_account (
  account_id VARCHAR(64) PRIMARY KEY,
  account_name VARCHAR(256) NOT NULL,
  parent_account_id VARCHAR(64) COMMENT '母公司ID(集团型企业)',
  account_level INT COMMENT '企业层级: 1-集团 2-子公司 3-分公司',
  industry VARCHAR(64),
  scale VARCHAR(32),
  annual_revenue BIGINT,
  status VARCHAR(32) COMMENT '客户状态: 潜在客户/商机/成交客户/流失',
  owner_staff_id VARCHAR(64) COMMENT '客户负责人',
  create_time TIMESTAMP,
  update_time TIMESTAMP,
  INDEX idx_parent (parent_account_id)
);

-- 客户企业组织架构表
CREATE TABLE cdp_account_organization (
  org_id VARCHAR(64) PRIMARY KEY,
  account_id VARCHAR(64) NOT NULL,
  org_name VARCHAR(256) COMMENT '部门/团队名称',
  parent_org_id VARCHAR(64) COMMENT '上级组织',
  org_level INT COMMENT '组织层级',
  org_type VARCHAR(32) COMMENT '组织类型: 部门/中心/小组',
  org_path VARCHAR(512) COMMENT '组织路径: /公司/技术部/研发中心',
  leader_cdp_id VARCHAR(64) COMMENT '负责人CDP ID',
  create_time TIMESTAMP,
  INDEX idx_account (account_id),
  INDEX idx_parent (parent_org_id),
  INDEX idx_path (org_path)
);
```

#### 5.1.2 联系人组织关系模型

```sql
-- 联系人主表
CREATE TABLE cdp_contact (
  cdp_id VARCHAR(64) PRIMARY KEY,
  account_id VARCHAR(64) NOT NULL,
  name VARCHAR(128),
  mobile_encrypted VARCHAR(256),
  email_encrypted VARCHAR(256),
  title VARCHAR(128) COMMENT '职位',
  level VARCHAR(32) COMMENT '职级: 员工/主管/经理/总监/VP/CXO',
  seniority VARCHAR(32) COMMENT '资历: 初级/中级/高级/专家',
  create_time TIMESTAMP,
  update_time TIMESTAMP,
  INDEX idx_account (account_id)
);

-- 联系人组织关系表
CREATE TABLE cdp_contact_org_relation (
  relation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cdp_id VARCHAR(64) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  org_id VARCHAR(64) COMMENT '所属组织',
  report_to_cdp_id VARCHAR(64) COMMENT '直属上级CDP ID',
  is_primary TINYINT COMMENT '是否主要职位(1-是 0-兼职)',
  position_type VARCHAR(32) COMMENT '岗位类型: 正式/兼职/虚线汇报',
  start_date DATE,
  end_date DATE,
  create_time TIMESTAMP,
  INDEX idx_cdp (cdp_id),
  INDEX idx_org (org_id),
  INDEX idx_report (report_to_cdp_id)
);
```

### 5.2 组织关系查询能力

基于上述模型，提供强大的组织关系查询能力：

#### 5.2.1 核心查询场景

```java
public interface OrganizationService {
    
    /**
     * 查询某人的直属上级
     */
    ContactInfo getDirectManager(String cdpId);
    
    /**
     * 查询某人的所有上级(向上追溯到CEO)
     */
    List<ContactInfo> getAllManagers(String cdpId);
    
    /**
     * 查询某人的直属下属
     */
    List<ContactInfo> getDirectReports(String cdpId);
    
    /**
     * 查询某人的所有下属(递归查询所有层级)
     */
    List<ContactInfo> getAllReports(String cdpId);
    
    /**
     * 查询某个部门的所有成员
     */
    List<ContactInfo> getOrgMembers(String orgId, boolean includeSubOrg);
    
    /**
     * 查询两个人的共同上级(最近共同上级)
     */
    ContactInfo getCommonManager(String cdpId1, String cdpId2);
    
    /**
     * 查询决策链(找到某个部门的决策层)
     */
    List<ContactInfo> getDecisionChain(String orgId);
}
```

#### 5.2.2 图数据库优化

对于复杂的组织关系查询，建议使用**Neo4j图数据库**：

```cypher
// 创建联系人节点
CREATE (c:Contact {
  cdp_id: 'CDP_000001',
  name: '张三',
  title: '技术总监',
  account_id: 'A001'
})

// 创建组织节点
CREATE (o:Organization {
  org_id: 'ORG_001',
  org_name: '技术部',
  account_id: 'A001'
})

// 创建汇报关系
CREATE (c1:Contact)-[:REPORT_TO]->(c2:Contact)

// 创建所属关系
CREATE (c:Contact)-[:BELONG_TO]->(o:Organization)

// 查询某人的所有上级(向上追溯)
MATCH path = (c:Contact {cdp_id: 'CDP_000001'})-[:REPORT_TO*]->(manager)
RETURN manager

// 查询某人的所有下属(向下追溯)
MATCH path = (c:Contact {cdp_id: 'CDP_000002'})<-[:REPORT_TO*]-(subordinate)
RETURN subordinate

// 查询两人的共同上级
MATCH (c1:Contact {cdp_id: 'CDP_000001'})-[:REPORT_TO*]->(common)<-[:REPORT_TO*]-(c2:Contact {cdp_id: 'CDP_000003'})
RETURN common
ORDER BY length(path) ASC
LIMIT 1
```

### 5.3 组织架构可视化

在销售工作台中，提供组织架构图可视化能力：

```
                     [CEO - 王总]
                          |
        +-----------------+-----------------+
        |                                   |
   [技术VP - 李总]                    [销售VP - 刘总]
        |                                   |
   +----+----+                         +----+----+
   |         |                         |         |
[技术总监]  [产品总监]              [大客户总监] [渠道总监]
 张三       周六                      赵七        孙八
```

**前端实现**：使用D3.js、AntV G6等图可视化库，动态渲染组织树。

---

## 六、数据建模与存储

### 6.1 数据分层架构

采用标准的数仓分层架构：

```
ODS (Operational Data Store) - 原始数据层
  ↓
DWD (Data Warehouse Detail) - 明细数据层
  ↓
DWS (Data Warehouse Summary) - 汇总数据层
  ↓
ADS (Application Data Service) - 应用数据层
```

### 6.2 核心数据模型

#### 6.2.1 客户企业主题

```sql
-- DWD: 客户企业明细表
CREATE TABLE dwd_account_info (
  account_id VARCHAR(64) PRIMARY KEY,
  account_name VARCHAR(256),
  industry VARCHAR(64),
  scale VARCHAR(32),
  annual_revenue BIGINT,
  status VARCHAR(32),
  owner_staff_id VARCHAR(64),
  first_contact_date DATE,
  last_contact_date DATE,
  total_opportunities INT,
  total_revenue BIGINT,
  data_date DATE,
  -- 扩展字段
  ext_fields JSON
);

-- DWS: 客户企业汇总表
CREATE TABLE dws_account_summary (
  account_id VARCHAR(64),
  stat_date DATE,
  contact_count INT COMMENT '联系人数量',
  interaction_count INT COMMENT '互动次数',
  wework_chat_count INT COMMENT '企微聊天次数',
  email_count INT COMMENT '邮件往来次数',
  meeting_count INT COMMENT '会议次数',
  opportunity_count INT COMMENT '商机数量',
  opportunity_amount BIGINT COMMENT '商机金额',
  last_7d_active_days INT COMMENT '近7天活跃天数',
  last_30d_active_days INT COMMENT '近30天活跃天数',
  PRIMARY KEY (account_id, stat_date)
);
```

#### 6.2.2 联系人主题

```sql
-- DWD: 联系人明细表
CREATE TABLE dwd_contact_info (
  cdp_id VARCHAR(64) PRIMARY KEY,
  account_id VARCHAR(64),
  name VARCHAR(128),
  title VARCHAR(128),
  department VARCHAR(128),
  level VARCHAR(32),
  report_to_cdp_id VARCHAR(64),
  mobile_encrypted VARCHAR(256),
  email_encrypted VARCHAR(256),
  wework_external_id VARCHAR(128),
  first_add_time TIMESTAMP,
  last_active_time TIMESTAMP,
  data_date DATE
);

-- DWS: 联系人行为汇总表
CREATE TABLE dws_contact_behavior (
  cdp_id VARCHAR(64),
  stat_date DATE,
  page_view_count INT COMMENT '页面浏览次数',
  wework_chat_count INT COMMENT '企微聊天次数',
  wework_reply_count INT COMMENT '企微回复次数',
  avg_reply_minutes INT COMMENT '平均回复时长(分钟)',
  email_open_count INT COMMENT '邮件打开次数',
  email_click_count INT COMMENT '邮件点击次数',
  meeting_count INT COMMENT '会议次数',
  document_view_count INT COMMENT '文档查看次数',
  total_interaction_count INT COMMENT '总互动次数',
  last_interaction_time TIMESTAMP,
  PRIMARY KEY (cdp_id, stat_date)
);
```

#### 6.2.3 行为事件主题

```sql
-- DWD: 行为事件明细表
CREATE TABLE dwd_behavior_event (
  event_id VARCHAR(64) PRIMARY KEY,
  event_type VARCHAR(64) COMMENT '事件类型: page_view/wework_chat/email_open',
  cdp_id VARCHAR(64) COMMENT '联系人CDP ID',
  account_id VARCHAR(64) COMMENT '客户企业ID',
  event_time TIMESTAMP,
  event_properties JSON COMMENT '事件属性',
  source VARCHAR(32) COMMENT '数据来源: WEWORK/CRM/WEB',
  data_date DATE,
  INDEX idx_cdp_time (cdp_id, event_time),
  INDEX idx_account_time (account_id, event_time),
  INDEX idx_type_time (event_type, event_time)
) PARTITION BY RANGE COLUMNS(data_date);
```

### 6.3 存储选型

| 数据类型 | 存储方案 | 说明 |
|---------|---------|------|
| 元数据 | MySQL | 账户信息、联系人信息、标签配置等 |
| 行为事件 | ClickHouse | 海量事件数据，支持OLAP分析 |
| 实时画像 | HBase/Redis | 宽表存储，支持快速查询 |
| 组织关系 | Neo4j | 图数据库，支持复杂关系查询 |
| 搜索 | Elasticsearch | 联系人搜索、企业搜索 |
| 数据湖 | Hive/Iceberg | 原始数据长期存储 |
| 缓存 | Redis | 热点数据缓存，提升查询性能 |

---

## 七、标签体系设计

### 7.1 标签分类

企业级CDP的标签体系分为三大类：

#### 7.1.1 客户企业标签

| 类别 | 标签示例 |
|-----|---------|
| **基础属性** | 行业、规模、地域、上市状态、成立时间 |
| **商业属性** | 年营收、融资轮次、客户生命周期阶段 |
| **业务状态** | 潜在客户、商机阶段、成交客户、流失客户 |
| **行为特征** | 活跃度、互动频次、决策周期 |
| **意向度** | 高意向、中意向、低意向、无意向 |
| **产品偏好** | 关注AI产品、关注SaaS产品、关注定制化服务 |

#### 7.1.2 联系人标签

| 类别 | 标签示例 |
|-----|---------|
| **基础属性** | 姓名、职位、部门、职级、工作年限 |
| **决策角色** | 决策者、影响者、使用者、把关者 |
| **组织关系** | 部门负责人、核心团队成员、关键决策人 |
| **行为特征** | 活跃用户、沉默用户、高频互动、快速回复 |
| **兴趣偏好** | 关注技术细节、关注价格、关注案例、关注服务 |
| **互动渠道** | 企微活跃、邮件活跃、会议活跃 |

#### 7.1.3 组织关系标签

| 类别 | 标签示例 |
|-----|---------|
| **层级标签** | 高层领导、中层管理、基层员工 |
| **影响力标签** | 核心决策人、部门话事人、意见领袖 |
| **关系标签** | 已触达高层、已触达决策人、仅触达执行层 |
| **团队标签** | 完整决策链、缺失决策人、单点联系 |

### 7.2 标签计算引擎

#### 7.2.1 标签类型

**1. 规则类标签**（基于条件判断）
```sql
-- 示例：高意向客户
-- 规则：近30天互动次数>=10次 AND 近7天有高层互动
SELECT cdp_id
FROM dws_contact_behavior
WHERE stat_date = CURRENT_DATE - 1
  AND last_30d_interaction_count >= 10
  AND EXISTS (
    SELECT 1 FROM dwd_behavior_event
    WHERE cdp_id = dws_contact_behavior.cdp_id
      AND event_time >= CURRENT_DATE - 7
      AND contact_level IN ('VP', 'CXO')
  )
```

**2. 统计类标签**（基于聚合计算）
```sql
-- 示例：互动频次标签
-- L7D_互动次数、L30D_互动次数
SELECT 
  cdp_id,
  SUM(CASE WHEN stat_date >= CURRENT_DATE - 7 THEN total_interaction_count ELSE 0 END) as l7d_interaction,
  SUM(CASE WHEN stat_date >= CURRENT_DATE - 30 THEN total_interaction_count ELSE 0 END) as l30d_interaction
FROM dws_contact_behavior
WHERE stat_date >= CURRENT_DATE - 30
GROUP BY cdp_id
```

**3. 预测类标签**（基于机器学习模型）
```python
# 示例：成交概率预测
from sklearn.ensemble import RandomForestClassifier

# 特征工程
features = [
    'interaction_count_30d',
    'decision_maker_count',
    'has_cxo_contact',
    'response_rate',
    'meeting_count',
    'document_view_count'
]

# 模型训练
model = RandomForestClassifier()
model.fit(X_train, y_train)

# 预测
prob = model.predict_proba(X_test)
```

#### 7.2.2 标签计算流程

```
1. 标签定义 → 业务人员在标签平台配置规则
2. 标签调度 → 定时任务触发标签计算(每日/每小时/实时)
3. 数据准备 → 从DWS层读取汇总数据
4. 规则执行 → 执行SQL/Python脚本
5. 结果写入 → 写入标签结果表
6. 缓存更新 → 更新Redis缓存，供实时查询
```

### 7.3 标签存储

```sql
-- 标签配置表
CREATE TABLE cdp_tag_config (
  tag_id VARCHAR(64) PRIMARY KEY,
  tag_name VARCHAR(128),
  tag_category VARCHAR(64) COMMENT '标签分类',
  tag_type VARCHAR(32) COMMENT '标签类型: RULE/STAT/ML',
  target_type VARCHAR(32) COMMENT '目标类型: ACCOUNT/CONTACT',
  calc_logic TEXT COMMENT '计算逻辑(SQL/规则)',
  calc_frequency VARCHAR(32) COMMENT '计算频率: DAILY/HOURLY/REALTIME',
  is_active TINYINT,
  create_time TIMESTAMP
);

-- 标签结果表
CREATE TABLE cdp_tag_result (
  target_id VARCHAR(64) COMMENT '目标ID(account_id或cdp_id)',
  target_type VARCHAR(32) COMMENT 'ACCOUNT/CONTACT',
  tag_id VARCHAR(64),
  tag_value VARCHAR(512) COMMENT '标签值',
  confidence DECIMAL(3,2) COMMENT '置信度(仅预测类标签)',
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  update_time TIMESTAMP,
  PRIMARY KEY (target_id, tag_id),
  INDEX idx_tag (tag_id)
);
```

---

## 八、圈人能力设计

### 8.1 圈人场景

基于标签和组织关系的客户筛选，典型场景：

1. **精准营销**：圈选高意向的决策人，推送产品方案
2. **销售分配**：圈选新增商机客户，分配给销售跟进
3. **客户关怀**：圈选长期未互动的老客户，进行激活
4. **交叉销售**：圈选已购买A产品的客户，推荐B产品
5. **组织穿透**：圈选某客户企业的所有高层决策人

### 8.2 圈人条件

#### 8.2.1 基础条件

```json
{
  "target_type": "CONTACT",
  "conditions": [
    {
      "field": "account.industry",
      "operator": "IN",
      "value": ["互联网", "金融"]
    },
    {
      "field": "contact.level",
      "operator": "IN",
      "value": ["VP", "CXO"]
    },
    {
      "field": "tag.高意向",
      "operator": "HAS"
    },
    {
      "field": "behavior.last_30d_interaction_count",
      "operator": ">=",
      "value": 10
    }
  ],
  "logic": "AND"
}
```

#### 8.2.2 组织关系条件

```json
{
  "target_type": "CONTACT",
  "conditions": [
    {
      "field": "account.account_id",
      "operator": "=",
      "value": "A001"
    }
  ],
  "org_conditions": [
    {
      "type": "LEVEL",
      "operator": ">=",
      "value": "总监"
    },
    {
      "type": "DEPARTMENT",
      "operator": "IN",
      "value": ["技术部", "产品部"]
    },
    {
      "type": "REPORT_TO",
      "cdp_id": "CDP_000001",
      "depth": 2,
      "comment": "张三的所有下属(向下2级)"
    }
  ]
}
```

### 8.3 圈人引擎

#### 8.3.1 查询优化

对于复杂的圈人条件，采用多阶段查询：

```
Stage 1: 基础条件过滤 (MySQL/ES)
  - 行业、规模、地域等静态属性
  - 快速缩小候选集
  
Stage 2: 标签条件过滤 (Redis/HBase)
  - 查询标签结果表
  - 进一步筛选
  
Stage 3: 行为条件过滤 (ClickHouse)
  - 查询行为汇总数据
  - 满足互动频次等条件
  
Stage 4: 组织关系过滤 (Neo4j)
  - 查询组织关系图
  - 满足汇报关系等条件
  
Stage 5: 结果合并去重
  - 返回最终CDP ID列表
```

#### 8.3.2 圈人API

```java
public interface AudienceService {
    
    /**
     * 创建圈人任务
     */
    String createAudience(AudienceRequest request);
    
    /**
     * 查询圈人结果(分页)
     */
    PageResult<ContactInfo> getAudienceResult(String audienceId, int page, int size);
    
    /**
     * 导出圈人结果
     */
    String exportAudience(String audienceId, ExportFormat format);
    
    /**
     * 推送圈人结果到外部系统
     */
    void pushAudience(String audienceId, PushTarget target);
}
```

### 8.4 圈人结果应用

圈选结果可以应用到多种场景：

1. **导出**：导出为Excel/CSV，提供给业务团队
2. **推送**：推送到企业微信、邮件系统、短信平台
3. **广告投放**：同步到广告平台(如腾讯广告、字节广告)
4. **任务分配**：自动创建销售任务，分配给对应销售
5. **实时触发**：基于用户行为实时触发营销动作

---

## 九、数据安全与合规

### 9.1 合规要求

根据《个人信息保护法》、《数据安全法》等法规，CDP需要满足：

1. **数据最小化**：只采集业务必需的数据
2. **明示同意**：用户明确授权后才能采集和使用
3. **数据脱敏**：敏感数据加密存储
4. **访问控制**：基于角色的权限管理
5. **审计日志**：记录所有数据访问和操作
6. **数据删除**：用户有权要求删除个人数据

### 9.2 数据脱敏策略

#### 9.2.1 脱敏字段

| 字段类型 | 脱敏方式 | 示例 |
|---------|---------|------|
| 手机号 | 中间4位打码 | 138****8000 |
| 邮箱 | 用户名部分打码 | zha***@example.com |
| 身份证 | 中间部分打码 | 110***********1234 |
| 姓名 | 姓氏保留 | 张** |
| 地址 | 详细地址模糊化 | 北京市朝阳区*** |

#### 9.2.2 加密存储

```sql
-- 敏感字段使用AES加密存储
CREATE TABLE cdp_contact_sensitive (
  cdp_id VARCHAR(64) PRIMARY KEY,
  mobile_encrypted VARBINARY(512) COMMENT 'AES加密后的手机号',
  email_encrypted VARBINARY(512) COMMENT 'AES加密后的邮箱',
  id_card_encrypted VARBINARY(512) COMMENT 'AES加密后的身份证',
  encrypt_key_version INT COMMENT '加密密钥版本',
  update_time TIMESTAMP
);

-- 应用层解密
String mobile = AESUtil.decrypt(mobileEncrypted, secretKey);
```

### 9.3 权限控制

#### 9.3.1 RBAC模型

```sql
-- 角色表
CREATE TABLE cdp_role (
  role_id VARCHAR(64) PRIMARY KEY,
  role_name VARCHAR(128),
  role_desc TEXT,
  create_time TIMESTAMP
);

-- 权限表
CREATE TABLE cdp_permission (
  permission_id VARCHAR(64) PRIMARY KEY,
  permission_name VARCHAR(128),
  resource_type VARCHAR(64) COMMENT '资源类型: ACCOUNT/CONTACT/TAG',
  action VARCHAR(32) COMMENT '操作: READ/WRITE/DELETE/EXPORT',
  create_time TIMESTAMP
);

-- 角色权限关联表
CREATE TABLE cdp_role_permission (
  role_id VARCHAR(64),
  permission_id VARCHAR(64),
  PRIMARY KEY (role_id, permission_id)
);

-- 用户角色关联表
CREATE TABLE cdp_user_role (
  user_id VARCHAR(64),
  role_id VARCHAR(64),
  PRIMARY KEY (user_id, role_id)
);
```

#### 9.3.2 数据权限

除了功能权限，还需要控制数据范围权限：

```sql
-- 数据权限表
CREATE TABLE cdp_data_permission (
  permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(64),
  data_type VARCHAR(32) COMMENT 'ACCOUNT/CONTACT',
  filter_condition JSON COMMENT '过滤条件',
  comment TEXT,
  create_time TIMESTAMP
);

-- 示例：销售只能看到自己负责的客户
{
  "user_id": "sales_001",
  "data_type": "ACCOUNT",
  "filter_condition": {
    "owner_staff_id": "sales_001"
  }
}

-- 示例：区域经理可以看到整个区域的客户
{
  "user_id": "manager_001",
  "data_type": "ACCOUNT",
  "filter_condition": {
    "region": "华北区"
  }
}
```

### 9.4 审计日志

```sql
-- 操作审计表
CREATE TABLE cdp_audit_log (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(64),
  user_name VARCHAR(128),
  operation VARCHAR(64) COMMENT '操作类型: QUERY/EXPORT/UPDATE/DELETE',
  resource_type VARCHAR(64) COMMENT '资源类型',
  resource_id VARCHAR(64) COMMENT '资源ID',
  operation_detail JSON COMMENT '操作详情',
  ip_address VARCHAR(64),
  user_agent TEXT,
  operation_time TIMESTAMP,
  INDEX idx_user_time (user_id, operation_time),
  INDEX idx_operation_time (operation, operation_time)
) PARTITION BY RANGE COLUMNS(operation_time);
```

---

## 十、技术选型与架构

### 10.1 技术栈选型

#### 10.1.1 后端技术栈

| 技术领域 | 选型方案 | 说明 |
|---------|---------|------|
| 开发语言 | Java 11+ / Spring Boot | 成熟稳定，生态丰富 |
| 微服务框架 | Spring Cloud Alibaba | 国内主流，社区活跃 |
| API网关 | Spring Cloud Gateway | 统一入口，流量控制 |
| 注册中心 | Nacos | 服务注册与配置管理 |
| 消息队列 | Kafka | 高吞吐，支持流式处理 |
| 缓存 | Redis Cluster | 高性能缓存 |
| 关系数据库 | MySQL 8.0 | 元数据存储 |
| OLAP数据库 | ClickHouse | 行为事件分析 |
| 图数据库 | Neo4j | 组织关系查询 |
| 搜索引擎 | Elasticsearch | 全文搜索 |
| 实时计算 | Flink | 实时数据处理 |
| 离线计算 | Spark | 批量数据处理 |
| 任务调度 | DolphinScheduler | 数据任务编排 |
| 监控告警 | Prometheus + Grafana | 系统监控 |

#### 10.1.2 前端技术栈

| 技术领域 | 选型方案 |
|---------|---------|
| 框架 | React / Vue 3 |
| UI组件库 | Ant Design / Element Plus |
| 图表库 | ECharts / AntV |
| 图可视化 | AntV G6 |
| 状态管理 | Redux / Pinia |

### 10.2 部署架构

#### 10.2.1 云上部署(推荐)

```
                    [负载均衡 SLB]
                          ↓
              [API网关集群 (K8s)]
                          ↓
        +------------------+------------------+
        |                  |                  |
   [应用服务集群]    [数据服务集群]    [计算服务集群]
        |                  |                  |
        |                  |                  |
   [RDS MySQL]       [ClickHouse]        [Flink]
   [Redis]           [Elasticsearch]     [Spark]
   [Kafka]           [Neo4j]
```

**优势**：
- 弹性伸缩，按需付费
- 高可用，跨可用区容灾
- 托管服务(RDS、Kafka等)，减少运维成本

#### 10.2.2 私有化部署

适用于对数据安全要求极高的客户：

```
[硬件负载均衡 F5]
        ↓
[应用服务器集群 * 3]
        ↓
[数据库服务器 * 2 (主从)]
[缓存服务器 * 3 (集群)]
[消息队列 * 3 (集群)]
[大数据集群 * N]
```

### 10.3 核心服务设计

#### 10.3.1 服务拆分

```
cdp-gateway-service         # API网关
cdp-user-service            # 用户权限服务
cdp-collection-service      # 数据采集服务
cdp-oneid-service           # OneID服务
cdp-organization-service    # 组织架构服务
cdp-tag-service             # 标签服务
cdp-audience-service        # 圈人服务
cdp-profile-service         # 画像服务
cdp-data-quality-service    # 数据质量服务
cdp-export-service          # 数据导出服务
```

#### 10.3.2 服务通信

- **同步调用**：基于HTTP/REST，使用OpenFeign
- **异步调用**：基于Kafka消息队列
- **服务降级**：集成Sentinel，防止雪崩
- **链路追踪**：集成SkyWalking，排查问题

---

## 十一、系统实施路线图

### 11.1 Phase 1: MVP版本 (2-3个月)

**核心目标**：快速上线基础功能，验证业务价值

**功能范围**：
- 数据采集：CRM同步、企业微信接入
- OneID：基本的ID-Mapping能力
- 客户管理：客户企业、联系人基础信息管理
- 组织架构：基础的上下级关系查询
- 标签体系：10-20个核心业务标签
- 圈人能力：基于标签的简单圈人
- 销售工作台：客户列表、客户详情、组织架构图

### 11.2 Phase 2: 增强版本 (3-4个月)

**核心目标**：完善核心能力，支持更复杂的业务场景

**功能范围**：
- 数据采集：第三方数据接入、Web埋点SDK
- OneID：多源数据深度打通、ID合并策略
- 组织架构：矩阵式组织、复杂关系查询(图数据库)
- 标签体系：50+标签、自定义标签平台、标签自动更新
- 圈人能力：组织关系圈人、复杂条件圈人、圈人结果推送
- 数据分析：客户分析报表、销售漏斗、客户健康度

### 11.3 Phase 3: 智能版本 (4-6个月)

**核心目标**：引入AI能力,提升智能化水平

**功能范围**：
- 预测类标签：成交概率、流失风险、客户价值评分
- 智能推荐：下一步行动建议、推荐联系人、推荐话术
- 自然语言查询：通过自然语言描述圈人条件
- 异常检测：数据质量监控、异常行为识别
- 自动化营销：基于触发条件的自动化流程

---

## 十二、成功案例参考

### 12.1 客户画像场景

**业务场景**：销售拜访某客户前，快速了解客户全貌

**解决方案**：
1. 销售在企微侧边栏打开CDP客户画像
2. 展示客户企业基本信息(行业、规模、经营状况)
3. 展示组织架构图，标注已触达联系人
4. 展示关键决策人(CEO、CTO等)，提示是否已触达
5. 展示近期互动记录(聊天、邮件、会议)
6. 展示客户意向度、成交概率等智能标签
7. 推荐下一步行动(如"建议约见技术VP李总")

**业务价值**：
- 销售拜访准备时间从2小时缩短到30分钟
- 触达关键决策人的概率提升40%
- 商机转化率提升25%

### 12.2 组织穿透场景

**业务场景**：已经触达某客户的技术经理，希望触达其上级技术VP

**解决方案**：
1. 在CDP中查询该技术经理的组织关系
2. 系统自动展示其直属上级"技术VP-李总"
3. 展示李总的画像：职位、联系方式、近期活跃情况
4. 展示李总的关注点：曾查看过产品方案、参加过线上会议
5. 推荐触达策略：通过现有联系人引荐、或发送定向内容

**业务价值**：
- 决策链触达完整度从30%提升到70%
- 大单成交周期缩短20%

### 12.3 精准营销场景

**业务场景**：新产品发布,需要圈选目标客户进行推广

**圈人条件**：
- 行业：互联网、金融
- 规模：500人以上
- 客户阶段：商机阶段
- 联系人层级：VP/CXO
- 意向度：高意向
- 互动情况：近30天有互动
- 组织关系：已触达技术部门

**执行动作**：
- 圈选出500个目标联系人
- 通过企业微信推送产品介绍
- 邀请参加线上产品发布会
- 销售跟进重点客户

**业务价值**：
- 营销触达精准度提升60%
- 产品发布会参会率提升3倍
- 新产品试用转化率提升50%

---

## 十三、总结与展望

### 13.1 企业级CDP与C端CDP的核心差异

| 维度 | C端CDP | 企业级CDP |
|-----|--------|-----------|
| 客户主体 | 个人消费者 | 企业+联系人 |
| 数据规模 | 千万-亿级用户 | 万-百万级企业 |
| 核心关系 | 用户画像 | 组织架构关系 |
| 决策模式 | 个人决策 | 多人协同决策 |
| 生命周期 | 较短(数月) | 较长(数年) |
| 典型场景 | 精准投放、个性化推荐 | 销售赋能、组织穿透 |

### 13.2 关键成功要素

1. **数据质量**：GIGO(Garbage In, Garbage Out)，数据质量是CDP的生命线
2. **组织关系**：这是企业级CDP的核心竞争力，必须重点投入
3. **OneID精准度**：直接影响客户画像的准确性
4. **业务深度结合**：CDP不是数据仓库，必须与业务场景深度结合
5. **数据安全合规**：红线问题，必须严格遵守

### 13.3 未来演进方向

1. **实时化**：从T+1向实时CDP演进，支持秒级的数据更新
2. **智能化**：引入更多AI能力，从被动查询到主动洞察
3. **开放化**：通过API、SDK开放CDP能力，构建数据生态
4. **私有化**：支持私有化部署，满足大型企业的数据主权诉求
5. **行业化**：针对不同行业(金融、教育、制造)提供行业解决方案

---

## 附录

### 附录A：关键指标定义

| 指标名称 | 定义 | 计算公式 |
|---------|------|---------|
| 客户覆盖率 | 已录入CDP的客户占总客户的比例 | CDP客户数 / 总客户数 |
| OneID准确率 | OneID匹配正确的比例 | 正确匹配数 / 总匹配数 |
| 标签覆盖率 | 有标签的客户占总客户的比例 | 有标签客户数 / 总客户数 |
| 标签平均数 | 每个客户平均拥有的标签数 | 总标签数 / 客户数 |
| 组织完整度 | 组织架构信息完整的客户比例 | 有组织信息客户数 / 总客户数 |
| 数据新鲜度 | 数据最近更新的时间 | 当前时间 - 最后更新时间 |

### 附录B：核心API列表

```
# OneID服务
GET  /api/v1/oneid/customer/{cdpId}           # 获取客户画像
POST /api/v1/oneid/bind                       # 绑定ID
POST /api/v1/oneid/merge                      # 合并ID

# 组织架构服务
GET  /api/v1/org/manager/{cdpId}              # 查询上级
GET  /api/v1/org/reports/{cdpId}              # 查询下属
GET  /api/v1/org/tree/{accountId}             # 查询组织树

# 标签服务
GET  /api/v1/tag/list                         # 标签列表
POST /api/v1/tag/calculate                    # 计算标签
GET  /api/v1/tag/customer/{cdpId}             # 查询客户标签

# 圈人服务
POST /api/v1/audience/create                  # 创建圈人任务
GET  /api/v1/audience/{audienceId}/result     # 查询圈人结果
POST /api/v1/audience/{audienceId}/export     # 导出圈人结果
```

### 附录C：数据字典

详见独立文档《CDP数据字典v1.0》

---

**文档版本**：v1.0  
**最后更新**：2024-10-28  
**作者**：CDP项目组  
**审核人**：（待定）

---

## 问题讨论与反馈

如您对本设计方案有任何疑问或建议，请联系项目组进行讨论。

**关键待确认事项**：
1. 数据源系统的详细对接规范（CRM系统接口文档、企微API权限）
2. 组织架构的复杂度（是否存在矩阵式管理、虚线汇报等特殊情况）
3. 性能要求（并发用户数、查询响应时间SLA）
4. 预算与人力资源（开发团队规模、预算范围）
5. 上线时间要求（是否有硬性deadline）
