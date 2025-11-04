## B2B CDP 核心模型 Schema

以下内容适用于对接 Salesforce、企业微信、微信公众号、PayPal（表单与核心数据）以及第三方 CRM 等多数据源场景。

### Account

| 字段 | 类型 | 说明 / 典型映射 |
| --- | --- | --- |
| global_id | string | CDP 主键（UUID/算法生成） |
| source_system | enum | 数据源标识：salesforce、wechat_corp、paypal_core、crm_vendor_x 等 |
| source_id | string | 各系统原始账号 ID；如 Salesforce AccountId |
| external_ids | array<object> | 税号、组织机构代码、营业执照号、自定义 Code |
| account_name | string | 企业名称；SF Name、企微客户名 |
| account_alias | string | 简称/品牌名；公众号主体名 |
| industry | string | 标准行业分类；SF Industry、第三方 CRM |
| sub_industry | string | 细分行业 |
| company_size | string/int | 员工数或区间；SF NumberOfEmployees |
| annual_revenue | decimal | 年营收；SF AnnualRevenue |
| country | string | 国家 |
| region | string | 省/州 |
| city | string | 城市 |
| address | object | 街道、邮编等结构化地址 |
| website | string | 官网域名；SF Website、PayPal 商户资料 |
| social_profiles | array<object> | 公众号主体 ID、企微企业 ID 等 |
| account_status | enum | active/prospect/terminated 等；CRM 状态 |
| customer_tier | string | 客户分级（如 KA/普通/试用）；SF Type、CRM 标签 |
| lifecycle_stage | enum | suspect/prospect/customer/churned |
| parent_account_id | string | 上级企业 global_id（无需层级时可留空） |
| subsidiary_accounts | array<string> | 子企业 global_id 列表（无需层级可空） |
| tags | array<string> | 企微标签、CRM 标签整合 |
| billing_currency | string | 记账币种；PayPal |
| payment_terms | string | 付款条款；CRM 或 PayPal |
| lifetime_value | decimal | 累计 GMV/回款；PayPal 核心数据或 CRM 合同 |
| last_payment_date | datetime | 最近回款时间 |
| total_contacts | int | 关联联系人数量 |
| open_deals | int | 进行中商机数 |
| active_leads | int | 活跃线索数 |
| engagement_score | decimal | 互动评分 |
| consent_status | enum | granted/revoked |
| do_not_contact | boolean | 是否禁止营销 |
| ingested_at | datetime | 首次进入 CDP 时间 |
| last_synced_at | datetime | 最近同步时间 |
| data_quality_score | decimal | 数据完整度评分 |
| custom_attributes | json | 长尾或定制字段 |

---

### Contact

| 字段 | 类型 | 说明 / 典型映射 |
| --- | --- | --- |
| global_id | string | CDP 主键 |
| source_system | enum | salesforce、wechat_corp、wechat_official、paypal_form、crm_vendor_x 等 |
| source_id | string | SF ContactId、企微外部联系人 ID、公众号 OpenID、表单提交 ID |
| account_id | string | 关联 Account.global_id；SF AccountId |
| external_ids | array<object> | 企微 UnionID、公众号 UnionID、邮箱哈希等 |
| first_name | string | 名；SF FirstName |
| last_name | string | 姓；SF LastName |
| full_name | string | 全名；企微昵称+备注 |
| gender | enum | male/female/unknown |
| title | string | 职务；SF Title、CRM 字段 |
| department | string | 部门 |
| work_email | string | 工作邮箱；SF Email |
| personal_email | string | 私人邮箱；表单/公众号 |
| work_phone | string | 办公电话；SF Phone |
| mobile_phone | string | 手机 |
| wechat_corp_userid | string | 企微成员绑定 ID（如有） |
| wechat_openid | string | 公众号粉丝 OpenID |
| unionid | string | 微信 UnionID |
| location | object | 国家/城市/时区 |
| language_preference | string | 沟通语言偏好 |
| contact_role | string | 决策人/影响人/使用者；SF Role |
| seniority_level | enum | C-level/VP/Manager/Individual |
| lifecycle_stage | enum | subscriber/marketing qualified/sales qualified/customer |
| lead_source | string | 渠道来源：paypal_form、wechat_official、event 等 |
| tags | array<string> | 企微或公众号标签 |
| last_touch_channel | enum | email/wechat_corp/wechat_official/phone/event/form |
| last_touch_at | datetime | 最近互动时间 |
| email_engagement | object | 邮件打开、点击次数等 |
| wechat_interactions | object | 会话数、消息数、菜单点击等 |
| form_submissions | int | 表单提交次数 |
| recent_payments | int | 近期支付次数（若联系人即付款人） |
| consent_email_marketing | enum | granted/revoked |
| consent_wechat_marketing | enum | granted/revoked |
| do_not_call | boolean | 是否拒接电话 |
| preferred_contact_time | string | 偏好联系时间 |
| ingested_at | datetime | 首次进入 CDP 时间 |
| last_synced_at | datetime | 最近同步时间 |
| data_quality_score | decimal | 数据完整度评分 |
| custom_attributes | json | 长尾或定制字段 |

---

### Lead

| 字段 | 类型 | 说明 / 典型映射 |
| --- | --- | --- |
| global_id | string | CDP 主键 |
| source_system | enum | salesforce、wechat_official、paypal_form、crm_vendor_x 等 |
| source_id | string | SF LeadId、表单记录 ID、第三方 CRM 线索 ID |
| account_id | string/null | 若识别所属企业则关联 Account.global_id；SF ConvertedAccountId |
| contact_id | string/null | 若已转为联系人则关联 Contact.global_id；SF ConvertedContactId |
| lead_name | string | 线索姓名/标题；SF Name |
| company | string | 填写的企业名称；SF Company |
| job_title | string | 职位 |
| email | string | 邮件；表单/线索信息 |
| phone | string | 电话 |
| wechat_openid | string | 公众号线索 OpenID |
| form_source_url | string | 表单来源 URL；PayPal/官网 |
| campaign_id | string | 对应营销活动 ID |
| lead_source | string | 渠道：wechat_corp/wechat_official/paypal_form/event/referral 等 |
| lead_status | enum | new/working/qualified/unqualified/nurturing；映射 SF Status |
| score | decimal | 线索评分；营销或企微打分 |
| priority | enum | high/medium/low |
| product_interest | array<string> | 关注产品线 |
| estimated_budget | decimal | 预计预算 |
| intent_level | enum | low/medium/high |
| captured_at | datetime | 首次进入 CDP 时间 |
| last_activity_at | datetime | 最近跟进时间 |
| converted_at | datetime | 转化为商机/联系人时间 |
| closed_at | datetime | 作废/关闭时间 |
| owner_id | string | 线索负责人；SF OwnerId、企微跟进人 |
| follow_up_channel | enum | phone/email/wechat/offline 等 |
| next_action | string | 下一步计划 |
| notes | text | 销售备注（可 JSON 化） |
| consent_status | enum | granted/revoked 等 |
| disqualification_reason | string | 淘汰原因 |
| ingested_at | datetime | 首次进入 CDP 时间 |
| last_synced_at | datetime | 最近同步时间 |
| data_quality_score | decimal | 数据完整度评分 |
| custom_attributes | json | 长尾或定制字段 |
