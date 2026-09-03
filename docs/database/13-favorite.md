# 13 `favorite` 动物收藏表

## 1. 表用途

`favorite` 保存 `USER / RESCUER` 与动物之间的当前收藏关系。它是轻量关联表，不保存历史取消记录；取消收藏直接物理删除关联。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 收藏 ID |
| `user_id` | `BIGINT UNSIGNED` | 否 | — | FK, UNIQUE 组合 | 收藏用户，来自认证上下文 |
| `animal_id` | `BIGINT UNSIGNED` | 否 | — | FK, UNIQUE 组合 | 被收藏动物 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 收藏时间 |

本表不包含 `status / updated_at / deleted / version`。

## 3. 唯一约束与并发

```sql
CONSTRAINT uk_favorite_user_animal
UNIQUE (user_id, animal_id)
```

同一用户与动物最多存在一条当前收藏关系。业务层可以提前查询以改善提示，但并发防重最终依赖该命名约束；只有该约束冲突可解释为“已收藏”，其他数据库异常必须按真实原因处理。

## 4. 收藏条件与幂等语义

收藏接口首先普通查询当前关系：

```text
已经存在
→ 直接返回“已收藏”
→ 不重新检查 Animal 状态

不存在
→ BEGIN
→ SELECT Animal ... FOR SHARE
→ 确认 Animal.status = AVAILABLE
→ INSERT Favorite
→ COMMIT
```

`FOR SHARE` 只用于不存在收藏时的短事务，保证状态检查与创建关系具有清晰的先后顺序。当前角色必须为 `USER / RESCUER`；`user_id` 从认证上下文取得，客户端不得指定或覆盖。

## 5. 动物状态变化

Animal 后续从 `AVAILABLE` 进入 `SUSPENDED / ADOPTED` 时不自动删除收藏。本人收藏列表可以继续展示动物当前状态，但应关闭不合法的领养操作。

这避免动物状态事务联动高频轻量收藏表，并允许用户自行取消历史收藏。

## 6. 取消收藏

```sql
DELETE FROM favorite
WHERE user_id = ?
  AND animal_id = ?;
```

`user_id` 必须来自认证上下文。`affectedRows = 1` 表示本次删除成功，`affectedRows = 0` 表示原本就未收藏；二者都视为正常的最终“未收藏”状态。取消不读取或锁定 Animal。

## 7. 查询索引

```sql
KEY idx_favorite_user_created
    (user_id, created_at DESC, id DESC)

KEY idx_favorite_animal
    (animal_id)
```

前者支持本人收藏列表，后者支持按动物进行反向关联及显式满足外键索引要求。存在性查询由 `(user_id, animal_id)` 唯一索引支持。

## 8. 审计与删除策略

收藏和取消收藏不属于关键审核或核心状态迁移，V1.0 不写 `OperationLog`。取消收藏采用物理删除，不使用逻辑删除。

两个外键均使用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。

> **`favorite` 表 V1.0 — Frozen**
