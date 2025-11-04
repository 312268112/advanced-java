## B2B CDP 数据表设计（字段级）

围绕实时打标、实时圈人目标，数据分层分为 RAW、ODS、DIM、FACT、CFG、META 六个域，对应的推荐存储引擎分别为 MongoDB、ClickHouse、ClickHouse、ClickHouse、PostgreSQL（或 MySQL）、PostgreSQL。Redis 作为缓存层，承载热点标签、人群与规则。以下按照层级给出表总览与字段说明，并补充表间关系及存储理由。

---

### RAW 原始层

#### 表总览

| 表名 | 主键 | 建议存储 | 存储理由 |
| --- | --- | --- | --- |
| raw_salesforce_account | id | MongoDB Replica Set | JSON 结构灵活、易于保留全量字段、支持快速重放 |
| raw_salesforce_contact | id | MongoDB | 同上 |
| raw_salesforce_lead | id | MongoDB | 同上 |
| raw_wechat_corp_customer | external_userid | MongoDB Sharded Cluster | 企微字段变化频繁，文档库更易扩展 |
| raw_wechat_official_user | openid | MongoDB | 公众号事件字段多样，文档存储友好 |
| raw_paypal_form_submission | submission_id | MongoDB | 表单随版本升级字段不同，需灵活 schema |
| raw_paypal_core_transaction | txn_id | MongoDB | 保留支付回执原文，便于稽核与重放 |
| raw_crm_vendor_account | account_id | MongoDB | 第三方 CRM 字段差异大，文档库兼容性佳 |
| raw_crm_vendor_contact | contact_id | MongoDB | 同上 |
| raw_crm_vendor_lead | lead_id | MongoDB | 同上 |

> RAW 层建议搭配 Kafka Offset 与批次信息，确保可追溯、可重放。可按来源 + 日期创建集合或分片，定期压缩归档。

#### raw_salesforce_account

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| id | string | Salesforce AccountId。 |
| source_system | string | 固定为 `salesforce`。 |
| source_env | string | 数据来源环境（prod、sandbox 等）。 |
| payload | json | 原始 Account JSON，全量字段原样存储。 |
| received_at | datetime | 接收上游事件的时间。 |
| ingested_at | datetime | 写入 RAW 的时间。 |
| kafka_topic | string | 对应 Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset，便于重放。 |
| ingestion_batch_id | string | 摄取批次 ID。 |
| processing_status | string | `success`/`failed`，记录解析状态。 |
| error_message | string | 失败原因。 |
| created_at | datetime | 记录创建时间（用于索引）。 |

#### raw_salesforce_contact

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| id | string | Salesforce ContactId。 |
| source_system | string | `salesforce`。 |
| source_env | string | prod/sandbox 等。 |
| payload | json | 原始 Contact JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入时间。 |
| kafka_topic | string | Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 创建时间。 |

#### raw_salesforce_lead

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| id | string | Salesforce LeadId。 |
| source_system | string | `salesforce`。 |
| source_env | string | prod/sandbox 等。 |
| payload | json | 原始 Lead JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入时间。 |
| kafka_topic | string | Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 创建时间。 |

#### raw_wechat_corp_customer

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| external_userid | string | 企业微信外部联系人 ID。 |
| source_system | string | `wechat_corp`。 |
| corp_id | string | 企业微信 CorpID。 |
| payload | json | 原始客户 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入时间。 |
| kafka_topic | string | Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 创建时间。 |

#### raw_wechat_official_user

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| openid | string | 公众号粉丝 OpenID。 |
| unionid | string | 微信 UnionID（可空）。 |
| source_system | string | `wechat_official`。 |
| official_account_id | string | 公众号 AppID。 |
| payload | json | 原始粉丝 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入时间。 |
| kafka_topic | string | Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 创建时间。 |

#### raw_paypal_form_submission

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| submission_id | string | 表单提交 ID。 |
| source_system | string | `paypal_form`。 |
| form_id | string | PayPal 表单 ID。 |
| payload | json | 原始表单 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入时间。 |
| kafka_topic | string | Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 创建时间。 |

#### raw_paypal_core_transaction

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| txn_id | string | PayPal 交易号。 |
| source_system | string | `paypal_core`。 |
| merchant_account | string | 商户编号。 |
| payload | json | 原始交易 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入时间。 |
| kafka_topic | string | Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 创建时间。 |

#### raw_crm_vendor_account

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| account_id | string | 第三方 CRM 客户 ID。 |
| source_system | string | `crm_vendor_x`。 |
| vendor_code | string | CRM 厂商编码。 |
| payload | json | 原始客户 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入时间。 |
| kafka_topic | string | Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 创建时间。 |

#### raw_crm_vendor_contact

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| contact_id | string | 第三方 CRM 联系人 ID。 |
| source_system | string | `crm_vendor_x`。 |
| vendor_code | string | CRM 厂商编码。 |
| payload | json | 原始联系人 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入时间。 |
| kafka_topic | string | Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 创建时间。 |

