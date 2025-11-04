## B2B CDP 数据表设计概览

围绕实时打标、圈人诉求，结合 Kafka Streams + Redis + ClickHouse + PostgreSQL/MySQL 的方案，建议划分以下数据域：

- **采集/原始层（RAW）**：按来源保留未经加工的数据，为重放和稽核提供依据。
- **标准化层（ODS）**：完成字段清洗、ID 统一、结构化，支撑下游维表与事实表生成。
- **维度层（DIM）**：沉淀相对静态的实体（Account、Contact、Lead 等）。
- **事实层（FACT）**：记录行为、交易、标签、圈人等明细事件。
- **标签与圈人配置层（CFG）**：管理标签字典、规则、受众定义、任务状态机等。
- **元数据/运维层（META）**：记录数据源、字段映射、Schema 变更等。

以下表建议均可部署在 PostgreSQL/MySQL（配置、元数据类）与 ClickHouse（明细、分析类）。Redis 负责缓存，文末给出推荐的 Key 结构。

---

### RAW 采集 / 原始层

#### raw_salesforce_account

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| id | string | Salesforce Account 原始 ID（AccountId）。 |
| source_system | string | 固定为 `salesforce`。 |
| source_env | string | 数据来源环境，如 `prod`、`sandbox`。 |
| payload | json | 原始 JSON 内容，保持上游字段名。 |
| received_at | datetime | 接收到上游推送的时间。 |
| ingested_at | datetime | 写入 RAW 表的时间。 |
| kafka_offset | bigint | 对应 Kafka 事件的 offset，便于回放。 |
| ingestion_batch_id | string | 本次摄取批次 ID。 |
| processing_status | string | `success`/`failed`，记录解析状态。 |
| error_message | string | 失败时的错误信息。 |
| created_at | datetime | 记录创建时间（冗余，用于 ClickHouse 分片或 Mongo 索引）。 |

#### raw_salesforce_contact

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| id | string | Salesforce ContactId。 |
| source_system | string | `salesforce`。 |
| source_env | string | prod/sandbox 等。 |
| payload | json | 原始 Contact JSON。 |
| received_at | datetime | 接收到事件的时间。 |
| ingested_at | datetime | 写入 RAW 表的时间。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次 ID。 |
| processing_status | string | 事件处理状态。 |
| error_message | string | 处理失败原因。 |
| created_at | datetime | 记录创建时间。 |

#### raw_salesforce_lead

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| id | string | Salesforce LeadId。 |
| source_system | string | `salesforce`。 |
| source_env | string | prod/sandbox 等。 |
| payload | json | 原始 Lead JSON。 |
| received_at | datetime | 接收到事件时间。 |
| ingested_at | datetime | 写入 RAW 表时间。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | `success`/`failed`。 |
| error_message | string | 失败原因。 |
| created_at | datetime | 记录创建时间。 |

#### raw_wechat_corp_customer

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| external_userid | string | 企业微信外部联系人 ID。 |
| source_system | string | `wechat_corp`。 |
| corp_id | string | 所属企业微信 CorpID。 |
| payload | json | 原始客户 JSON 数据。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入 RAW 时间。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 记录创建时间。 |

#### raw_wechat_official_user

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| openid | string | 公众号粉丝 OpenID。 |
| unionid | string | 微信 UnionID（如可获取）。 |
| source_system | string | `wechat_official`。 |
| official_account_id | string | 公众号 appid。 |
| payload | json | 原始粉丝 JSON 数据。 |
| received_at | datetime | 接收到事件时间。 |
| ingested_at | datetime | 写入 RAW 时间。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 记录创建时间。 |

#### raw_paypal_form_submission

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| submission_id | string | 表单提交通用 ID。 |
| source_system | string | `paypal_form`。 |
| form_id | string | PayPal 表单 ID。 |
| payload | json | 原始表单提交 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入 RAW 时间。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 记录创建时间。 |

