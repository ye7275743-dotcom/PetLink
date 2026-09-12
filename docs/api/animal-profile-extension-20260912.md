# 动物完整档案扩展（2026-09-12）

本扩展保持原有 72 个 Frozen REST 路由、方法、鉴权和状态机不变，只在动物请求与响应中增加两个可选字段：

| 字段 | 类型 | 长度 | 含义 |
|---|---|---:|---|
| `personality` | string / null | 1000 | 性格、行为及与人或其他动物相处观察 |
| `adoptionRequirements` | string / null | 1000 | 居住、陪伴、运动、医疗与回访等领养要求 |

涉及的既有接口：

- `POST /api/rescue-tasks/{taskId}/result`：救助成功创建动物档案时可提交。
- `PATCH /api/animals/{animalId}`：管理员或负责救助人员可补充、修改或清空。
- 动物摘要与详情响应：以同名字段返回，旧数据返回 `null`。

部署顺序：

1. 备份数据库。
2. 旧数据库优先执行兼容总迁移 `sql/migrate-existing-to-20260912.sql`；确认已经完成 2026-09-04 用户表迁移时，也可只执行 `sql/migrate-20260912-animal-profile.sql`。
3. 发布新版后端。
4. 发布 PC 与移动 H5 前端。
5. 使用 `scripts/seed-demo-data.mjs` 创建独立演示批次；不要在真实生产数据中冒充案例身份。

前端对旧版后端及历史数据保留了“待补充”文案，因此渐进发布期间不会出现空白或页面报错。
