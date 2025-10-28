### 组织与Boss定位（示例/预留）
- 企微组织同步（字段与频率待确认）；Buying Committee 角色字典待补充。
- 示例公式：`boss_score = grade_weight × interaction_freq × decision_score`（待确认权重）。
- 产出：`decision_maker_level`, `reports_to_one_id`, 推荐Boss与触达窗口。

```mermaid
flowchart TB
  QY[WeCom Org] --> R1[等级/上下级规则]
  CRM[CRM互动] --> R2[强度与影响力]
  R1 --> S[角色推断+Boss]
  R2 --> S
  S --> OUT[标签回写]
```


