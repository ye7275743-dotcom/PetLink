# 05 `rescue_record` 救助过程记录表

## 1. 表用途

`rescue_record` 保存救助人员在任务执行期间追加的过程记录。一个 `IN_PROGRESS` 任务可以包含多条记录；记录创建后只读，V1.0 不提供修改或删除功能。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 救助过程记录 ID |
| `task_id` | `BIGINT UNSIGNED` | 否 | — | FK | 所属任务，关联 `rescue_task.id` |
| `recorder_id` | `BIGINT UNSIGNED` | 否 | — | FK | 实际记录人，关联 `sys_user.id` |
| `content` | `VARCHAR(2000)` | 否 | — | CHECK | 救助过程内容，不得为空白 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 实际提交时间 |

## 3. 所有权与并发规则

创建记录必须满足：

```text
recorder_id = 当前认证用户.id
recorder_id = RescueTask.rescuer_id
RescueTask.status = IN_PROGRESS
```

`recorder_id` 从认证上下文取得，客户端不得指定。添加前必须开启事务并锁定任务：

```sql
SELECT id, rescuer_id, status
FROM rescue_task
WHERE id = ?
FOR UPDATE;
```

校验通过后才能插入记录。添加记录与 ADMIN 取消、RESCUER 完成或失败操作竞争同一任务行锁，从而避免向终态任务追加记录。

## 4. 只读规则

V1.0 只允许新增，不允许修改或删除 `RescueRecord`。因此不增加 `updated_at` 或 `deleted`；通过 `recorder_id` 与 `created_at` 保留提交历史。

## 5. 索引

```sql
KEY idx_rescue_record_task_created (task_id, created_at, id)
KEY idx_rescue_record_recorder (recorder_id)
```

查询统一采用 `WHERE task_id = ? ORDER BY created_at ASC, id ASC`。

## 6. 外键策略

`task_id` 与 `recorder_id` 均采用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`，不级联删除历史过程记录。

> **`rescue_record` 表 V1.0 — Frozen**