#### raw_paypal_core_transaction

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| txn_id | string | PayPal 交易号。 |
| source_system | string | `paypal_core`。 |
| merchant_account | string | 商户编号。 |
| payload | json | 原始交易 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入 RAW 时间。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 记录创建时间。 |

#### raw_crm_vendor_account

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| account_id | string | 第三方 CRM 客户 ID。 |
| source_system | string | `crm_vendor_x`。 |
| vendor_code | string | CRM 厂商标识。 |
| payload | json | 原始客户 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入 RAW 时间。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 记录创建时间。 |

#### raw_crm_vendor_contact

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| contact_id | string | 第三方 CRM 联系人 ID。 |
| source_system | string | `crm_vendor_x`。 |
| vendor_code | string | CRM 厂商标识。 |
| payload | json | 原始联系人 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入 RAW 时间。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 记录创建时间。 |

#### raw_crm_vendor_lead

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| lead_id | string | 第三方 CRM 线索 ID。 |
| source_system | string | `crm_vendor_x`。 |
| vendor_code | string | CRM 厂商标识。 |
| payload | json | 原始线索 JSON。 |
| received_at | datetime | 接收时间。 |
| ingested_at | datetime | 写入 RAW 时间。 |
| kafka_offset | bigint | Kafka Offset。 |
| ingestion_batch_id | string | 摄取批次。 |
| processing_status | string | 处理状态。 |
| error_message | string | 错误信息。 |
| created_at | datetime | 记录创建时间。 |

---

### ODS 标准化层

#### ods_account_snapshot

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | CDP 全局唯一 ID。 |
| source_system | string | 原始来源，如 `salesforce`、`crm_vendor_x`。 |
| source_id | string | 来源系统中对应的账户 ID。 |
| snapshot_at | datetime | 快照生成时间（通常按小时/天分区）。 |
| account_name | string | 标准化后的企业名称。 |
| account_alias | string | 企业简称或品牌名。 |
| industry | string | 标准行业分类。 |
| sub_industry | string | 细分行业。 |
| company_size | string | 员工规模（枚举或区间）。 |
| annual_revenue | decimal | 年营收（标准货币）。 |
| country | string | 国家。 |
| region | string | 省/州。 |
| city | string | 城市。 |
| address_struct | json | 结构化地址（街道、邮编等）。 |
| website | string | 官网域名。 |
| lifecycle_stage | string | suspect/prospect/customer/churned。 |
| customer_tier | string | KA/普通/试用等。 |
| tags | array<string> | 来自上游的原始标签集合。 |
| last_modified_at | datetime | 来源系统最后更新时间。 |
| ingested_at | datetime | 写入 ODS 的时间。 |

#### ods_contact_snapshot

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | CDP 全局唯一 ID。 |
| source_system | string | 来源系统：salesforce、wechat_corp 等。 |
| source_id | string | 来源系统的联系人 ID。 |
| snapshot_at | datetime | 快照时间。 |
| account_id | string | 对应 `dim_account.global_id`。 |
| full_name | string | 标准化姓名。 |
| first_name | string | 名（可空）。 |
| last_name | string | 姓（可空）。 |
| gender | string | male/female/unknown。 |
| job_title | string | 职务。 |
| department | string | 部门。 |
| emails | array<string> | 邮箱集合（去重、归一化）。 |
| phones | array<string> | 电话集合。 |
| lead_source | string | 渠道来源。 |
| lifecycle_stage | string | 联系人生命周期阶段。 |
| tags | array<string> | 标签集合。 |
| last_active_at | datetime | 最近互动时间。 |
| last_modified_at | datetime | 来源系统最新时间。 |
| ingested_at | datetime | 写入 ODS 时间。 |

