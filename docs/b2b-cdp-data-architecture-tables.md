## B2B CDP 数据表设计概览

围绕实时打标、圈人诉求，结合 Kafka Streams + Redis + ClickHouse + PostgreSQL/MySQL 的方案，建议划分以下数据域：

- **采集/原始层（RAW）**：按来源保留未经加工的数据，为重放和稽核提供依据。
- **标准化层（ODS）**：完成字段清洗、ID 统一、结构化，支撑下游维表与事实表生成。
- **维度层（DIM）**：沉淀相对静态的实体（Account、Contact、Lead 等）。
- **事实层（FACT）**：记录行为、交易、标签、圈人等明细事件。
- **标签与圈人配置层（CFG）**：管理标签字典、规则、受众定义、任务状态机等。
- **元数据/运维层（META）**：记录数据源、字段映射、Schema 变更等。

以下表建议均可部署在 PostgreSQL/MySQL（配置、元数据类）与 ClickHouse（明细、分析类）。Redis 负责缓存，但也在文末给出建议的 Key 结构。

---

### 1. RAW 原始表

| 表名 | 推荐库 | 关键字段 | 说明 |
| --- | --- | --- | --- |
| raw_salesforce_account | ClickHouse / Mongo | id, payload, ingested_at | Salesforce Account 原始 JSON（包含全量字段）。 |
| raw_salesforce_contact | ClickHouse / Mongo | id, payload, ingested_at | Salesforce Contact 原始 JSON。 |
| raw_salesforce_lead | ClickHouse / Mongo | id, payload, ingested_at | Salesforce Lead 原始 JSON。 |
| raw_wechat_corp_customer | ClickHouse / Mongo | external_userid, payload, ingested_at | 企微外部联系人数据。 |
| raw_wechat_official_user | ClickHouse / Mongo | openid, payload, ingested_at | 公众号粉丝信息。 |
| raw_paypal_form_submission | ClickHouse / Mongo | submission_id, payload, ingested_at | PayPal CN 表单提交原始数据。 |
| raw_paypal_core_transaction | ClickHouse / Mongo | txn_id, payload, ingested_at | PayPal 核心交易原始数据。 |
| raw_crm_vendor_lead | ClickHouse / Mongo | lead_id, payload, ingested_at | 第三方 CRM 线索数据。 |
| raw_crm_vendor_contact | ClickHouse / Mongo | contact_id, payload, ingested_at | 第三方 CRM 联系人数据。 |
| raw_crm_vendor_account | ClickHouse / Mongo | account_id, payload, ingested_at | 第三方 CRM 客户数据。 |

> RAW 层 payload 统一保存原始 JSON，方便重放；可选分区字段 `date=YYYYMMDD` 便于归档。

---

### 2. ODS 标准化表

| 表名 | 推荐库 | 关键字段 | 核心字段示例 | 说明 |
| --- | --- | --- | --- | --- |
| ods_account_snapshot | ClickHouse | global_id, source_system, source_id, snapshot_at | account_name, industry, company_size, normalized_address | Account 标准化快照；按天/小时时间分区。 |
| ods_contact_snapshot | ClickHouse | global_id, source_system, source_id, snapshot_at | full_name, emails, phones, account_ref | Contact 标准化快照。 |
| ods_lead_snapshot | ClickHouse | global_id, source_system, source_id, snapshot_at | lead_name, company, lead_status, owner | Lead 标准化快照。 |
| ods_account_delta | ClickHouse | global_id, change_type, change_at | before_json, after_json | 记录 Account 变更日志，供 CDC、回滚使用。 |
| ods_contact_delta | ClickHouse | global_id, change_type, change_at | before_json, after_json | Contact 变更日志。 |
| ods_lead_delta | ClickHouse | global_id, change_type, change_at | before_json, after_json | Lead 变更日志。 |
| ods_interaction_event | ClickHouse | event_id, entity_type, entity_id, occurred_at | channel, action, payload | 合并邮件、企微、公众号、表单等互动事件。 |
| ods_payment_transaction | ClickHouse | txn_id, account_id, contact_id, occurred_at | amount, currency, status, source_system | PayPal 核心交易标准化。 |
| ods_form_submission | ClickHouse | submission_id, contact_id, account_id, submitted_at | form_id, source_url, answers_json | 表单提交标准化。 |

