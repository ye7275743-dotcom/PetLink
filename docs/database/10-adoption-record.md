# 10 `adoption_record` 成功领养记录表

## 1. 表用途

`adoption_record` 是 UC-06 批准领养申请时产生的不可变事实记录，保存来源申请、被领养动物、最终领养人和领养生效时间。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 领养记录 ID |
| `application_id` | `BIGINT UNSIGNED` | 否 | — | FK, UNIQUE | 来源领养申请 |
| `animal_id` | `BIGINT UNSIGNED` | 否 | — | FK, UNIQUE | 被领养动物 |
| `user_id` | `BIGINT UNSIGNED` | 否 | — | FK | 最终领养人 |
| `adopted_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 领养生效业务时间，由数据库时钟产生 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 记录创建时间 |

本表不包含 `updated_at / version / deleted`。

## 3. 唯一创建路径与不可变性

唯一创建路径是 UC-06 的领养批准事务。客户端只提交申请 ID 和审核动作；后端从锁定后的 `AdoptionApplication` 复制：

```text
application_id = Application.id
animal_id      = Application.animal_id
user_id        = Application.user_id
adopted_at     = NOW()
created_at     = NOW()
```

所有字段创建后均不可修改，记录不得删除。V1.0 中 ADMIN 的“管理领养记录”仅指查询、查看详情、业务监管和关联追踪，不包含手工新增、修改或删除。

## 4. 冗余一致性规则

```text
AdoptionRecord.user_id
= AdoptionApplication.user_id

AdoptionRecord.animal_id
= AdoptionApplication.animal_id
```

普通外键无法验证上述跨表值相等关系；由 UC-06 对锁定申请进行服务端复制并在同一事务创建记录保证。客户端不得指定或覆盖 `user_id / animal_id`。

## 5. 唯一约束

```sql
CONSTRAINT uk_adoption_record_application
UNIQUE (application_id)
```

保证一份申请最多生成一条成功领养记录。

```sql
CONSTRAINT uk_adoption_record_animal
UNIQUE (animal_id)
```

保证一只动物最多生成一条成功领养记录。`user_id` 不唯一，同一用户历史上可以领养多只动物。

## 6. 批准事务

本表记录必须与以下操作处于同一数据库事务：

```text
当前 AdoptionApplication → APPROVED
→ 创建 AdoptionRecord
→ Animal AVAILABLE → ADOPTED，version++
→ 同动物其他 PENDING → INVALIDATED
→ 写入全部关键 OperationLog
→ COMMIT
```

任一步失败均整体回滚，不允许单独创建或补录 `AdoptionRecord`。

## 7. 查询索引

```sql
KEY idx_adoption_record_user_adopted
    (user_id, adopted_at DESC, id DESC)

KEY idx_adoption_record_adopted
    (adopted_at DESC, id DESC)
```

前者支持领养人查看本人历史，后者支持 ADMIN 全局分页。按 `application_id` 和 `animal_id` 查询分别由对应唯一索引支持。

## 8. 外键与删除策略

`application_id`、`animal_id`、`user_id` 均采用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。`AdoptionRecord` 作为历史事实不进行物理删除或逻辑删除。

> **`adoption_record` 表 V1.0 — Frozen**
