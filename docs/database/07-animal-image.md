# 07 `animal_image` 动物图片表

## 1. 表用途

`animal_image` 保存动物档案与正式图片的关联。数据库只保存正式相对路径。冻结的“单次最多 9 张”是单次上传限制，不是动物生命周期总数限制。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 动物图片 ID |
| `animal_id` | `BIGINT UNSIGNED` | 否 | — | FK | 所属动物，关联 `animal.id` |
| `image_path` | `VARCHAR(500)`, ASCII BIN | 否 | — | UNIQUE | 正式图片相对路径 |
| `sort_order` | `SMALLINT UNSIGNED` | 否 | — | CHECK/UNIQUE | 稳定上传与展示顺序，从 1 开始 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |

## 3. 上传、删除与顺序

V1.0 不支持任意拖拽换位。新增、删除和连续编号整理必须先锁父 Animal：

```sql
SELECT id, rescue_task_id, status, version
FROM animal
WHERE id = ?
FOR UPDATE;
```

新增时使用 `MAX(sort_order) + 1`，单次请求最多接收 9 张。删除后对后续图片按照原 `sort_order` 升序逐条前移。

父 Animal 锁用于串行化序号计算和图片修改。权限判断通过 `animal.rescue_task_id` 普通读取 `RescueTask.rescuer_id`；不得反向对 RescueTask 使用 `FOR UPDATE`，避免形成 `Animal → RescueTask` 写锁顺序。

Animal 图片维护不额外限制动物状态。负责 `RESCUER` 可以维护本人负责动物，`ADMIN` 可以维护全部动物。

## 4. 约束与索引

```sql
CONSTRAINT uk_animal_image_animal_sort
UNIQUE (animal_id, sort_order)

CONSTRAINT uk_animal_image_path
UNIQUE (image_path)

CONSTRAINT chk_animal_image_sort_order
CHECK (sort_order >= 1)
```

联合唯一索引已经支持 `WHERE animal_id = ? ORDER BY sort_order`，不再增加重复的 `animal_id` 普通索引。

## 5. 文件生命周期

删除图片采用数据库关联先提交、正式文件后删除。删除失败时记录错误日志并由具有 24 小时安全窗口的孤立文件扫描兜底，规则复用全库文件服务规范。

## 6. 外键策略

`animal_id` 使用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。图片关联不使用逻辑删除。

> **`animal_image` 表 V1.0 — Frozen**