---

### 3. DIM 维度表

| 表名 | 推荐库 | 核心字段 | 说明 |
| --- | --- | --- | --- |
| dim_account | ClickHouse | global_id (PK), account_name, industry, lifecycle_stage, customer_tier, region, tags | 最新的 Account 维度信息，供事实表引用。 |
| dim_contact | ClickHouse | global_id (PK), account_id, full_name, emails, phones, lifecycle_stage, lead_source | 最新的 Contact 维度信息。 |
| dim_lead | ClickHouse | global_id (PK), account_id, contact_id, lead_status, score, intent_level, latest_owner | 最新 Lead 维度信息。 |
| dim_channel | PostgreSQL/MySQL | channel_id (PK), channel_name, channel_type, description | 渠道字典（email、wechat_corp、wechat_official、form 等）。 |
| dim_tag | PostgreSQL/MySQL | tag_id (PK), tag_code, tag_name, category, data_type, sensitivity_level | 标签字典基础信息。 |
| dim_campaign | PostgreSQL/MySQL | campaign_id (PK), campaign_name, source_system, status, start_at, end_at | 营销活动维度。 |
| dim_user | PostgreSQL/MySQL | user_id (PK), name, role, department | 内部运营/销售人员字典。 |
| dim_time | ClickHouse | date_key, day, week, month, quarter, year | 可选时间维度。 |

---

### 4. FACT 事实表

| 表名 | 推荐库 | 粒度 | 核心字段 | 说明 |
| --- | --- | --- | --- | --- |
| fact_interaction | ClickHouse | 单次互动 | event_id, entity_type, entity_id, channel_id, action, occurred_at, payload_digest | 聚合邮件、企微、公众号等互动行为。 |
| fact_payment | ClickHouse | 单笔交易 | txn_id, account_id, contact_id, amount, currency, status, occurred_at, source_system | LTV、回款分析。 |
| fact_form_submission | ClickHouse | 单次表单 | submission_id, contact_id, account_id, form_id, submitted_at, answers_hash | 表单行为明细。 |
| fact_tag_assignment | ClickHouse | 标签变动 | record_id, tag_id, entity_type, entity_id, tag_value, calculated_at, expires_at, rule_id | 标签赋值历史，支持追溯。 |
| fact_tag_snapshot | ClickHouse | 实体+标签当前值 | entity_type, entity_id, tag_id, tag_value, updated_at, confidence_score | 最新标签快照，圈人主表。 |
| fact_audience_membership | ClickHouse | 人群成员关系 | audience_id, entity_type, entity_id, membership_status, joined_at, left_at | 圈人结果记录。 |
| fact_audience_trigger | ClickHouse | 触达动作 | trigger_id, audience_id, entity_id, trigger_channel, triggered_at, status, trace_id | 实时触达日志。 |
| fact_lead_state_transition | ClickHouse | Lead 状态流转 | event_id, lead_id, prev_status, new_status, changed_at, operator_id, reason | 漏斗与状态机分析。 |
| fact_contact_lifecycle_transition | ClickHouse | Contact 生命周期流转 | event_id, contact_id, prev_stage, new_stage, changed_at, operator_id | 生命周期监控。 |
| fact_account_health_score | ClickHouse | Account 日健康度 | record_date, account_id, engagement_score, churn_risk_level, computed_at | 可由批/实时任务写入。 |

---

### 5. 标签 / 圈人配置表（CFG）