#### raw_crm_vendor_lead

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| lead_id | string | 第三方 CRM 线索 ID。 |
| source_system | string | `crm_vendor_x`。 |
| vendor_code | string | CRM 厂商编码。 |
| payload | json | 原始线索 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入时间。 |
| kafka_topic | string | Kafka 主题。 |
| kafka_partition | int | Kafka 分区。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 创建时间。 |

---

### ODS 标准化层

#### 表总览

| 表名 | 主键 | 建议存储 | 存储理由 |
| --- | --- | --- | --- |
| ods_account_snapshot | (global_id, snapshot_at) | ClickHouse MergeTree | 按时间分区、支持批量聚合与回溯 |
| ods_contact_snapshot | (global_id, snapshot_at) | ClickHouse MergeTree | 同上 |
| ods_lead_snapshot | (global_id, snapshot_at) | ClickHouse MergeTree | 同上 |
| ods_account_delta | change_id | ClickHouse MergeTree | 保存变更历史、便于差分比对 |
| ods_contact_delta | change_id | ClickHouse MergeTree | 同上 |
| ods_lead_delta | change_id | ClickHouse MergeTree | 同上 |
| ods_interaction_event | event_id | ClickHouse MergeTree | 事件量大，列存分析性能优 |
| ods_payment_transaction | txn_id | ClickHouse MergeTree | 支撑近实时交易分析 |
| ods_form_submission | submission_id | ClickHouse MergeTree | 表单行为查询频繁 |

#### ods_account_snapshot

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | CDP Account 全局 ID。 |
| source_system | string | 来源系统（salesforce、crm_vendor_x 等）。 |
| source_id | string | 来源系统对应 ID。 |
| snapshot_at | datetime | 快照生成时间（建议按小时或天分区）。 |
| account_name | string | 标准化企业名称。 |
| account_alias | string | 企业简称。 |
| industry | string | 标准行业分类。 |
| sub_industry | string | 细分行业。 |
| company_size | string | 员工规模。 |
| annual_revenue | decimal | 年营收（统一币种）。 |
| country | string | 国家。 |
| region | string | 省/州。 |
| city | string | 城市。 |
| address_struct | json | 结构化地址（街道、邮编等）。 |
| website | string | 官网域名。 |
| lifecycle_stage | string | suspect/prospect/customer/churned。 |
| customer_tier | string | 客户分级。 |
| tags | array<string> | 上游标签集合。 |
| last_modified_at | datetime | 来源系统更新时间。 |
| ingested_at | datetime | 写入时间。 |

#### ods_contact_snapshot

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | CDP Contact 全局 ID。 |
| source_system | string | 来源系统：salesforce、wechat_corp 等。 |
| source_id | string | 来源系统联系人 ID。 |
| snapshot_at | datetime | 快照时间。 |
| account_id | string | 关联 `dim_account.global_id`。 |
| full_name | string | 姓名。 |
| first_name | string | 名。 |
| last_name | string | 姓。 |
| gender | string | 性别。 |
| job_title | string | 职务。 |
| department | string | 部门。 |
| emails | array<string> | 邮箱集合。 |
| phones | array<string> | 电话集合。 |
| lead_source | string | 渠道来源。 |
| lifecycle_stage | string | 生命周期阶段。 |
| tags | array<string> | 标签集合。 |
| last_active_at | datetime | 最近互动时间。 |
| last_modified_at | datetime | 来源系统更新时间。 |
| ingested_at | datetime | 写入时间。 |

#### ods_lead_snapshot

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | CDP Lead 全局 ID。 |
| source_system | string | 来源系统：salesforce、paypal_form、crm_vendor_x 等。 |
| source_id | string | 来源线索 ID。 |
| snapshot_at | datetime | 快照时间。 |
| account_id | string | 已识别时关联的 Account global_id。 |
| contact_id | string | 若已转化，关联 Contact global_id。 |
| lead_name | string | 线索姓名/标题。 |
| company | string | 公司名称。 |
| email | string | 邮箱。 |
| phone | string | 电话。 |
| job_title | string | 职位。 |
| lead_status | string | new/working/qualified 等。 |
| score | decimal | 线索评分。 |
| intent_level | string | low/medium/high。 |
| lead_source | string | 渠道来源。 |
| campaign_id | string | 活动 ID（关联 `dim_campaign`）。 |
| owner_id | string | 负责人（关联 `dim_user`）。 |
| tags | array<string> | 标签集合。 |
| last_activity_at | datetime | 最近跟进时间。 |
| last_modified_at | datetime | 来源系统更新时间。 |
| ingested_at | datetime | 写入时间。 |

#### ods_account_delta

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| change_id | string | 变更记录 ID。 |
| global_id | string | 关联 Account global_id。 |
| source_system | string | 来源系统。 |
| source_id | string | 来源 ID。 |
| change_type | string | `insert`/`update`/`delete`。 |
| change_at | datetime | 变更发生时间。 |
| before_json | json | 变更前数据。 |
| after_json | json | 变更后数据。 |
| diff | json | 字段差异摘要。 |
| processed_at | datetime | 记录写入时间。 |

