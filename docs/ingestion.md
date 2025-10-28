### 采集与消息（示例/预留）
- 接入：API/Webhook/CDC/文件；SDK埋点。
- 消息：优先Kafka；无Kafka时使用DB/ES队列表+幂等。
- 质量：`event_id` 去重，重试/DLQ，Schema Registry。