#### ods_lead_snapshot

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | CDP 全局唯一 ID。 |
| source_system | string | 来源系统：salesforce、paypal_form、crm_vendor_x 等。 |
| source_id | string | 来源系统线索 ID。 |
| snapshot_at | datetime | 快照时间。 |
| account_id | string | 已识别时关联的 Account global_id。 |
| contact_id | string | 已转化时关联的 Contact global_id。 |
| lead_name | string | 线索姓名或标题。 |
| company | string | 填写的公司名。 |
| email | string | 主邮箱。 |
| phone | string | 主电话。 |
| job_title | string | 职位。 |
| lead_status | string | new/working/qualified 等。 |
| score | decimal | 线索评分。 |
| intent_level | string | low/medium/high。 |
| lead_source | string | 渠道来源。 |
| campaign_id | string | 活动 ID（关联 `dim_campaign`）。 |
| owner_id | string | 负责人 ID（关联 `dim_user`）。 |
| tags | array<string> | 标签集合。 |
| last_activity_at | datetime | 最近跟进时间。 |
| last_modified_at | datetime | 来源系统更新时间。 |
| ingested_at | datetime | 写入 ODS 时间。 |

#### ods_account_delta

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| change_id | string | 变更记录 ID。 |
| global_id | string | 关联 Account global_id。 |
| source_system | string | 来源系统。 |
| source_id | string | 来源 ID。 |
| change_type | string | `insert`/`update`/`delete`。 |
| change_at | datetime | 变更发生时间。 |
| before_json | json | 变更前原始 JSON。 |
| after_json | json | 变更后 JSON。 |
| diff | json | 字段差异摘要。 |
| processed_at | datetime | 记录写入时间。 |

#### ods_contact_delta

字段与 ods_account_delta 类似，仅 `global_id`、`source_id` 指向联系人。

#### ods_lead_delta

字段与 ods_account_delta 类似，仅 `global_id`、`source_id` 指向线索。

#### ods_interaction_event

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| event_id | string | 事件唯一 ID。 |
| source_system | string | 数据来源（email、wechat_corp 等）。 |
| entity_type | string | `account`/`contact`/`lead`。 |
| entity_id | string | 对应实体 global_id。 |
| channel | string | 互动渠道。 |
| action | string | 行为类型（open、click、message、visit 等）。 |
| payload | json | 标准化后的事件内容。 |
| occurred_at | datetime | 行为发生时间。 |
| received_at | datetime | 接收到事件时间。 |
| ingested_at | datetime | 写入 ODS 时间。 |
| rule_triggered | array<string> | 命中的标签/规则 ID。 |

#### ods_payment_transaction

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| txn_id | string | 交易号。 |
| source_system | string | `paypal_core` 或其他支付来源。 |
| account_id | string | 关联 Account global_id（如可识别）。 |
| contact_id | string | 关联 Contact global_id（付款人）。 |
| amount | decimal | 交易金额。 |
| currency | string | 币种。 |
| status | string | pending/success/failed/refunded 等。 |
| payment_method | string | 支付方式。 |
| occurred_at | datetime | 交易时间。 |
| captured_at | datetime | 捕获/结算时间。 |
| raw_payload | json | 标准化前关键信息。 |
| ingested_at | datetime | 写入 ODS 时间。 |

#### ods_form_submission

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| submission_id | string | 表单提交 ID。 |
| source_system | string | `paypal_form`、`crm_vendor_x` 等。 |
| form_id | string | 表单标识。 |
| account_id | string | 若可识别，关联 Account。 |
| contact_id | string | 若可识别，关联 Contact。 |
| lead_id | string | 若生成线索，关联 Lead。 |
| submitted_at | datetime | 提交时间。 |
| source_url | string | 来源页面 URL。 |
| referrer | string | 引荐来源。 |
| answers_json | json | 标准化后的问卷答案。 |
| ip_address | string | 提交 IP。 |
| user_agent | string | 浏览器 UA。 |
| ingested_at | datetime | 写入 ODS 时间。 |

---