`ods_contact_delta`、`ods_lead_delta` 字段结构与 `ods_account_delta` 相同，分别记录联系人、线索的差异。

#### ods_interaction_event

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| event_id | string | 事件 ID。 |
| source_system | string | 数据来源（email、wechat_corp 等）。 |
| entity_type | string | `account`/`contact`/`lead`。 |
| entity_id | string | 关联实体 global_id。 |
| channel | string | 互动渠道。 |
| action | string | 行为类型（open、click、message、visit 等）。 |
| payload | json | 标准化后的事件内容。 |
| occurred_at | datetime | 行为发生时间。 |
| received_at | datetime | 接收到事件时间。 |
| ingested_at | datetime | 写入时间。 |
| rule_triggered | array<string> | 命中的标签/规则 ID。 |

#### ods_payment_transaction

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| txn_id | string | 交易号。 |
| source_system | string | `paypal_core` 等。 |
| account_id | string | 关联 Account global_id（可空）。 |
| contact_id | string | 关联 Contact global_id（可空）。 |
| amount | decimal | 交易金额。 |
| currency | string | 币种。 |
| status | string | pending/success/failed/refunded 等。 |
| payment_method | string | 支付方式。 |
| occurred_at | datetime | 交易时间。 |
| captured_at | datetime | 捕获/结算时间。 |
| raw_payload | json | 标准化前的关键字段。 |
| ingested_at | datetime | 写入时间。 |

#### ods_form_submission

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| submission_id | string | 表单提交 ID。 |
| source_system | string | `paypal_form`、`crm_vendor_x` 等。 |
| form_id | string | 表单标识。 |
| account_id | string | 关联 Account global_id（可空）。 |
| contact_id | string | 关联 Contact global_id（可空）。 |
| lead_id | string | 关联 Lead global_id（可空）。 |
| submitted_at | datetime | 提交时间。 |
| source_url | string | 来源 URL。 |
| referrer | string | 引荐来源。 |
| answers_json | json | 标准化答案。 |
| ip_address | string | 提交 IP。 |
| user_agent | string | 浏览器 UA。 |
| ingested_at | datetime | 写入时间。 |

---

### DIM 维度层

#### 表总览

| 表名 | 主键 | 建议存储 | 存储理由 |
| --- | --- | --- | --- |
| dim_account | global_id | ClickHouse ReplacingMergeTree | 主数据更新少、查询多，需与事实表高效 Join |
| dim_contact | global_id | ClickHouse ReplacingMergeTree | 同上 |
| dim_lead | global_id | ClickHouse ReplacingMergeTree | 同上 |
| dim_channel | channel_id | PostgreSQL | 渠道字典规模小，需事务保障 |
| dim_tag | tag_id | PostgreSQL | 标签字典涉及审批、权限 |
| dim_campaign | campaign_id | PostgreSQL | 活动主数据需频繁修改 |
| dim_user | user_id | PostgreSQL | 人员信息需与权限系统对齐 |
| dim_time | date_key | ClickHouse | 时间维度常用于事实 Join |

#### dim_account

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | 主键，Account 全局 ID。 |
| master_source | string | 主数据来源（salesforce 等）。 |
| account_name | string | 企业名称（主视图）。 |
| account_alias | string | 企业简称。 |
| industry | string | 行业。 |
| sub_industry | string | 细分行业。 |
| company_size | string | 员工规模。 |
| annual_revenue | decimal | 年营收。 |
| lifecycle_stage | string | suspect/prospect/customer/churned。 |
| customer_tier | string | 客户分级。 |
| region | string | 省/州。 |
| city | string | 城市。 |
| website | string | 官网。 |
| owner_user_id | string | 负责人（关联 dim_user）。 |
| tags | array<string> | 标签集合。 |
| created_at | datetime | 首次写入时间。 |
| updated_at | datetime | 最近更新时间。 |

#### dim_contact

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | 主键，Contact 全局 ID。 |
| account_id | string | 关联 Account global_id。 |
| master_source | string | 主数据来源。 |
| full_name | string | 全名。 |
| first_name | string | 名。 |
| last_name | string | 姓。 |
| gender | string | 性别。 |
| job_title | string | 职务。 |
| department | string | 部门。 |
| emails | array<string> | 邮箱集合。 |
| phones | array<string> | 电话集合。 |
| lifecycle_stage | string | 联系人阶段。 |
| lead_source | string | 渠道来源。 |
| owner_user_id | string | 负责人。 |
| tags | array<string> | 标签集合。 |
| created_at | datetime | 首次写入时间。 |
| updated_at | datetime | 最近更新时间。 |

