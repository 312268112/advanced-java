### 多租户与隔离（示例/预留）
- 模型：`Tenant` → `Organization` → `User/Contact/Account`
- 隔离：库/Schema优先，行列级补充；ES索引按租户前缀；计算配额。
- 审计：字段级脱敏、访问日志、导出审批。