### DIM 维度层

维度表用于描述实体的相对静态属性，提供事实表关联键。

#### dim_account

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| global_id | string | 主键，Account 全局 ID。 |
| master_source | string | 主数据来源系统。 |
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
| master_source | string | 主数据来源系统。 |
| full_name | string | 姓名。 |
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
| account_id | string | 关联 Account。 |
| contact_id | string | 关联 Contact（若已转化）。 |
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
| category | string | 类别（行为/属性/预测等）。 |
| data_type | string | 值类型（string/int/decimal/bool/datetime/list）。 |
| sensitivity_level | string | 敏感程度：low/medium/high。 |
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
| username | string | 登录账号。 |
| display_name | string | 显示名称。 |
| role | string | 角色（运营、销售、管理员等）。 |
| department | string | 部门。 |
| email | string | 邮箱。 |
| phone | string | 联系电话。 |
| status | string | 启用状态。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### dim_time

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| date_key | int | YYYYMMDD 整数，主键。 |
| date_value | date | 日期。 |
| day_of_week | int | 周几（1-7）。 |
| week_of_year | int | 第几周。 |
| month | int | 月份。 |
| quarter | int | 季度。 |
| year | int | 年份。 |
| is_weekend | boolean | 是否周末。 |
| is_holiday | boolean | 是否节假日（可自维护）。 |
| created_at | datetime | 创建时间。 |

---

### FACT 事实层

事实表记录可聚合的事件和指标，通常部署在 ClickHouse。

#### fact_interaction

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| event_id | string | 主键。 |
| entity_type | string | `account`/`contact`/`lead`。 |
| entity_id | string | 对应实体 global_id。 |
| channel_id | string | 关联 `dim_channel`。 |
| action | string | 行为类型（open/click/message等）。 |
| occurred_at | datetime | 行为发生时间。 |
| payload_digest | string | 内容摘要或哈希。 |
| metadata | json | 额外属性（如消息ID、按钮ID）。 |
| rule_triggered | array<string> | 命中的规则/标签。 |
| ingested_at | datetime | 写入时间。 |

#### fact_payment

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| txn_id | string | 主键。 |
| account_id | string | 关联 Account。 |
| contact_id | string | 关联 Contact。 |
| amount | decimal | 交易金额。 |
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
| account_id | string | 关联 Account（可空）。 |
| contact_id | string | 关联 Contact（可空）。 |
| lead_id | string | 关联 Lead（可空）。 |
| form_id | string | 表单 ID。 |
| source_system | string | 表单来源。 |
| submitted_at | datetime | 提交时间。 |
| source_url | string | 页面 URL。 |
| referrer | string | 引荐来源。 |
| answers_hash | string | 结构化答案哈希。 |
| answers_json | json | 结构化答案。 |
| ip_address | string | IP 地址。 |
| user_agent | string | UA 字符串。 |
| ingested_at | datetime | 写入时间。 |

#### fact_tag_assignment

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| record_id | string | 主键。 |
| tag_id | string | 关联 `dim_tag`。 |
| entity_type | string | 实体类型。 |
| entity_id | string | 实体 global_id。 |
| tag_value | string | 标签值（原始字符串，必要时配合 `dim_tag.data_type` 解析）。 |
| calculated_at | datetime | 计算完成时间。 |
| expires_at | datetime | 标签过期时间。 |
| rule_id | string | 触发规则（关联 `cfg_tag_rule`）。 |
| source_system | string | 计算来源（实时/离线）。 |
| quality_score | decimal | 质量评分。 |
| ingested_at | datetime | 写入时间。 |

#### fact_tag_snapshot

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| entity_type | string | 实体类型。 |
| entity_id | string | 实体 global_id。 |
| tag_id | string | 标签 ID。 |
| tag_value | string | 当前值。 |
| value_updated_at | datetime | 最近一次更新。 |
| confidence_score | decimal | 可信度评分。 |
| source_rule_id | string | 最近一次更新对应的规则。 |
| last_calculated_by | string | 计算任务 ID。 |
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
| source_version_id | string | 圈人版本。 |
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
| source_system | string | 来源。 |
| ingested_at | datetime | 写入时间。 |