#### dim_lead

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | 主键，Lead 全局 ID。 |
| account_id | string | 关联 Account global_id（可空）。 |
| contact_id | string | 关联 Contact global_id（可空）。 |
| master_source | string | 主数据来源。 |
| lead_name | string | 线索名称。 |
| company | string | 公司名称。 |
| email | string | 邮箱。 |
| phone | string | 电话。 |
| job_title | string | 职位。 |
| lead_status | string | 状态。 |
| score | decimal | 评分。 |
| intent_level | string | 意向程度。 |
| lead_source | string | 渠道来源。 |
| owner_user_id | string | 负责人。 |
| tags | array<string> | 标签集合。 |
| created_at | datetime | 首次写入时间。 |
| updated_at | datetime | 最近更新时间。 |

#### dim_channel

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| channel_id | string | 主键。 |
| channel_code | string | 渠道编码。 |
| channel_name | string | 渠道名称。 |
| channel_type | string | 类型（owned/paid/earned）。 |
| description | string | 备注。 |
| status | string | 启用状态。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### dim_tag

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| tag_id | string | 主键。 |
| tag_code | string | 标签编码（唯一）。 |
| tag_name | string | 标签名称。 |
| category | string | 类别（属性/行为/预测等）。 |
| data_type | string | 数据类型（string/int/decimal/bool/datetime/list）。 |
| sensitivity_level | string | 敏感等级。 |
| description | string | 标签说明。 |
| owner_user_id | string | 负责人。 |
| is_active | boolean | 是否启用。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### dim_campaign

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| campaign_id | string | 主键。 |
| campaign_code | string | 活动编码。 |
| campaign_name | string | 活动名称。 |
| source_system | string | 上游系统。 |
| status | string | 活动状态。 |
| channel_id | string | 关联渠道。 |
| start_at | datetime | 开始时间。 |
| end_at | datetime | 结束时间。 |
| budget | decimal | 预算。 |
| owner_user_id | string | 负责人。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### dim_user

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| user_id | string | 主键。 |
| username | string | 登录名。 |
| display_name | string | 显示名称。 |
| role | string | 角色（运营、销售、管理员等）。 |
| department | string | 部门。 |
| email | string | 邮箱。 |
| phone | string | 电话。 |
| status | string | 启用状态。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### dim_time

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| date_key | int | 主键，格式 YYYYMMDD。 |
| date_value | date | 日期。 |
| day_of_week | int | 周几（1-7）。 |
| week_of_year | int | 第几周。 |
| month | int | 月份。 |
| quarter | int | 季度。 |
| year | int | 年份。 |
| is_weekend | boolean | 是否周末。 |
| is_holiday | boolean | 是否节假日。 |
| created_at | datetime | 创建时间。 |

---

### FACT 事实层

#### 表总览

| 表名 | 主键/唯一键 | 建议存储 | 存储理由 |
| --- | --- | --- | --- |
| fact_interaction | event_id | ClickHouse MergeTree | 互动事件量大，需多维筛选 |
| fact_payment | txn_id | ClickHouse MergeTree | 金融交易分析与回溯 |
| fact_form_submission | submission_id | ClickHouse MergeTree | 表单行为分析 |
| fact_tag_assignment | record_id | ClickHouse MergeTree | 标签历史留痕，高吞吐写入 |
| fact_tag_snapshot | (entity_type, entity_id, tag_id) | ClickHouse ReplacingMergeTree | 保持最新标签快照，支持去重更新 |
| fact_audience_membership | (audience_id, entity_type, entity_id) | ClickHouse MergeTree | 人群成员明细，可增量更新 |
| fact_audience_trigger | trigger_id | ClickHouse MergeTree | 触达日志，需追踪发送状态 |
| fact_lead_state_transition | event_id | ClickHouse MergeTree | Lead 漏斗分析 |
| fact_contact_lifecycle_transition | event_id | ClickHouse MergeTree | 联系人生命周期分析 |
| fact_account_health_score | (record_date, account_id) | ClickHouse MergeTree | 每日健康度指标 |

#### fact_interaction

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| event_id | string | 主键。 |
| entity_type | string | `account`/`contact`/`lead`。 |
| entity_id | string | 关联实体 global_id。 |
| channel_id | string | 关联 `dim_channel.channel_id`。 |
| action | string | 行为类型（open/click/message 等）。 |
| occurred_at | datetime | 行为发生时间。 |
| payload_digest | string | 内容摘要或哈希。 |
| metadata | json | 扩展属性（如消息 ID、按钮 ID）。 |
| rule_triggered | array<string> | 命中的规则/标签。 |
| ingested_at | datetime | 写入时间。 |

#### fact_payment

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| txn_id | string | 主键。 |
| account_id | string | 关联 Account global_id。 |
| contact_id | string | 关联 Contact global_id（付款人）。 |
| amount | decimal | 金额。 |
| currency | string | 币种。 |
| status | string | 状态。 |
| payment_method | string | 支付方式。 |
| occurred_at | datetime | 交易时间。 |
| captured_at | datetime | 结算时间。 |
| source_system | string | 数据来源。 |
| ingestion_batch_id | string | 摄取批次。 |
| created_at | datetime | 记录创建时间。 |

