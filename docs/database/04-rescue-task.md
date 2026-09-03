# 04 `rescue_task` 救助任务表

## 1. 表用途

`rescue_task` 保存救助人员接取线索后形成的救助任务、负责人、状态、开始与终止时间以及失败或取消原因。一条线索允许保留多个历史任务，但同一时刻最多一个活动任务。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 救助任务 ID |
| `clue_id` | `BIGINT UNSIGNED` | 否 | — | FK | 来源线索，关联 `rescue_clue.id`，不得设置普通唯一约束 |
| `rescuer_id` | `BIGINT UNSIGNED` | 否 | — | FK | 负责人，关联 `sys_user.id`，创建后不可修改 |
| `status` | `VARCHAR(20)`, ASCII BIN | 否 | `WAITING_START` | CHECK | 当前任务状态 |
| `active_clue_id` | `BIGINT UNSIGNED`, STORED | 是 | 生成值 | UNIQUE | 活动状态时等于 `clue_id`，终态时为 `NULL` |
| `started_at` | `DATETIME` | 是 | `NULL` | CHECK | 实际开始救助时间 |
| `finished_at` | `DATETIME` | 是 | `NULL` | CHECK | 任务进入任一终态的时间 |
| `failure_reason` | `VARCHAR(500)` | 是 | `NULL` | CHECK | `FAILED` 时必填 |
| `cancel_reason` | `VARCHAR(500)` | 是 | `NULL` | CHECK | `CANCELED` 时必填 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | CHECK | 接取成功并创建任务的时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 自动更新 | 更新时间 |

## 3. 状态机与字段组合

```text
WAITING_START：started_at = NULL，finished_at = NULL
IN_PROGRESS：started_at IS NOT NULL，finished_at = NULL
SUCCESS：started_at / finished_at 均非空
FAILED：时间均非空，failure_reason 为有效非空白文本
CANCELED：finished_at 非空，cancel_reason 为有效非空白文本，started_at 可空
```

`SUCCESS / FAILED / CANCELED` 均为当前任务终态。数据库限制状态值及字段组合；状态转换合法性由 Service 和带原状态条件的更新保证。

## 4. 单活动任务硬约束

```sql
active_clue_id = CASE
    WHEN status IN ('WAITING_START', 'IN_PROGRESS') THEN clue_id
    ELSE NULL
END
```

```sql
CONSTRAINT uk_rescue_task_active_clue
UNIQUE (active_clue_id)
```

活动任务生成相同的非空 `active_clue_id`，同一线索最多存在一条；历史终态任务均生成 `NULL`，不会破坏历史任务模型。真实外键仍由 `clue_id → rescue_clue.id` 保证，生成列不另设外键。

## 5. 时间规则

```text
created_at <= started_at（如有）
created_at <= finished_at（如有）
started_at <= finished_at（两者均有时）
```

业务时间均由后端通过数据库当前时间生成，客户端不得提交或覆盖。

## 6. 状态更新规范

状态及配套时间、原因必须在同一条 SQL 中修改，并检查 `affectedRows`。例如开始救助：

```sql
UPDATE rescue_task
SET status = 'IN_PROGRESS',
    started_at = NOW(),
    updated_at = NOW()
WHERE id = ?
  AND rescuer_id = ?
  AND status = 'WAITING_START';
```

失败、成功和取消同样必须同时更新 `status`、对应原因及 `finished_at`。`affectedRows = 0` 时不得继续后续业务，应查询最新任务并返回对应错误。

## 7. Task 与 Clue 联动

涉及既有 `RescueTask` 与 `RescueClue` 的写事务统一按照 `RescueTask → RescueClue` 顺序锁定。客户端只提交 `task_id`；关联线索 ID 必须从锁定后的 `RescueTask.clue_id` 取得。

ADMIN 取消任务必须在同一事务内完成 `Task → CANCELED`、`Clue CONVERTED → WAITING_ACCEPT` 与两条关键日志。救助成功必须原子完成 `Task → SUCCESS`、创建至少一个 `Animal`、`Clue → CLOSED` 与关键日志。任一步失败均整体回滚。

## 8. 索引

```sql
KEY idx_rescue_task_clue_created
    (clue_id, created_at DESC, id DESC)

KEY idx_rescue_task_rescuer_status_created
    (rescuer_id, status, created_at DESC, id DESC)

KEY idx_rescue_task_status_created
    (status, created_at DESC, id DESC)
```

列表查询统一采用 `ORDER BY created_at DESC, id DESC`。

## 9. 外键策略

`clue_id` 与 `rescuer_id` 均采用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。`rescuer_id` 取自认证上下文，客户端不得指定；`RESCUER` 角色由 Service 校验。

> **`rescue_task` 表 V1.0 — Frozen**
