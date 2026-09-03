# 09 `adoption_application` 领养申请表

## 1. 表用途

`adoption_application` 保存 `USER / RESCUER` 针对可领养动物提交的申请快照、审核结论及状态。申请正文创建后不可修改，记录不删除；同一用户对同一动物历史上最多提交一次申请。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 申请 ID |
| `user_id` | `BIGINT UNSIGNED` | 否 | — | FK, UNIQUE 组合 | 申请人，来自认证上下文，创建后不可修改 |
| `animal_id` | `BIGINT UNSIGNED` | 否 | — | FK, UNIQUE 组合 | 申请动物，创建后不可修改 |
| `adoption_reason` | `VARCHAR(1000)` | 否 | — | CHECK | 领养原因快照，不得为空白 |
| `housing_condition` | `VARCHAR(1000)` | 否 | — | CHECK | 居住情况快照，不得为空白 |
| `family_members` | `VARCHAR(1000)` | 否 | — | CHECK | 家庭成员情况快照，不得为空白 |
| `pet_experience` | `VARCHAR(1000)` | 否 | — | CHECK | 养宠经验快照，不得为空白 |
| `contact` | `VARCHAR(100)` | 否 | — | CHECK | 本次申请联系方式快照，不得为空白 |
| `status` | `VARCHAR(20)`, ASCII BIN | 否 | `PENDING` | CHECK | 申请状态 |
| `reviewer_id` | `BIGINT UNSIGNED` | 是 | `NULL` | FK, CHECK | 执行批准或拒绝的 ADMIN |
| `reviewed_at` | `DATETIME` | 是 | `NULL` | CHECK | 审核时间，不能早于创建时间 |
| `reject_reason` | `VARCHAR(500)` | 是 | `NULL` | CHECK | 仅 `REJECTED` 时必填 |
| `approved_animal_id` | `BIGINT UNSIGNED`, STORED | 是 | 生成值 | UNIQUE | `APPROVED` 时等于 `animal_id`，用于单动物单批准约束 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 申请提交时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 自动更新 | 最后状态更新时间 |

## 3. 申请快照与不可变字段

申请正文与联系方式表示提交时的业务快照，后续用户修改个人资料不得反向修改历史申请。以下字段创建后禁止业务更新：

```text
user_id
animal_id
adoption_reason
housing_condition
family_members
pet_experience
contact
created_at
```

V1.0 不为申请提供修改或删除能力；Mapper 更新语句必须采用字段白名单，只允许合法状态转换及其配套审核字段发生变化。

## 4. 状态与审核字段组合

合法状态：

```text
PENDING / APPROVED / REJECTED / WITHDRAWN / INVALIDATED
```

字段组合：

```text
PENDING / WITHDRAWN / INVALIDATED
→ reviewer_id = NULL
→ reviewed_at = NULL
→ reject_reason = NULL

APPROVED
→ reviewer_id、reviewed_at 非空
→ reject_reason = NULL

REJECTED
→ reviewer_id、reviewed_at 非空
→ reject_reason 为有效非空白文本
```

`INVALIDATED` 是其他申请获批后由系统产生的自动失效，不表示 ADMIN 对该申请作出了独立审核结论，因此不填写审核人和审核时间。自动失效原因通过 `OperationLog` 追踪。

审核人不得与申请人为同一账户；审核时间不得早于申请创建时间。审核人是否确为 `ADMIN` 由 Service 校验。

## 5. 生命周期唯一与单动物单批准

```sql
CONSTRAINT uk_adoption_application_user_animal
UNIQUE (user_id, animal_id)
```

该约束覆盖全部历史状态。即使申请最终为 `REJECTED / WITHDRAWN / INVALIDATED`，申请人也不能再次申请同一动物。

```sql
approved_animal_id = CASE
    WHEN status = 'APPROVED' THEN animal_id
    ELSE NULL
END
```

```sql
CONSTRAINT uk_adoption_application_approved_animal
UNIQUE (approved_animal_id)
```

生成列唯一约束保证同一动物最多存在一条 `APPROVED` 申请；非批准申请生成 `NULL`，允许同一动物存在多条历史或待审核申请。生成列不另设外键。

## 6. 创建、撤回与审计

创建申请必须在一个事务内完成：

```text
锁定 Animal 并确认 AVAILABLE
→ 创建 AdoptionApplication(PENDING)
→ 写 CREATE OperationLog
→ COMMIT
```

撤回只允许本人对 `PENDING` 申请执行条件更新：

```text
BEGIN
→ PENDING → WITHDRAWN
→ affectedRows = 1
→ 写 WITHDRAW OperationLog
→ COMMIT
```

任一日志写入失败均回滚对应业务事务。

## 7. 审核并发与锁顺序

批准事务统一按照以下顺序加锁：

```text
Animal
→ 当前 AdoptionApplication
→ 同动物其他 PENDING AdoptionApplication（按 id 升序）
```

批准时原子完成当前申请 `APPROVED`、创建 `AdoptionRecord`、动物 `ADOPTED + version++`、其他申请 `INVALIDATED` 以及全部关键日志。拒绝只锁定当前 `AdoptionApplication`，不锁 `Animal`，并在同一事务写入 `AUDIT_REJECT` 日志。

状态更新必须带原状态条件并检查 `affectedRows`。申请正文不可修改，因此不增加 `version`。

## 8. 查询索引

```sql
KEY idx_adoption_application_user_created
    (user_id, created_at DESC, id DESC)

KEY idx_adoption_application_status_created
    (status, created_at ASC, id ASC)

KEY idx_adoption_application_animal_status_id
    (animal_id, status, id)

KEY idx_adoption_application_created
    (created_at DESC, id DESC)
```

分别支持本人历史、ADMIN 按提交顺序审核、同动物申请处理和 ADMIN 全局分页。

## 9. 外键与删除策略

`user_id`、`animal_id`、`reviewer_id` 均采用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。申请不物理删除，也不设置会绕过生命周期唯一规则的逻辑删除字段。

> **`adoption_application` 表 V1.0 — Frozen**