#### fact_form_submission

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| submission_id | string | 主键。 |
| account_id | string | 关联 Account global_id（可空）。 |
| contact_id | string | 关联 Contact global_id（可空）。 |
| lead_id | string | 关联 Lead global_id（可空）。 |
| form_id | string | 表单 ID。 |
| source_system | string | 来源系统。 |
| submitted_at | datetime | 提交时间。 |
| source_url | string | 来源 URL。 |
| referrer | string | 引荐来源。 |
| answers_hash | string | 答案哈希。 |
| answers_json | json | 答案详情。 |
| ip_address | string | IP。 |
| user_agent | string | UA。 |
| ingested_at | datetime | 写入时间。 |

#### fact_tag_assignment

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| record_id | string | 主键。 |
| tag_id | string | 关联 `dim_tag.tag_id`。 |
| entity_type | string | `account`/`contact`/`lead`。 |
| entity_id | string | 实体 global_id。 |
| tag_value | string | 标签值（原始字符串，结合 data_type 解析）。 |
| calculated_at | datetime | 计算完成时间。 |
| expires_at | datetime | 过期时间（可空）。 |
| rule_id | string | 关联 `cfg_tag_rule.rule_id`。 |
| source_system | string | 计算来源（stream/batch/manual）。 |
| quality_score | decimal | 质量评分。 |
| ingested_at | datetime | 写入时间。 |

#### fact_tag_snapshot

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| entity_type | string | 实体类型。 |
| entity_id | string | 实体 global_id。 |
| tag_id | string | 标签 ID。 |
| tag_value | string | 当前值。 |
| value_updated_at | datetime | 最近更新。 |
| confidence_score | decimal | 可信度评分。 |
| source_rule_id | string | 最后一次计算的规则 ID。 |
| last_calculated_by | string | 任务 ID。 |
| updated_at | datetime | 记录更新时间。 |

#### fact_audience_membership

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| audience_id | string | 人群 ID。 |
| entity_type | string | 实体类型。 |
| entity_id | string | 实体 global_id。 |
| membership_status | string | `active`/`removed`。 |
| joined_at | datetime | 加入时间。 |
| left_at | datetime | 移出时间（可空）。 |
| source_version_id | string | 关联 `cfg_audience_version.version_id`。 |
| calculation_job_id | string | 任务 ID。 |
| ingested_at | datetime | 写入时间。 |

#### fact_audience_trigger

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| trigger_id | string | 主键。 |
| audience_id | string | 人群 ID。 |
| entity_type | string | 实体类型。 |
| entity_id | string | 实体 global_id。 |
| trigger_channel | string | 触达渠道。 |
| triggered_at | datetime | 触发时间。 |
| status | string | `pending`/`sent`/`failed`。 |
| trace_id | string | 下游追踪 ID。 |
| payload | json | 推送内容。 |
| ingested_at | datetime | 写入时间。 |

#### fact_lead_state_transition

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| event_id | string | 主键。 |
| lead_id | string | 关联 Lead global_id。 |
| prev_status | string | 变更前状态。 |
| new_status | string | 变更后状态。 |
| changed_at | datetime | 变更时间。 |
| operator_id | string | 操作人（关联 dim_user）。 |
| reason | string | 变更原因。 |
| source_system | string | 来源系统。 |
| ingested_at | datetime | 写入时间。 |

#### fact_contact_lifecycle_transition

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| event_id | string | 主键。 |
| contact_id | string | 关联 Contact global_id。 |
| prev_stage | string | 原生命周期阶段。 |
| new_stage | string | 新生命周期阶段。 |
| changed_at | datetime | 变更时间。 |
| operator_id | string | 操作人。 |
| source_system | string | 来源系统。 |
| ingested_at | datetime | 写入时间。 |

#### fact_account_health_score

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| record_date | date | 统计日期。 |
| account_id | string | 关联 Account global_id。 |
| engagement_score | decimal | 互动评分。 |
| churn_risk_level | string | 流失风险等级。 |
| health_component_json | json | 评分构成明细。 |
| computed_at | datetime | 计算时间。 |
| calculation_job_id | string | 任务 ID。 |
| ingested_at | datetime | 写入时间。 |

---

### CFG 标签 / 圈人配置层

#### 表总览

