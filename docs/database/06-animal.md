# 06 `animal` 动物档案表

## 1. 表用途

`animal` 保存救助成功后建立的动物档案、当前健康概要、领养状态及乐观锁版本。每个动物必须且只能来源于一个 `RescueTask`，负责人通过任务关系追溯，不冗余保存负责人字段。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 动物 ID |
| `rescue_task_id` | `BIGINT UNSIGNED` | 否 | — | FK | 来源任务，关联 `rescue_task.id`，创建后不可修改 |
| `name` | `VARCHAR(100)` | 否 | — | CHECK | 动物名称，不得为空白 |
| `species` | `VARCHAR(50)` | 否 | — | CHECK | 动物种类，不得为空白 |
| `sex` | `VARCHAR(20)`, ASCII BIN | 否 | `UNKNOWN` | CHECK | `MALE / FEMALE / UNKNOWN` |
| `estimated_age_months` | `SMALLINT UNSIGNED` | 是 | `NULL` | — | 估算年龄，单位月；无法估计时为空 |
| `color` | `VARCHAR(100)` | 是 | `NULL` | — | 毛色或外观颜色 |
| `health_condition` | `VARCHAR(1000)` | 否 | — | CHECK | 当前对外展示的健康概要 |
| `status` | `VARCHAR(20)`, ASCII BIN | 否 | `TREATING` | CHECK | 当前动物状态 |
| `suspend_reason` | `VARCHAR(500)` | 是 | `NULL` | CHECK | 暂停领养原因，仅 `SUSPENDED` 时必填 |
| `version` | `INT UNSIGNED` | 否 | `0` | 乐观锁 | 每次档案或状态更新递增 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 建档时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 自动更新 | 更新时间 |

## 3. 来源任务与负责人

客户端在完成救助时只提交 `task_id` 和一个或多个动物资料。`rescue_task_id` 必须由后端取自当前锁定任务，客户端不得指定或覆盖，创建后不可修改。

```text
Animal.rescue_task_id
→ RescueTask.rescuer_id
→ sys_user.id
```

该关系用于判断负责 `RESCUER`。Animal 的来源任务必须在同一救助完成事务中转为 `SUCCESS`；普通外键不能约束目标任务状态，因此由 UC-04 原子事务保证。

## 4. 状态与暂停原因

合法状态：

```text
TREATING
OBSERVING
AVAILABLE
SUSPENDED
ADOPTED
```

允许的转换：

```text
TREATING → OBSERVING
OBSERVING → AVAILABLE
AVAILABLE ↔ SUSPENDED
AVAILABLE → ADOPTED（仅 UC-06 领养批准事务）
```

`SUSPENDED` 时 `suspend_reason` 必须为有效非空白文本，其他状态必须为 `NULL`。状态及暂停原因必须在同一条条件更新中修改。

## 5. 乐观锁

普通档案维护和全部状态转换均使用：

```sql
version = version + 1
WHERE id = ? AND version = ?
```

并检查 `affectedRows`。使用 MyBatis-Plus 内置更新时由 `@Version` 参与；自定义 XML/SQL 必须手工写入版本条件和递增表达式。

UC-06 即使已经通过 `FOR UPDATE` 锁定动物，也必须递增 `version`，保持版本语义统一。

## 6. 健康概要

`animal.health_condition` 是当前展示概要，`HealthRecord` 是不可变的历史健康过程。新增健康记录默认不自动覆盖概要。

如组合接口同时新增健康记录并更新概要，则两项必须处于同一事务；概要更新使用乐观锁，版本冲突时整体回滚。

## 7. 查询索引

```sql
KEY idx_animal_task_created
    (rescue_task_id, created_at DESC, id DESC)

KEY idx_animal_status_created
    (status, created_at DESC, id DESC)

KEY idx_animal_status_species_created
    (status, species, created_at DESC, id DESC)
```

前者支持任务来源查询，后两者分别支持全部公开动物和按种类筛选。

## 8. 外键与删除策略

`rescue_task_id` 使用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。Animal 不进行物理删除；当前 V1.0 也不增加逻辑删除字段。

> **`animal` 表 V1.0 — Frozen**
