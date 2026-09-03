# 16 `temporary_file` 临时上传文件技术支撑表

## 1. 表用途

`temporary_file` 保存服务端临时上传 token、所有者、临时路径、文件属性、有效期和一次性业务绑定信息。它是物理数据库技术支撑表，不进入核心领域 ER 图。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 临时文件 ID |
| `token` | `CHAR(36)`, ASCII BIN | 否 | — | UNIQUE, CHECK | 服务端签发的小写 UUID token |
| `owner_id` | `BIGINT UNSIGNED` | 否 | — | FK | 上传用户 |
| `temp_path` | `VARCHAR(500)`, ASCII BIN | 否 | — | UNIQUE, CHECK | 非空白临时文件相对路径 |
| `file_extension` | `VARCHAR(10)`, ASCII BIN | 否 | — | CHECK | `jpg / jpeg / png`，不含点号 |
| `mime_type` | `VARCHAR(50)`, ASCII BIN | 否 | — | CHECK | `image/jpeg / image/png` |
| `file_size_bytes` | `BIGINT UNSIGNED` | 否 | — | CHECK | 文件大小，1～5242880 字节 |
| `status` | `VARCHAR(20)`, ASCII BIN | 否 | `UPLOADED` | CHECK | `UPLOADED / BOUND` |
| `business_type` | `VARCHAR(30)`, ASCII BIN | 是 | `NULL` | CHECK | 绑定业务类型 |
| `business_id` | `BIGINT UNSIGNED` | 是 | `NULL` | 逻辑关联, CHECK | 绑定业务父对象 ID；绑定后必须大于 0 |
| `formal_path` | `VARCHAR(500)`, ASCII BIN | 是 | `NULL` | UNIQUE, CHECK | 绑定后对应的非空白正式相对路径 |
| `expires_at` | `DATETIME` | 否 | — | CHECK | 临时 token 过期时间 |
| `bound_at` | `DATETIME` | 是 | `NULL` | CHECK | 成功绑定时间 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | CHECK | 上传记录创建时间 |

本表不包含 `updated_at / deleted / version`。完成技术清理后物理删除记录。

## 3. Token、路径与文件属性

`token` 使用标准 UUID 小写形式。数据库最低保证 36 字符且为小写，Service 继续通过 UUID 解析或正则验证完整格式。服务端生成的 `temp_path / formal_path` 不得包含首尾空白，且有效路径不得为空白。

客户端只能持有 token，不得获取或提交 `temp_path / formal_path`。两个路径都只保存相对路径；服务端解析、规范化后必须验证真实路径仍位于配置的 `temp/uploads` 根目录内。

扩展名、MIME 对应关系和 5 MB 上限同时由 Service 文件头检查与数据库 CHECK 保证：

```text
jpg / jpeg → image/jpeg
png        → image/png
0 < file_size_bytes <= 5242880
```

## 4. 生命周期状态

```text
UPLOADED
→ business_type = NULL
→ business_id = NULL
→ formal_path = NULL
→ bound_at = NULL

BOUND
→ business_type / business_id / formal_path / bound_at 均非空
→ business_id > 0
→ formal_path 去除首尾空白后仍有内容
→ created_at <= bound_at < expires_at
```

允许绑定的业务类型只有：

```text
RESCUE_CLUE
ANIMAL
FOLLOW_UP
```

`business_id` 是对应业务父对象 ID，采用多态逻辑关联，不建立业务 FK。BOUND 分支必须同时显式校验 `business_id IS NOT NULL` 与 `business_id > 0`；不能只依赖大小比较，因为 SQL `CHECK` 会接受结果为 `UNKNOWN` 的表达式。`formal_path` 精确指向本 token 对应的正式图片路径。

## 5. 上传与 TTL

默认 TTL 使用配置：

```properties
petlink.file.temporary-ttl=PT1H
```

数据库只保存计算后的 `expires_at`，并保证其晚于 `created_at`。调整为 `PT30M / PT2H` 不需要修改表结构。

上传流程：

```text
服务端校验文件
→ 生成 token 与 temp_path
→ 写入临时物理文件
→ INSERT TemporaryFile(UPLOADED)
→ 返回 token
```

物理文件写入失败时不创建数据库行；物理文件已写入但数据库插入失败时，立即删除本次临时文件。

## 6. 一次性绑定与加锁

多个 token 必须先校验数量并去重，再按 `temporary_file.id ASC` 执行 `FOR UPDATE`。数据库锁顺序不改变客户端 token 原顺序对应的图片 `sort_order`。

统一条件绑定：

```sql
UPDATE temporary_file
SET status = 'BOUND',
    business_type = ?,
    business_id = ?,
    formal_path = ?,
    bound_at = NOW()
WHERE id = ?
  AND owner_id = ?
  AND status = 'UPLOADED'
  AND expires_at > NOW();
```

每条 `affectedRows` 必须为 1，否则整个业务事务回滚。`business_type / business_id / formal_path` 必须由 Service 根据本次锁定或创建的业务数据生成，客户端不得指定。

## 7. UPLOADED 清理

定时任务处理 `status = UPLOADED AND expires_at < NOW()`：

```text
规范化 temp_path 并确认位于 temp 根目录
→ 删除临时物理文件
→ 文件已删除或本就不存在
→ DELETE temporary_file
```

过期 token 不允许再次绑定。不增加 `EXPIRED / CLEANED` 等额外状态。

## 8. BOUND 精确核验与清理

清理任务根据 `business_type + business_id + formal_path` 精确验证正式图片关联：

```text
RESCUE_CLUE → rescue_clue_image(clue_id, image_path)
ANIMAL      → animal_image(animal_id, image_path)
FOLLOW_UP   → follow_up_image(follow_up_id, image_path)
```

决策固定为：

```text
正式图片数据库关联不存在
→ ERROR，什么都不删

正式关联存在但 formal_path 物理文件不存在
→ ERROR，什么都不删

正式关联与正式文件均存在，temp_path 也存在
→ 删除临时文件
→ 删除 TemporaryFile 行

正式关联与正式文件均存在，temp_path 已不存在
→ 视为上次已经完成文件删除
→ 删除残留 TemporaryFile 行
```

临时副本可能是正式图片的唯一恢复来源，因此正式关联或文件异常时不得自动删除。清理操作必须验证规范化真实路径位于预期根目录，并记录异常。

## 9. 正式文件孤立清理协作

正式文件孤立扫描继续使用 24 小时安全窗口，并确认三个正式图片表均无引用、不存在有效临时绑定流程、规范化路径位于上传根目录，全部满足后才允许删除。

## 10. 索引

```sql
UNIQUE(token)
UNIQUE(temp_path)
UNIQUE(formal_path)

CHECK(temp_path = TRIM(temp_path) AND CHAR_LENGTH(temp_path) > 0)

KEY idx_temporary_file_owner_status_expires
    (owner_id, status, expires_at, id)

KEY idx_temporary_file_status_expires
    (status, expires_at, id)

KEY idx_temporary_file_business
    (business_type, business_id)
```

## 11. 外键与删除策略

`owner_id` 使用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。绑定业务采用多态逻辑关联，不设置物理 FK。临时记录在物理文件清理完成后物理删除。

> **`temporary_file` 表 V1.0 — Frozen**