| 表名 | 主键 | 建议存储 | 存储理由 |
| --- | --- | --- | --- |
| cfg_tag_dictionary | tag_id | PostgreSQL（B-Tree 索引） | 标签主数据需强事务、支持审批与权限 |
| cfg_tag_rule | rule_id | PostgreSQL | 规则 DSL 需版本与锁管理 |
| cfg_tag_rule_version | rule_version_id | PostgreSQL | 保存历史版本，便于回滚与审计 |
| cfg_tag_dependency | (tag_id, dependent_tag_id) | PostgreSQL | 小表多次关联，事务操作简洁 |
| cfg_tag_job_status | job_id | PostgreSQL | 任务运行状态需可靠更新 |
| cfg_audience_definition | audience_id | PostgreSQL | 人群定义需审批、授权 |
| cfg_audience_version | version_id | PostgreSQL | 记录版本历史 |
| cfg_audience_job_status | job_id | PostgreSQL | 圈人任务状态追踪 |
| cfg_channel_mapping | channel_id | PostgreSQL | 存储渠道鉴权信息需安全控制 |
| cfg_webhook_subscription | webhook_id | PostgreSQL | 外部订阅配置需事务保障 |

#### cfg_tag_dictionary

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| tag_id | string | 主键。 |
| tag_code | string | 标签编码，唯一。 |
| tag_name | string | 标签名称。 |
| category | string | 标签分类。 |
| data_type | string | 标签值类型。 |
| value_domain | json | 合法值范围（可空）。 |
| description | text | 标签说明。 |
| owner_user_id | string | 负责人（关联 dim_user）。 |
| is_sensitive | boolean | 是否敏感。 |
| is_active | boolean | 是否启用。 |
| created_by | string | 创建人。 |
| created_at | datetime | 创建时间。 |
| updated_by | string | 最近修改人。 |
| updated_at | datetime | 更新时间。 |

#### cfg_tag_rule

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| rule_id | string | 主键。 |
| tag_id | string | 关联标签。 |
| rule_type | string | `streaming`/`batch`/`manual`。 |
| rule_dsl_json | json | 规则 DSL/SQL 模板。 |
| source_priority | int | 数据源优先级。 |
| refresh_strategy | string | `realtime`/`hourly`/`daily`/`on_demand`。 |
| schedule_cron | string | 定时表达式（可空）。 |
| status | string | `draft`/`approved`/`online`/`offline`。 |
| approver_id | string | 审批人。 |
| approval_at | datetime | 审批时间。 |
| created_by | string | 创建人。 |
| created_at | datetime | 创建时间。 |
| updated_by | string | 修改人。 |
| updated_at | datetime | 更新时间。 |

#### cfg_tag_rule_version

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| rule_version_id | string | 主键。 |
| rule_id | string | 关联规则。 |
| version_no | int | 版本号。 |
| rule_dsl_json | json | 版本规则内容。 |
| change_summary | text | 变更说明。 |
| approver_id | string | 审批人。 |
| released_at | datetime | 上线时间。 |
| created_by | string | 创建人。 |
| created_at | datetime | 创建时间。 |

#### cfg_tag_dependency

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| tag_id | string | 主标签 ID。 |
| dependent_tag_id | string | 依赖标签 ID。 |
| dependency_type | string | `value`/`trigger` 等。 |
| created_at | datetime | 创建时间。 |
| created_by | string | 创建人。 |

#### cfg_tag_job_status

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| job_id | string | 主键。 |
| tag_id | string | 关联标签。 |
| rule_version_id | string | 使用的规则版本。 |
| schedule_type | string | `streaming`/`batch`/`manual`。 |
| last_run_at | datetime | 最近执行时间。 |
| last_status | string | `success`/`failed`/`running`。 |
| retry_count | int | 重试次数。 |
| error_message | text | 最近失败原因。 |
| next_run_at | datetime | 下次执行时间。 |
| operator_id | string | 最近操作人。 |
| updated_at | datetime | 更新时间。 |

#### cfg_audience_definition

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| audience_id | string | 主键。 |
| audience_name | string | 人群名称。 |
| description | text | 说明。 |
| target_entity_type | string | `account`/`contact`/`lead`。 |
| definition_dsl_json | json | 圈人 DSL/SQL。 |
| owner_user_id | string | 负责人。 |
| status | string | `draft`/`approved`/`online`/`offline`。 |
| created_by | string | 创建人。 |
| created_at | datetime | 创建时间。 |
| updated_by | string | 更新人。 |
| updated_at | datetime | 更新时间。 |

#### cfg_audience_version

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| version_id | string | 主键。 |
| audience_id | string | 关联人群。 |
| version_no | int | 版本号。 |
| definition_dsl_json | json | 版本规则。 |
| released_at | datetime | 上线时间。 |
| approver_id | string | 审批人。 |
| created_by | string | 创建人。 |
| created_at | datetime | 创建时间。 |

#### cfg_audience_job_status

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| job_id | string | 主键。 |
| audience_id | string | 关联人群。 |
| version_id | string | 使用的人群版本。 |
| execute_mode | string | `full`/`incremental`/`realtime`。 |
| last_run_at | datetime | 最近执行时间。 |
| last_status | string | `success`/`failed`/`running`。 |
| result_count | bigint | 最近一次生成成员数。 |
| error_message | text | 错误信息。 |
| next_run_at | datetime | 下次执行时间。 |
| operator_id | string | 最近操作人。 |
| updated_at | datetime | 更新时间。 |