#### fact_contact_lifecycle_transition

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| event_id | string | 主键。 |
| contact_id | string | 关联 Contact global_id。 |
| prev_stage | string | 前生命周期阶段。 |
| new_stage | string | 新生命周期阶段。 |
| changed_at | datetime | 变更时间。 |
| operator_id | string | 操作人。 |
| source_system | string | 来源。 |
| ingested_at | datetime | 写入时间。 |

#### fact_account_health_score

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| record_date | date | 统计日期。 |
| account_id | string | 关联 Account。 |
| engagement_score | decimal | 互动评分。 |
| churn_risk_level | string | 流失风险等级。 |
| health_component_json | json | 评分构成明细。 |
| computed_at | datetime | 计算时间。 |
| calculation_job_id | string | 任务 ID。 |
| ingested_at | datetime | 写入时间。 |

---

### CFG 标签 / 圈人配置层

配置类表建议放在 PostgreSQL/MySQL，支撑规则管理与审批。

#### cfg_tag_dictionary

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| tag_id | string | 主键。 |
| tag_code | string | 标签编码，唯一。 |
| tag_name | string | 标签名称。 |
| category | string | 标签分类。 |
| data_type | string | 标签值类型（string/int/decimal/bool/datetime/list）。 |
| value_domain | json | 允许值范围（可选）。 |
| description | text | 标签说明。 |
| owner_user_id | string | 负责人。 |
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
| rule_dsl_json | json | 具体规则 DSL/SQL 模板。 |
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
| dependent_tag_id | string | 依赖的标签 ID。 |
| dependency_type | string | `value`（依赖值）/`trigger`（依赖触发）等。 |
| created_at | datetime | 创建时间。 |
| created_by | string | 创建人。 |

#### cfg_tag_job_status

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| job_id | string | 主键，任务 ID。 |
| tag_id | string | 关联标签。 |
| rule_version_id | string | 使用的规则版本。 |
| schedule_type | string | `streaming`/`batch`/`manual`。 |
| last_run_at | datetime | 最近执行时间。 |
| last_status | string | `success`/`failed`/`running`。 |
| retry_count | int | 重试次数。 |
| error_message | text | 最近失败原因。 |
| next_run_at | datetime | 下次计划时间。 |
| operator_id | string | 最近操作人。 |
| updated_at | datetime | 更新时间。 |

#### cfg_audience_definition

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| audience_id | string | 主键。 |
| audience_name | string | 人群名称。 |
| description | text | 说明。 |
| target_entity_type | string | `account`/`contact`/`lead`。 |
| definition_dsl_json | json | 人群定义 DSL/SQL。 |
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
| definition_dsl_json | json | 版本规则内容。 |
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
| last_run_at | datetime | 最近运行时间。 |
| last_status | string | `success`/`failed`/`running`。 |
| result_count | bigint | 最近一次生成的成员数。 |
| error_message | text | 失败原因。 |
| next_run_at | datetime | 下次计划时间。 |
| operator_id | string | 最近操作人。 |
| updated_at | datetime | 更新时间。 |

#### cfg_channel_mapping

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| channel_id | string | 主键。 |
| channel_type | string | 渠道类型（email、sms、wechat_corp 等）。 |
| system_endpoint | string | API/Topic 地址。 |
| auth_info | json | 鉴权信息（token、密钥）。 |
| callback_url | string | 回执回调地址。 |
| status | string | 启用状态。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### cfg_webhook_subscription

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| webhook_id | string | 主键。 |
| event_type | string | 订阅事件类型（tag.updated、audience.ready 等）。 |
| callback_url | string | 回调地址。 |
| secret | string | 签名密钥。 |
| status | string | 启用状态。 |
| created_by | string | 创建人。 |
| created_at | datetime | 创建时间。 |
| updated_by | string | 更新人。 |
| updated_at | datetime | 更新时间。 |

