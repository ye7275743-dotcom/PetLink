# 11 `follow_up_record` 领养回访记录表

## 1. 表用途

`follow_up_record` 保存领养人针对本人成功领养记录提交的后续生活与健康情况。一次领养允许产生多次回访；每条回访创建后只读，不提供编辑或删除能力。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 回访记录 ID |
| `adoption_record_id` | `BIGINT UNSIGNED` | 否 | — | FK | 对应成功领养记录，不唯一 |
| `submitter_id` | `BIGINT UNSIGNED` | 否 | — | FK, UNIQUE 组合 | 实际提交人，必须为该领养记录的领养人 |
| `content` | `VARCHAR(2000)` | 是 | `NULL` | CHECK | 生活情况或文字描述 |
| `health_condition` | `VARCHAR(1000)` | 是 | `NULL` | CHECK | 当前健康情况 |
| `idempotency_key` | `VARCHAR(64)`, ASCII BIN | 否 | — | UNIQUE 组合, CHECK | 小写规范 UUID 幂等键 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 回访提交时间 |

本表不包含 `status / updated_at / version / deleted`。

## 3. 所有权与多次回访

```text
FollowUpRecord.submitter_id
= AdoptionRecord.user_id
```

客户端不得提交或覆盖 `submitter_id`。后端普通读取不可变的 `AdoptionRecord`，确认其 `user_id` 等于当前认证用户后，再从认证上下文设置提交人。原救助 `RESCUER` 仅可只读查看本人负责动物的回访，不能代替领养人提交。

`adoption_record_id` 不设置唯一约束，保持：

```text
AdoptionRecord 1 → 0..N FollowUpRecord
```

## 4. 有效内容与空值规范

每次回访至少包含以下一项：

```text
trim(content) 后的有效文字
trim(health_condition) 后的有效健康情况
至少一张验证通过的生活照片
```

空字符串及纯空白文本由 Service 统一转换为 `NULL`。数据库分别禁止非空字段保存纯空白内容；由于图片位于子表，“文字、健康情况、图片至少一项有效”的跨表约束由 Service 在事务提交前保证，不使用触发器或冗余图片计数字段。

## 5. 幂等键

`idempotency_key` 使用客户端生成的标准 UUID。服务端校验格式并规范化为小写后写入；数据库最低保证无首尾空白、长度为 36 且全部为小写。字段随 `FollowUpRecord` 永久保存，创建后不可修改、不可释放、没有过期时间。

```sql
CONSTRAINT uk_follow_up_submitter_idempotency
UNIQUE (submitter_id, idempotency_key)

CONSTRAINT chk_follow_up_idempotency_key
CHECK (
    idempotency_key = TRIM(idempotency_key)
    AND CHAR_LENGTH(idempotency_key) = 36
    AND idempotency_key = LOWER(idempotency_key)
)
```

同一用户和同一幂等键永远表示第一次逻辑提交。发生该命名约束冲突时，事务回滚后查询已有记录：

```text
已有 adoption_record_id = 本次目标
→ 返回第一次成功结果

已有 adoption_record_id != 本次目标
→ 拒绝，幂等键已用于其他业务请求
```

V1.0 不保存请求哈希。同一 key、同一领养记录的重试即使正文不同，也以第一次成功提交为准；真正的新回访必须使用新 key。其他唯一约束或数据库完整性错误不得解释为幂等重试。

## 6. 创建事务与临时图片

无图片时，事务创建 `FollowUpRecord` 即占用幂等键。有图片时，正式顺序为：

```text
验证 AdoptionRecord 所有权
→ 规范化文字和幂等键
→ 校验 imageTokens 数量 0..9 且内部无重复
→ BEGIN
→ INSERT FollowUpRecord，占用幂等键
→ 按 temporary_file.id ASC 批量 FOR UPDATE
→ 查询数量必须等于 token 数量
→ 校验 owner/status/expiry/格式/MIME/大小及物理文件
→ 生成正式 UUID 相对路径
→ 创建 FollowUpImage，sort_order 按客户端 token 原顺序
→ 条件更新每个 TemporaryFile 为 BOUND，affectedRows 均为 1
→ 复制临时文件到正式路径
→ COMMIT
→ 删除本次已成功绑定的原临时物理文件
```

数据库加锁顺序固定为临时文件 ID 升序；图片展示顺序仍使用客户端原始 token 顺序，两者不得混淆。`AdoptionRecord` 创建后不可修改或删除，因此所有权校验不使用 `FOR UPDATE`。

## 7. 文件补偿边界

提交前使用复制而不是移动，保证数据库回滚后合法原临时文件仍存在：

```text
COPY 或数据库事务失败
→ ROLLBACK
→ 仅删除本事务已创建的正式副本
→ 保留合法原临时文件

COMMIT 成功但删除原临时文件失败
→ 不回滚业务
→ 记录 ERROR
→ 由临时文件清理任务处理 BOUND 残余副本
```

业务实现必须维护本事务自己的已验证临时文件、已创建正式路径和已绑定文件集合。不得删除不存在、属于其他用户、已经被其他业务绑定、仅由客户端伪造或无法确认属于本事务的文件。

## 8. 查询索引

```sql
KEY idx_follow_up_adoption_created
    (adoption_record_id, created_at DESC, id DESC)

KEY idx_follow_up_submitter_created
    (submitter_id, created_at DESC, id DESC)

KEY idx_follow_up_created
    (created_at DESC, id DESC)
```

分别支持某次领养的回访列表、本人回访历史和 ADMIN 全局分页。幂等查询由唯一索引支持。

## 9. 审计与删除策略

UC-07 不产生状态转换，`submitter_id + created_at` 已保存提交事实，因此 V1.0 不强制写 `OperationLog`。所有外键采用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`，回访记录不物理删除或逻辑删除。

> **`follow_up_record` 表 V1.0 — Frozen**
