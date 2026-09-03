# 08 `health_record` 动物健康记录表

## 1. 表用途

`health_record` 保存动物随时间追加的健康过程记录。记录创建后只读，V1.0 不提供普通修改或删除功能。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 健康记录 ID |
| `animal_id` | `BIGINT UNSIGNED` | 否 | — | FK | 所属动物，关联 `animal.id` |
| `recorder_id` | `BIGINT UNSIGNED` | 否 | — | FK | 实际记录人，关联 `sys_user.id` |
| `content` | `VARCHAR(2000)` | 否 | — | CHECK | 健康记录内容，不得为空白 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 记录时间 |

## 3. 权限规则

`recorder_id` 必须从认证上下文取得，客户端不得指定。

```text
负责 RESCUER：Animal → RescueTask.rescuer_id = 当前用户
ADMIN：允许为任意 Animal 添加记录
```

`HealthRecord.recorder_id` 不要求等于任务负责人，因为 ADMIN 也可以添加记录。新增健康记录不设置 Animal 状态限制。

## 4. 写入与锁

普通新增健康记录不强制锁定 Animal，因为不存在基于动物状态的写入限制，Animal 与 RescueTask 均不物理删除，来源和负责人字段创建后不可修改。

如使用“新增记录并更新健康概要”的组合接口，则必须开启事务，插入记录后通过 `version` 乐观锁更新 Animal；乐观锁失败时整体回滚。

## 5. 只读与索引

记录创建后只读，不增加 `updated_at` 或 `deleted`。

```sql
KEY idx_health_record_animal_created
    (animal_id, created_at, id)

KEY idx_health_record_recorder
    (recorder_id)
```

健康时间线使用 `ORDER BY created_at ASC, id ASC`。

## 6. 外键策略

`animal_id` 与 `recorder_id` 均采用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。

> **`health_record` 表 V1.0 — Frozen**