---

### META 元数据 / 运维层

#### meta_source_registry

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| source_id | string | 主键，内部定义的来源 ID。 |
| source_name | string | 来源名称。 |
| source_type | string | 系统类型（crm、wechat、payment 等）。 |
| contact | string | 上游对接人。 |
| sync_mode | string | 数据同步方式（api、cdc、webhook、batch）。 |
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
| target_entity | string | 目标实体（account/contact/lead等）。 |
| target_field | string | 目标字段。 |
| transform_rule | json | 转换规则（表达式/函数）。 |
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
| detected_at | datetime | 发现时间。 |
| handled_by | string | 处理人。 |
| handled_at | datetime | 处理时间。 |
| status | string | `pending`/`handled`。 |

#### meta_kafka_topic_registry

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| topic_name | string | 主键。 |
| entity_type | string | 主题对应实体/事件。 |
| retention_hours | int | 保留时长。 |
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
| entity_type | string | 校验对象。 |
| rule_type | string | `completeness`/`uniqueness`/`validity` 等。 |
| rule_expression | json | 规则表达式。 |
| threshold | decimal | 通过阈值。 |
| severity | string | 严重级别。 |
| owner_id | string | 负责人。 |
| schedule | string | 执行频率。 |
| created_at | datetime | 创建时间。 |
| updated_at | datetime | 更新时间。 |

#### meta_data_quality_result

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| result_id | string | 主键。 |
| dq_rule_id | string | 关联规则。 |
| executed_at | datetime | 执行时间。 |
| status | string | `pass`/`fail`。 |
| score | decimal | 得分。 |
| sample_detail | json | 失败样本摘要。 |
| remarks | string | 备注。 |
| created_at | datetime | 记录生成时间。 |

#### meta_replay_checkpoint

| 字段 | 类型 | 描述 |
| --- | --- | --- |
| replay_id | string | 主键。 |
| topic_name | string | Kafka Topic。 |
| consumer_group | string | 消费组。 |
| offset | bigint | 最近处理 offset。 |
| status | string | `pending`/`running`/`completed`。 |
| started_at | datetime | 重放开始时间。 |
| updated_at | datetime | 最近更新时间。 |
| operator_id | string | 操作人。 |

---

### Redis Key 结构建议

- `tag:{entity_type}:{entity_id}` → Hash，字段为 `tag_code`，值为最新 `tag_value`。
- `audience:{audience_id}` → Set，成员为 `entity_id`，用于实时圈人。
- `entity:resolve:{source_system}:{source_id}` → String，值为 `global_id`，支持实体解析。
- `tag_rule:cache:{tag_id}` → String/Hash，缓存规则 DSL，减少库访问。
- `lock:tag_job:{tag_id}` / `lock:audience_job:{audience_id}` → String（带过期），用于分布式锁。

---

### 数据流转关系概览

- RAW → ODS：Kafka Streams 按照 meta_field_mapping 规则进行清洗、标准化。
- ODS → DIM/FACT：
  - `dim_*` 表通过物化视图/批处理聚合最新快照；
  - `fact_*` 表记录明细事件，引用 DIM 表主键与标签键。
- CFG/META：驱动实时/批量打标及圈人任务，提供治理、审计能力。
- Redis：承担热点标签、人群查询；ClickHouse：承担复杂筛选与分析。

该表结构覆盖实体主数据、行为事实、标签圈人、任务状态及治理元数据，可直接用于建表或生成数据字典，并支撑实时打标、实时圈人目标。根据业务规模可进一步细化字段类型（如 ClickHouse UInt/Decimal 精度）以及分区、副本、冷热分层策略。