| 表名 | 推荐库 | 核心字段 | 说明 |
| --- | --- | --- | --- |
| cfg_tag_dictionary | PostgreSQL/MySQL | tag_id (PK), tag_code, tag_name, description, category, data_type, is_sensitive, owner_user_id, created_at, updated_at | 标签字典主表。 |
| cfg_tag_rule | PostgreSQL/MySQL | rule_id (PK), tag_id, rule_type, rule_dsl_json, source_priority, refresh_strategy, status | 标签计算规则（DSL/SQL），供 Kafka Streams 读取。 |
| cfg_tag_rule_version | PostgreSQL/MySQL | rule_version_id, rule_id, version_no, rule_dsl_json, approver_id, released_at | 规则版本管理与审计。 |
| cfg_tag_dependency | PostgreSQL/MySQL | tag_id, dependent_tag_id, dependency_type | 标签依赖关系，用于批量刷新。 |
| cfg_tag_job_status | PostgreSQL/MySQL | job_id, tag_id, rule_version_id, schedule_type, last_run_at, last_status, retry_count, error_message | 打标任务状态机。 |
| cfg_audience_definition | PostgreSQL/MySQL | audience_id (PK), audience_name, description, target_entity_type, definition_dsl_json, owner_user_id, status | 人群定义，供圈人服务解析。 |
| cfg_audience_version | PostgreSQL/MySQL | version_id, audience_id, version_no, definition_dsl_json, released_at, approver_id | 人群版本管理。 |
| cfg_audience_job_status | PostgreSQL/MySQL | job_id, audience_id, version_id, execute_mode, last_run_at, last_status, result_count | 圈人任务状态机。 |
| cfg_channel_mapping | PostgreSQL/MySQL | channel_id, channel_type, system_endpoint, auth_info, status | 触达渠道配置。 |
| cfg_webhook_subscription | PostgreSQL/MySQL | webhook_id, event_type, callback_url, secret, status, created_at | 外部订阅标签/人群事件。 |

---

### 6. 元数据 / 运维表（META）

| 表名 | 推荐库 | 核心字段 | 说明 |
| --- | --- | --- | --- |
| meta_source_registry | PostgreSQL/MySQL | source_id, source_name, source_type, contact, sync_mode, sync_frequency, status | 数据源登记簿。 |
| meta_field_mapping | PostgreSQL/MySQL | mapping_id, source_id, source_field, target_entity, target_field, transform_rule, is_active | 字段映射规则。 |
| meta_schema_change_log | PostgreSQL/MySQL | change_id, source_id, entity_type, change_detail_json, detected_at | 上游 Schema 变更记录。 |
| meta_kafka_topic_registry | PostgreSQL/MySQL | topic_name, entity_type, retention_hours, partition_count, consumer_group | Kafka Topic 管理。 |
| meta_data_quality_rule | PostgreSQL/MySQL | dq_rule_id, entity_type, rule_type, rule_expression, threshold, owner_id | 数据质量校验规则。 |
| meta_data_quality_result | PostgreSQL/MySQL | result_id, dq_rule_id, executed_at, status, score, sample_detail | 校验执行结果。 |
| meta_replay_checkpoint | PostgreSQL/MySQL | replay_id, topic_name, offset, status, updated_at | Kafka 重放/补数位点。 |

---

### 7. Redis Key 结构建议

- `tag:{entity_type}:{entity_id}` → Hash，字段为 `tag_code`，值为最新 `tag_value`。
- `audience:{audience_id}` → Set，成员为 `entity_id`，用于实时圈人。
- `entity:resolve:{source_system}:{source_id}` → String，值为 `global_id`，支持实体ID解析。
- `tag_rule:cache:{tag_id}` → String/Hash，缓存规则 DSL，减少数据库访问。
- `lock:tag_job:{tag_id}` / `lock:audience_job:{audience_id}` → String（带过期），实现分布式锁。

---

### 8. 数据流转关系概览

- RAW → ODS：Kafka Streams 对 RAW 表进行清洗、标准化写入 ODS。
- ODS → DIM/FACT：
  - `dim_*` 表按最新快照聚合，可用 ClickHouse 物化视图或定时合并。
  - `fact_*` 表记录明细事件或状态机流转，引用维度表主键。
- CFG/META：驱动标签、圈人任务执行，并为监控、审计提供基础。
- Redis 缓存提供标签、人群的实时读写能力，ClickHouse 负责复杂筛选与分析。

该表设计覆盖实体主数据、行为事实、标签圈人、任务状态及治理元数据，支持实时打标、实时圈人目标。可根据实际业务规模调整分区、副本和冷热分层策略。
