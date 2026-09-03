# 12 `follow_up_image` 领养回访图片表

## 1. 表用途

`follow_up_image` 保存一次领养回访与正式生活照片的关联。数据库只保存服务端生成的正式相对路径，不接受客户端文件路径。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 回访图片 ID |
| `follow_up_id` | `BIGINT UNSIGNED` | 否 | — | FK, UNIQUE 组合 | 所属回访记录 |
| `image_path` | `VARCHAR(500)`, ASCII BIN | 否 | — | UNIQUE | 正式图片相对路径 |
| `sort_order` | `TINYINT UNSIGNED` | 否 | — | CHECK, UNIQUE 组合 | 本次提交中的展示顺序，范围 1～9 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |

## 3. 图片数量与顺序

一条回访允许 0～9 张图片。存在图片时，`sort_order` 按客户端原始 `imageTokens[]` 顺序写入连续的 `1..N`：

```text
数据库锁 temporary_file 的顺序：id ASC
图片最终 sort_order：客户端 token 原始顺序
```

回访及图片创建后只读，不支持追加、删除、拖拽换位或重新排序。

## 4. 约束

```sql
CONSTRAINT uk_follow_up_image_follow_up_sort
UNIQUE (follow_up_id, sort_order)

CONSTRAINT uk_follow_up_image_path
UNIQUE (image_path)

CONSTRAINT chk_follow_up_image_sort_order
CHECK (sort_order BETWEEN 1 AND 9)
```

`(follow_up_id, sort_order)` 同时限制最多 9 个展示位置并支持 `WHERE follow_up_id = ? ORDER BY sort_order`，不再增加重复的 `follow_up_id` 普通索引。

## 5. 文件绑定与生命周期

客户端只能提交服务器签发的临时文件 token。后端验证 token 所有权、状态、有效期、格式、MIME、大小和物理文件后生成正式 UUID 相对路径。

图片关联、`TemporaryFile → BOUND` 和正式文件复制发生在 UC-07 创建事务中；数据库提交后再删除原临时物理文件。失败补偿只能删除能够确认由当前事务创建的正式副本，不得处理其他临时文件或客户端提供的路径。

## 6. 外键与删除策略

`follow_up_id` 使用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。图片关联创建后不可修改、不可删除，不使用逻辑删除。

> **`follow_up_image` 表 V1.0 — Frozen**