#### cfg_channel_mapping

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| channel_id | string | 主键。 |
| channel_type | string | 渠道类型（email、sms、wechat_corp 等）。 |
| system_endpoint | string | API 或 Topic 地址。 |
| auth_info | json | 鉴权信息。 |
| callback_url | string | 回执回调地址。 |
| status | string | 启用状态。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### cfg_webhook_subscription

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| webhook_id | string | 主键。 |
| event_type | string | 订阅事件（tag.updated、audience.ready 等）。 |
| callback_url | string | 回调地址。 |
| secret | string | 签名密钥。 |
| status | string | 启用状态。 |
| created_by | string | 创建人。 |
| created_at | datetime | 创建时间。 |
| updated_by | string | 修改人。 |
| updated_at | datetime | 更新时间。 |

---

### META 元数据 / 运维层

#### 表总览

| 表名 | 主键 | 建议存储 | 存储理由 |
| --- | --- | --- | --- |
| meta_source_registry | source_id | PostgreSQL | 数据源登记需要权限、审批与审计 |
| meta_field_mapping | mapping_id | PostgreSQL | 字段映射频繁调整，需事务保障 |
| meta_schema_change_log | change_id | PostgreSQL | 上游结构变更需留痕 |
| meta_kafka_topic_registry | topic_name | PostgreSQL | Kafka 元数据管理 |
| meta_data_quality_rule | dq_rule_id | PostgreSQL | 质量规则需审批与版本控制 |
| meta_data_quality_result | result_id | PostgreSQL | 质量结果需可靠落盘 |
| meta_replay_checkpoint | replay_id | PostgreSQL | 重放位点需一致性 |

#### meta_source_registry

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| source_id | string | 主键，内部来源 ID。 |
| source_name | string | 数据源名称。 |
| source_type | string | 系统类型（crm、wechat、payment 等）。 |
| contact | string | 上游负责人。 |
| sync_mode | string | 同步方式（api、cdc、webhook、batch）。 |
| sync_frequency | string | 同步频率。 |
| status | string | 启用状态。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### meta_field_mapping

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| mapping_id | string | 主键。 |
| source_id | string | 关联数据源。 |
| source_field | string | 上游字段名。 |
| target_entity | string | 目标实体（account/contact/lead 等）。 |
| target_field | string | 目标字段。 |
| transform_rule | json | 转换规则或表达式。 |
| is_active | boolean | 是否启用。 |
| created_by | string | 创建人。 |
| created_at | datetime | 创建时间。 |
| updated_by | string | 更新人。 |
| updated_at | datetime | 更新时间。 |

#### meta_schema_change_log

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| change_id | string | 主键。 |
| source_id | string | 数据源。 |
| entity_type | string | 受影响实体。 |
| change_detail_json | json | 变更详情（新增/删除字段等）。 |
| detected_at | datetime | 检测时间。 |
| handled_by | string | 处理人。 |
| handled_at | datetime | 处理时间。 |
| status | string | `pending`/`handled`。 |

#### meta_kafka_topic_registry

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| topic_name | string | 主键。 |
| entity_type | string | 主题对应实体/事件。 |
| retention_hours | int | 主题保留时长。 |
| partition_count | int | 分区数。 |
| replication_factor | int | 副本数。 |
| consumer_group | string | 默认消费组。 |
| description | string | 说明。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### meta_data_quality_rule

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| dq_rule_id | string | 主键。 |
| entity_type | string | 校验对象（dim_account、fact_tag_snapshot 等）。 |
| rule_type | string | `completeness`/`uniqueness`/`validity` 等。 |
| rule_expression | json | 规则表达式或 SQL。 |
| threshold | decimal | 判定阈值。 |
| severity | string | 严重级别。 |
| owner_id | string | 负责人。 |
| schedule | string | 执行频率。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### meta_data_quality_result

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| result_id | string | 主键。 |
| dq_rule_id | string | 关联质量规则。 |
| executed_at | datetime | 执行时间。 |
| status | string | `pass`/`fail`。 |
| score | decimal | 得分。 |
| sample_detail | json | 失败样本摘要。 |
| remarks | string | 备注。 |
| created_at | datetime | 记录时间。 |

#### meta_replay_checkpoint

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| replay_id | string | 主键。 |
| topic_name | string | Kafka Topic 名称。 |
| consumer_group | string | 消费组。 |
| offset | bigint | 当前 offset。 |
| status | string | `pending`/`running`/`completed`。 |
| started_at | datetime | 重放开始时间。 |
| updated_at | datetime | 最近更新时间。 |
| operator_id | string | 操作人。 |

---

### Redis Key 结构

- `tag:{entity_type}:{entity_id}` → Hash，字段 `tag_code` 存最新 `tag_value`；结合 TTL 控制缓存时效。
- `audience:{audience_id}` → Set，成员为 `entity_id`；用于实时圈人结果缓存。
- `entity:resolve:{source_system}:{source_id}` → String，值为 `global_id`；支撑实体解析。
- `tag_rule:cache:{tag_id}` → Hash，缓存规则 DSL、版本号、更新时间。
- `lock:tag_job:{tag_id}` / `lock:audience_job:{audience_id}` → String，带过期时间，实现分布式锁。

