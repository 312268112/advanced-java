### 标准与规范（示例/预留）
- 事件命名：`<domain>.<action>.<object>`（待补充清单）。
- 公共字段：`tenant_id`, `org_id`, `one_id`, `event_id`, `occurred_at`, `source`, `ip`, `ua`。
- 示例事件：

```json
{
  "event_id": "evt_xxx",
  "tenant_id": "t001",
  "org_id": "org_xxx",
  "one_id": "cid_xxx",
  "occurred_at": "2024-01-01T10:00:00Z",
  "source": "crm",
  "type": "call.outbound.started",
  "payload": {"...": "..."}
}
```


