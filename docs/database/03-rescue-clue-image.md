# 03 `rescue_clue_image` 救助线索图片表

## 1. 表用途

`rescue_clue_image` 保存救助线索与正式现场图片的关联。数据库只保存正式相对路径，不保存客户端路径、临时路径或服务器绝对路径。

每条 `RescueClue` 必须关联 1～9 张图片。数据库通过排序位置约束最多 9 张，至少 1 张由 Service 事务保证。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 图片关联 ID |
| `clue_id` | `BIGINT UNSIGNED` | 否 | — | FK | 所属线索，关联 `rescue_clue.id` |
| `image_path` | `VARCHAR(500)`, ASCII BIN | 否 | — | UNIQUE | 正式图片相对路径 |
| `sort_order` | `TINYINT UNSIGNED` | 否 | — | CHECK/UNIQUE | 稳定展示顺序，范围 1～9 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |

## 3. 图片数量与顺序

`sort_order` 表示图片上传后的稳定展示顺序，V1.0 不提供任意拖拽换位功能。

新增图片时：

```text
BEGIN
→ SELECT RescueClue ... FOR UPDATE
→ 校验 publisher_id = 当前用户且 status = PENDING_REVIEW
→ 查询数量与 MAX(sort_order)
→ 确认数量 < 9
→ 新图片 sort_order = MAX(sort_order) + 1
→ INSERT
→ COMMIT
```

删除图片时：

```text
BEGIN
→ 锁定父 RescueClue
→ 校验本人且状态为 PENDING_REVIEW
→ 删除目标图片关联
→ 确认剩余数量 >= 1
→ 对删除位置后的图片按原 sort_order 升序逐条前移
→ COMMIT
→ 尝试删除已解除引用的正式文件
```

删除位置 `k` 后，按照 `k+1 → k、k+2 → k+1……` 的顺序逐条更新。因为前一个位置已空出，升序移动不会触发 `(clue_id, sort_order)` 唯一约束。

所有既有线索的图片增加、删除和连续编号整理都必须先锁定父线索，使同一线索的图片修改请求串行执行。任意拖拽交换不进入 V1.0。

## 4. 数据库约束

```sql
CONSTRAINT uk_rescue_clue_image_clue_sort
UNIQUE (clue_id, sort_order)

CONSTRAINT uk_rescue_clue_image_path
UNIQUE (image_path)

CONSTRAINT chk_rescue_clue_image_sort_order
CHECK (sort_order BETWEEN 1 AND 9)
```

`(clue_id, sort_order)` 已能支持 `WHERE clue_id = ? ORDER BY sort_order`，不再额外建立重复的 `INDEX(clue_id)`。

## 5. 文件删除与孤立文件扫描

文件删除采用“数据库先提交，文件后删除”：

```text
COMMIT 图片关联删除
→ 尝试删除正式文件
→ 删除失败时记录 ERROR 日志
→ 等待定时孤立文件扫描兜底
```

不得先删除磁盘文件再提交数据库事务。

V1.0 不增加 `file_cleanup_task` 表。孤立正式文件扫描只处理最后修改时间早于 24 小时安全窗口的文件，并同时确认：

1. `rescue_clue_image` 中无引用；
2. `animal_image` 中无引用；
3. `follow_up_image` 中无引用；
4. `temporary_file` 中不存在未过期且处于有效绑定流程的记录；
5. 规范化后的真实路径仍位于配置的 `uploads` 根目录内。

全部满足后才允许物理删除。安全实现应解析真实路径后验证 `realFilePath.startsWith(realUploadsRoot)`，禁止依赖未经规范化的字符串拼接。

## 6. 外键策略

```text
rescue_clue_image.clue_id → rescue_clue.id
ON UPDATE RESTRICT
ON DELETE RESTRICT
```

图片关联不使用逻辑删除。

> **`rescue_clue_image` 表 V1.0 — Frozen**