---

### 表间关联关系

- `dim_account.global_id` ← `dim_contact.account_id`、`dim_lead.account_id`、`fact_payment.account_id`、`fact_form_submission.account_id`、`fact_account_health_score.account_id`。
- `dim_contact.global_id` ← `fact_interaction.entity_id`（当 `entity_type='contact'`）、`fact_form_submission.contact_id`、`fact_audience_membership.entity_id`（联系人人群）、`fact_contact_lifecycle_transition.contact_id`。
- `dim_lead.global_id` ← `fact_lead_state_transition.lead_id`、`fact_audience_membership.entity_id`（线索人群）、`fact_interaction.entity_id`（`entity_type='lead'`）。
- `dim_channel.channel_id` ← `fact_interaction.channel_id`、`cfg_channel_mapping.channel_id`。
- `dim_tag.tag_id` ← `fact_tag_assignment.tag_id`、`fact_tag_snapshot.tag_id`、`cfg_tag_rule.tag_id`、`cfg_tag_dependency.tag_id`。
- `dim_campaign.campaign_id` ← `ods_lead_snapshot.campaign_id`、`fact_form_submission.form_id`（若绑定活动）。
- `dim_user.user_id` ← `dim_account.owner_user_id`、`dim_contact.owner_user_id`、`dim_lead.owner_user_id`、所有 CFG 表的 `owner_user_id`/`approver_id`/`operator_id`。
- `dim_time.date_key` ← `fact_account_health_score.record_date`，其他事实表可通过时间字段衍生。
- `cfg_tag_rule.rule_id` ← `fact_tag_assignment.rule_id`、`fact_tag_snapshot.source_rule_id`、`cfg_tag_rule_version.rule_id`。
- `cfg_audience_definition.audience_id` ← `fact_audience_membership.audience_id`、`fact_audience_trigger.audience_id`、`cfg_audience_version.audience_id`、`cfg_audience_job_status.audience_id`。
- `cfg_audience_version.version_id` ← `fact_audience_membership.source_version_id`、`cfg_audience_job_status.version_id`。
- `meta_source_registry.source_id` ← `meta_field_mapping.source_id`，并与 RAW/ODS 表 `source_system` 逻辑对应。
- `meta_data_quality_rule.dq_rule_id` ← `meta_data_quality_result.dq_rule_id`。

---

### 存储引擎与部署建议

- **MongoDB（RAW 层）**：
  - 优势：无需固定 schema，接入新字段零成本；可通过分片与 TTL 控制数据量；支持聚合查询用于排查。
  - 建议：启用写入确认（Write Concern majority）、对 `id/external_userid/openid` 建立唯一索引。

- **ClickHouse（ODS/DIM/FACT 层）**：
  - 优势：列式存储 + 向量化执行，适合高并发写入和秒级圈人查询；MergeTree 系列支持分区、TTL、去重。
  - 建议：
    - ODS Snapshot 表以 `snapshot_at` 分区并设置 30-90 天 TTL；
    - FACT 表以时间字段分区，常用过滤字段建立稀疏索引；
    - `fact_tag_snapshot` 使用 ReplacingMergeTree 按 `updated_at` 去重。

- **PostgreSQL（CFG/META 层）**：
  - 优势：ACID 事务、行级锁，适合标签、人群、任务的审批流；支持 JSONB 字段存储 DSL。
  - 建议：部署双节点热备或使用云 RDS；关键字段建立唯一索引，配合审计日志。

- **Redis（缓存层）**：
  - 优势：毫秒级读写，丰富数据结构；适合标签、人群、规则缓存与分布式锁。
  - 建议：使用 Cluster 模式保证扩展能力，标签缓存设置短 TTL 并定期刷新，防止脏数据。

---

### 数据流转路径

1. **采集入湖**：各数据源经 API/CDC/Webhook → Kafka → MongoDB RAW（保留原文与 Offset）。
2. **标准化处理**：Kafka Streams/ksqlDB 根据 `meta_field_mapping` 清洗，写入 ClickHouse ODS。
3. **主数据整合**：ODS Snapshot 通过任务写入 DIM 表，维护最新主数据。
4. **事实&标签**：实时计算服务依据 CFG 规则写入 FACT 表和 Redis 缓存；批任务补齐历史数据。
5. **圈人与触达**：圈人服务使用 ClickHouse 查询 `fact_tag_snapshot`、`fact_audience_membership`，结果写 Redis/触达渠道。
6. **运维治理**：META 表记录数据源、字段映射、质量与重放信息，支撑监控、审计。

此文档可直接用于内部数据字典或建表依据，后续仅需补充具体建表 SQL（含数据类型精度、分区、索引）即可落地实施。
