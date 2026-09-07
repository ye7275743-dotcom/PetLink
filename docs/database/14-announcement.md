# 14 `announcement` 系统公告表

## 1. 表用途

`announcement` 保存 ADMIN 创建并维护的系统公告正文、生命周期状态、首次发布时间、最后操作管理员和乐观锁版本。访客及普通登录用户只能访问已发布公告。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 公告 ID |
| `title` | `VARCHAR(200)` | 否 | — | CHECK | 公告标题，不得为空白 |
| `content` | `TEXT` | 否 | — | CHECK | 公告正文，不得为空白 |
| `status` | `VARCHAR(20)`, ASCII BIN | 否 | `DRAFT` | CHECK | `DRAFT / PUBLISHED / WITHDRAWN` |
| `created_by` | `BIGINT UNSIGNED` | 否 | — | FK | 创建公告的 ADMIN |
| `updated_by` | `BIGINT UNSIGNED` | 否 | — | FK | 最后修改、发布或撤回的 ADMIN |
| `published_at` | `DATETIME` | 是 | `NULL` | CHECK | 首次正式发布时间 |
| `version` | `INT UNSIGNED` | 否 | `0` | 乐观锁 | 每次内容或状态更新递增 |
| `deleted` | `TINYINT` | 否 | `0` | INDEX | 逻辑删除标记：`0` 正常，`1` 已删除 |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 自动更新 | 最后更新时间 |

## 3. 管理员字段

创建公告时：

```text
created_by = 当前 ADMIN.id
updated_by = 当前 ADMIN.id
```

后续修改、发布或撤回均更新 `updated_by`。两个字段只能从认证上下文取得，客户端不得指定或覆盖。外键只能保证用户存在，ADMIN 角色仍由 Service 校验。

不增加 `publisher_id / withdrawer_id`；完整生命周期操作人通过 `OperationLog` 保留。

## 4. 状态机与修改权限

V1.0 只允许：

```text
DRAFT → PUBLISHED → WITHDRAWN
```

不支持 `WITHDRAWN → PUBLISHED`。正文修改权限：

```text
DRAFT       可修改
PUBLISHED   可修改
WITHDRAWN   只读
```

新增公告永远创建为 `DRAFT`，客户端不得通过创建接口指定 `PUBLISHED`。

## 5. 发布时间

```text
DRAFT
→ published_at = NULL

PUBLISHED / WITHDRAWN
→ published_at 非空
→ published_at >= created_at
```

`published_at` 保存首次正式发布时间，撤回时不清空。不增加 `withdrawn_at`；撤回时间由终态记录的 `updated_at` 与 `OperationLog.created_at` 追踪。

ADMIN 可在携带当前 `version` 后逻辑删除公告。删除后后台列表、公开列表与详情均不再可见，操作记入 `DELETE_ANNOUNCEMENT` 审计日志。

## 6. 乐观锁与状态更新

所有写操作必须读取并携带当前 `version`：

```sql
version = version + 1
WHERE id = ? AND version = ?
```

普通修改还限制 `status IN ('DRAFT','PUBLISHED')`；发布限制原状态 `DRAFT`；撤回限制原状态 `PUBLISHED`。`affectedRows = 0` 时重新查询最新记录并提示已被其他管理员修改或状态已经变化，不得覆盖最新数据。

乐观锁使并发正文修改、正文修改与发布、发布与撤回之间只能有一个基于同一旧版本的操作成功。

## 7. 生命周期审计事务

以下操作与对应日志必须处于同一事务：

```text
创建 DRAFT + OperationLog(CREATE)
DRAFT → PUBLISHED + OperationLog(PUBLISH)
PUBLISHED → WITHDRAWN + OperationLog(WITHDRAW)
```

日志失败时整体回滚。普通标题或正文修改不强制写 `OperationLog`，由 `updated_by / updated_at / version` 保存最后维护信息。

## 8. 公开访问边界

公开列表与详情均必须带 `PUBLISHED` 条件：

```sql
SELECT ...
FROM announcement
WHERE status = 'PUBLISHED'
ORDER BY published_at DESC, id DESC;
```

```sql
SELECT ...
FROM announcement
WHERE id = ?
  AND status = 'PUBLISHED';
```

对于不存在、`DRAFT` 或 `WITHDRAWN`，普通访问端统一返回“公告不存在或不可访问”，不得泄露后台状态。ADMIN 后台查询不受公开状态限制。

## 9. 查询索引

```sql
KEY idx_announcement_status_published
    (status, published_at DESC, id DESC)

KEY idx_announcement_created
    (created_at DESC, id DESC)
```

分别支持公开公告和 ADMIN 全部公告分页。V1.0 暂不增加 `(status, created_at DESC, id DESC)`；REST API 阶段确认后台存在高频按状态分页后再决定。

## 10. 外键与删除策略

`created_by / updated_by` 均使用 `ON UPDATE RESTRICT / ON DELETE RESTRICT`。公告不物理删除；`WITHDRAWN` 表示业务撤回，`deleted=1` 表示从产品界面删除，两者语义分离。

> **`announcement` 表 V1.0 — Frozen**
