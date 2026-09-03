# 02 `rescue_clue` 救助线索表

## 1. 表用途

`rescue_clue` 保存 `USER / RESCUER` 发布的流浪动物救助线索、联系方式快照、审核信息及当前业务状态。线索不进行逻辑删除，业务结束后仍用于任务追溯、动物来源、统计与审计。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 救助线索 ID |
| `publisher_id` | `BIGINT UNSIGNED` | 否 | — | FK | 发布人，关联 `sys_user.id` |
| `location` | `VARCHAR(255)` | 否 | — | CHECK | 发现地点，不得为空白 |
| `found_time` | `DATETIME` | 否 | — | — | 发现时间，合理性由 Service 校验 |
| `animal_description` | `VARCHAR(1000)` | 否 | — | CHECK | 动物描述，不得为空白 |
| `scene_description` | `VARCHAR(1000)` | 是 | `NULL` | — | 现场补充描述；空字符串由 Service 转为 `NULL` |
| `contact` | `VARCHAR(100)` | 否 | — | CHECK | 本条线索的联系方式快照，不得为空白 |
| `status` | `VARCHAR(20)`, ASCII BIN | 否 | `PENDING_REVIEW` | CHECK | 当前线索状态 |
| `reviewer_id` | `BIGINT UNSIGNED` | 是 | `NULL` | FK/CHECK | 审核人，关联 `sys_user.id` |
| `reviewed_at` | `DATETIME` | 是 | `NULL` | CHECK | 审核时间 |
| `reject_reason` | `VARCHAR(500)` | 是 | `NULL` | CHECK | 驳回原因，仅 `REJECTED` 时必填 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 自动更新 | 更新时间 |

## 3. 状态与审核字段

合法状态：

```text
PENDING_REVIEW
REJECTED
WITHDRAWN
WAITING_ACCEPT
CONVERTED
CLOSED
```

数据库约束状态值及审核字段组合：

```text
PENDING_REVIEW / WITHDRAWN
→ reviewer_id = NULL
→ reviewed_at = NULL

REJECTED / WAITING_ACCEPT / CONVERTED / CLOSED
→ reviewer_id IS NOT NULL
→ reviewed_at IS NOT NULL

REJECTED
→ reject_reason 为有效非空白文本

非 REJECTED
→ reject_reason = NULL

reviewed_at 非空
→ reviewed_at >= created_at
```

审核人必须为 `ADMIN`、发布人必须为 `USER / RESCUER`，由 Service 鉴权；普通外键不能约束用户角色。状态迁移合法性通过 Service 与带原状态条件的 `UPDATE` 保证。

数据库通过 `chk_rescue_clue_reviewed_at` 防止审核时间早于线索创建时间。

## 4. 联系方式快照

`contact` 是线索发布时的业务快照，不是 `sys_user.phone` 的动态引用。用户以后修改个人手机号不会改变历史线索联系方式。

## 5. 单活动任务约束

一条线索允许保留多个历史 `RescueTask`，但同一时刻最多一个状态为 `WAITING_START / IN_PROGRESS` 的活动任务。该约束通过线索状态条件更新与 UC-03 创建任务的同一事务保证，不对 `rescue_task.clue_id` 设置普通唯一约束。

## 6. 查询索引

```sql
KEY idx_rescue_clue_publisher_status_created
    (publisher_id, status, created_at DESC, id DESC)

KEY idx_rescue_clue_status_created
    (status, created_at DESC, id DESC)

KEY idx_rescue_clue_reviewer
    (reviewer_id)
```

列表查询统一采用：

```sql
ORDER BY created_at DESC, id DESC
```

暂不增加 `(publisher_id, created_at, id)` 索引；待 REST API 查询模式确定后再依据真实高频接口评估。

## 7. 外键策略

发布人和审核人均使用：

```text
ON UPDATE RESTRICT
ON DELETE RESTRICT
```

禁止因删除上游用户而级联删除历史线索。

> **`rescue_clue` 表 V1.0 — Frozen**
