# 15 `operation_log` 核心业务操作审计日志表

## 1. 表用途

`operation_log` 以只追加方式保存核心业务对象、操作类型、前后状态、操作者、原因快照和操作时间。关键状态变化与必需日志必须位于同一数据库事务，日志写入失败时业务事务整体回滚。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 操作日志 ID |
| `business_type` | `VARCHAR(40)`, ASCII BIN | 否 | — | CHECK | 业务对象类型 |
| `business_id` | `BIGINT UNSIGNED` | 否 | — | 逻辑关联, CHECK | 对应业务对象 ID，必须大于 0，不建立多态 FK |
| `operation_type` | `VARCHAR(50)`, ASCII BIN | 否 | — | CHECK | 操作类型，由 Java Enum 管理 |
| `before_status` | `VARCHAR(30)`, ASCII BIN | 是 | `NULL` | CHECK | 操作前状态，可为空 |
| `after_status` | `VARCHAR(30)`, ASCII BIN | 是 | `NULL` | CHECK | 操作后状态，可为空 |
| `operator_id` | `BIGINT UNSIGNED` | 否 | — | FK | 实际操作人或触发联动事务的业务发起人 |
| `reason` | `VARCHAR(500)` | 是 | `NULL` | CHECK | 当次操作原因快照 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 操作发生时间 |

本表不包含 `updated_at / deleted / version`。

## 3. 多态业务关联

`business_id` 根据 `business_type` 逻辑关联不同业务表，无法建立单一物理外键。V1.0 允许：

```text
SYS_USER
RESCUE_CLUE
RESCUE_TASK
ANIMAL
ADOPTION_APPLICATION
ANNOUNCEMENT
```

唯一物理外键是 `operator_id → sys_user.id`。业务对象是否真实存在、操作类型与业务类型是否匹配，由对应 Service 在同一事务中保证。

由于全部业务表主键均为从 1 开始的自增正整数，数据库通过 `chk_operation_log_business_id` 禁止写入不存在语义的 `business_id = 0`。

## 4. 正式操作类型映射

Java 层固定校验以下组合：

```text
SYS_USER
├─ ENABLE
├─ DISABLE
├─ PROMOTE_RESCUER
├─ DEMOTE_RESCUER
└─ DELETE_USER

RESCUE_CLUE
├─ CREATE
├─ WITHDRAW
├─ AUDIT_APPROVE
├─ AUDIT_REJECT
├─ ACCEPT_RESCUE
├─ REOPEN
├─ CLOSE_AFTER_RESCUE
└─ CLOSE_AFTER_FAILURE

RESCUE_TASK
├─ START_RESCUE
├─ COMPLETE_RESCUE
├─ RESCUE_FAILED
└─ CANCEL_RESCUE

ANIMAL
├─ TO_OBSERVING
├─ OPEN_ADOPTION
├─ SUSPEND_ADOPTION
├─ RESUME_ADOPTION
└─ ADOPT

ADOPTION_APPLICATION
├─ CREATE
├─ WITHDRAW
├─ AUDIT_APPROVE
├─ AUDIT_REJECT
└─ AUTO_INVALIDATE

ANNOUNCEMENT
├─ CREATE
├─ PUBLISH
├─ WITHDRAW
└─ DELETE_ANNOUNCEMENT
```

数据库不对 `operation_type` 做长枚举 CHECK，只保证非空白；Service 必须验证 `business_type + operation_type` 的合法组合。V1.0 不再使用 `MARK_FAILED / APPROVE / REJECT` 等旧候选名称。

## 5. 状态与原因快照

状态迁移应同时保存 `before_status / after_status`；创建类日志可以使用 `NULL → 初始状态`。角色调整等非状态机操作允许两个状态字段均为 `NULL`。

状态字段和 `reason` 为非空时不得只包含空白。哪些业务操作必须有原因，由各业务状态规则和 Service 校验；日志中的原因是操作发生时的不可变快照。

## 6. Append-only 实现约束

```text
数据库结构：不设计更新、删除或版本字段
Mapper：只暴露 insert / select
Service：不提供 update / delete
数据库权限（部署条件允许）：只授予 SELECT / INSERT
```

业务系统不得提供任何修改或删除审计日志的接口。日志表不设置数据清理周期，V1.0 永久保留。

## 7. 事务规则

```text
关键业务状态变化
+ 对应必需 OperationLog
= 同一数据库事务
```

日志插入失败、事务提交失败或业务状态更新失败时均整体回滚。普通收藏、回访、健康记录、救助过程记录等没有核心状态迁移的轻量操作不机械写入日志。

## 8. 查询索引

```sql
KEY idx_operation_log_business_created
    (business_type, business_id, created_at ASC, id ASC)

KEY idx_operation_log_operator_created
    (operator_id, created_at DESC, id DESC)

KEY idx_operation_log_created
    (created_at DESC, id DESC)
```

分别支持业务对象完整时间线、操作者历史和后台全局最新操作列表。

## 9. 外键与删除策略

`operator_id` 使用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。业务对象采用逻辑多态关联，不设置物理 FK。日志只新增、不修改、不删除。

> **`operation_log` 表 V1.0 — Frozen**
