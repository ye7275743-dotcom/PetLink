# PetLink REST API 设计 V1.0 — M02 救助线索 + M03 救助任务

> 状态：Frozen  
> 前置依赖：REST API 全局规范 V1.0、M01 API V1.0 已 Frozen  
> Base URL：`/api`

---

# 0. M02/M03 通用响应 DTO 与媒体读取

## 0.1 统一响应 DTO

M02/M03 不使用“只有 HTTP 状态、无业务数据”的动作响应。

### `StateActionResponse`

用于审核、撤回、开始救助、取消任务、失败任务处理等状态动作。

```json
{
  "id": "4001",
  "status": "IN_PROGRESS",
  "updatedAt": "2026-08-29T10:20:00+08:00"
}
```

其中 `id` 表示本次状态动作的主资源 ID。

### `ClueSummaryResponse`

```json
{
  "id": "3001",
  "location": "南京市玄武区某路口",
  "foundTime": "2026-08-29T08:20:00+08:00",
  "animalDescription": "一只受伤的橘猫",
  "status": "PENDING_REVIEW",
  "coverImageUrl": "/api/media/rescue-clue-images/3101",
  "createdAt": "2026-08-29T09:45:00+08:00",
  "updatedAt": "2026-08-29T09:45:00+08:00"
}
```

### `ClueDetailResponse`

在 `ClueSummaryResponse` 基础上补充：

```text
sceneDescription
contact
rejectReason
reviewerId（有权限时）
reviewedAt
images[]
```

其中 `images[]` 固定字段为：

```json
[
  {
    "id": "3101",
    "url": "/api/media/rescue-clue-images/3101",
    "sortOrder": 1
  }
]
```

即：

```text
id
url
sortOrder
```

### `TaskSummaryResponse`

```json
{
  "id": "4001",
  "clueId": "3001",
  "status": "IN_PROGRESS",
  "startedAt": "2026-08-29T10:20:00+08:00",
  "finishedAt": null,
  "createdAt": "2026-08-29T10:05:00+08:00",
  "updatedAt": "2026-08-29T10:20:00+08:00"
}
```

### `TaskDetailResponse`

在 `TaskSummaryResponse` 基础上补充：

```text
来源 clue 摘要
failureReason
cancelReason
救助记录摘要
成功后创建的 Animal 摘要
```

### `RescueRecordResponse`

```json
{
  "id": "4101",
  "taskId": "4001",
  "content": "已到达现场，正在联系宠物医院。",
  "createdAt": "2026-08-29T10:30:00+08:00"
}
```

所有 BIGINT ID 均按全局规范序列化为字符串。

---

## 0.2 救助线索图片媒体接口

```http
GET /api/media/rescue-clue-images/{imageId}
```

权限与可见性：

```text
复用所属 RescueClue 的详情可见性规则。

ADMIN
→ 任意线索图片

USER
→ 仅本人发布线索的图片

RESCUER
→ 本人发布线索
OR 所属线索当前为 WAITING_ACCEPT
OR 所属线索存在 rescuer_id = 当前用户的历史/当前 RescueTask
```

不可见或资源不存在：

```text
40401 RESOURCE_NOT_FOUND
```

实现规则：

```text
1. 根据 imageId 查询 rescue_clue_image
2. 取得所属 clue_id
3. 执行 RescueClue 可见性校验
4. 从数据库 image_path 取得正式相对路径
5. 服务端 canonicalize 后确认路径仍位于 uploads 根目录
6. 禁止客户端提交/覆盖物理路径
7. 根据实际文件类型返回正确 Content-Type
8. 文件不存在时记录 ERROR，并对外返回 404
```

必须防止：

```text
../
绝对路径注入
符号链接/规范化后逃逸 uploads 根目录
```

成功：

```text
HTTP 200
响应体为图片二进制
不使用统一 JSON 成功包装
Content-Type 根据实际图片类型返回
```

例如：

```http
HTTP/1.1 200 OK
Content-Type: image/jpeg
```

失败：

```text
仍使用全局 ApiResponse 错误结构
```

例如资源不存在或不可见：

```text
HTTP 404
code = 40401 RESOURCE_NOT_FOUND
```

---

# 1. M02 救助线索 API

## 1.1 模块边界

M02 负责：

```text
发布救助线索
查看本人线索
查看线索详情
修改本人待审核线索
维护本人待审核线索图片
撤回本人待审核线索
ADMIN 查看审核队列
ADMIN 审核线索
```

状态机：

```text
PENDING_REVIEW
├── REJECTED
├── WITHDRAWN
└── WAITING_ACCEPT

WAITING_ACCEPT
└── CONVERTED

CONVERTED
├── WAITING_ACCEPT
└── CLOSED
```

其中 `CONVERTED → WAITING_ACCEPT` 仅发生于任务取消，或 FAILED 后 ADMIN 决定重新开放。

合法迁移按已冻结状态机执行，客户端不得直接提交 `status`。

---

## 1.2 UC-01 短期幂等键

数据库不新增幂等字段。

V1.0 采用：

```text
前端提交按钮禁用
+
服务端短期 Idempotency-Key
```

### 1.2.1 获取发布幂等键

```http
POST /api/rescue-clues/idempotency-keys
```

权限：

```text
USER / RESCUER
```

**申请时机固定为：用户点击“提交线索”时立即申请。**

禁止：

```text
进入表单页面时提前申请
```

推荐前端流程：

```text
用户填写完表单
→ 点击提交
→ 禁用提交按钮
→ POST /idempotency-keys
→ 获得 key
→ 立即 POST /api/rescue-clues
```

成功：

```http
HTTP/1.1 201 Created
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "idempotencyKey": "4a8f3ec5-e848-4d55-8655-179adf3181ba",
    "expiresAt": "2026-08-29T10:00:00+08:00"
  }
}
```

V1.0：

```text
默认 TTL：PT10M
配置项：petlink.api.clue-submit-idempotency-ttl=PT10M
```

服务端以：

```text
(userId, idempotencyKey)
```

为作用域保存：

```text
UNUSED
PROCESSING
SUCCEEDED
```

V1.0 单实例允许使用**进程内线程安全缓存**。

边界：

```text
只提供短期、尽力而为的重复提交保护。
服务重启后 UNUSED / PROCESSING / SUCCEEDED 缓存都会丢失。
V1.0 不承诺跨 JVM 重启幂等。
未来迁移 Redis 等共享存储时不改变 API。
```

### 1.2.2 规范化请求指纹

发布请求进入业务前：

```text
1. 执行 DTO 字段校验
2. 对需要 trim 的字段执行 trim
3. sceneDescription 等按正式 null 规则规范化
4. 保持 imageTokens 的客户端原顺序
5. 组合：
   当前 userId
   + 规范化后的 DTO
   + 保持原顺序的 imageTokens
6. 使用固定 canonical serialization
7. 计算 SHA-256 请求指纹
```

禁止：

```text
直接对原始 JSON 字符串做哈希
```

因为 JSON 字段顺序、空白、等价输入形式不应造成不同指纹。

### 1.2.3 缓存状态与数据库事务顺序

幂等缓存**不属于数据库事务**，不能假设数据库 ROLLBACK 会自动回滚缓存。

正式顺序：

```text
规范化请求并计算 SHA-256 指纹
↓
缓存使用原子 compute：
UNUSED → PROCESSING
↓
BEGIN 数据库事务
↓
执行业务写入 + 文件正式副本 copy
↓
COMMIT
↓
TransactionSynchronization.afterCommit
↓
PROCESSING → SUCCEEDED
并缓存首次 clueId / 首次成功响应
```

事务失败：

```text
ROLLBACK
↓
PROCESSING → UNUSED
```

必须使用：

```text
ConcurrentHashMap.compute(...)
```

或语义等价的原子操作。

禁止：

```text
get()
→ 判断
→ put()
```

否则并发请求可能同时占用同一 key。

### 1.2.4 重复请求处理

```text
key 不存在 / 已过期 / 不属于当前用户
→ 40005 INVALID_IDEMPOTENCY_KEY

UNUSED
→ 原子切换 PROCESSING
→ 进入业务

PROCESSING
→ 40905 IDEMPOTENCY_REQUEST_IN_PROGRESS

SUCCEEDED + 指纹一致
→ 返回首次成功响应
→ 不创建第二条线索

SUCCEEDED + 指纹不同
→ 40906 IDEMPOTENCY_KEY_REUSED
```

`SUCCEEDED`：

```text
保留到该 key 原始 expiresAt
```

`PROCESSING`：

```text
请求仍在执行期间不得被普通 TTL 清理任务移除
```

请求正常结束但数据库回滚：

```text
恢复 UNUSED
```

服务进程崩溃：

```text
缓存整体丢失
```

属于 V1.0 已接受边界。

---

## 1.3 发布救助线索

```http
POST /api/rescue-clues
Idempotency-Key: <server-issued-uuid>
```

权限：

```text
USER / RESCUER
```

请求：

```json
{
  "location": "南京市玄武区某路口",
  "foundTime": "2026-08-29T08:20:00+08:00",
  "animalDescription": "一只受伤的橘猫，后腿疑似受伤",
  "sceneDescription": "目前躲在绿化带附近",
  "contact": "13800138000",
  "imageTokens": [
    "550e8400-e29b-41d4-a716-446655440000"
  ]
}
```

字段规则：

| 字段 | 规则 |
|---|---|
| `location` | trim 后 1～255 |
| `foundTime` | 必填，ISO 8601 带时区；不得晚于服务端当前时间 |
| `animalDescription` | trim 后 1～1000 |
| `sceneDescription` | 可空；非空时 trim 后 1～1000 |
| `contact` | trim 后 1～100 |
| `imageTokens` | 必填，1～9 个，不允许重复 |

身份：

```text
publisher_id = 当前 JWT userId
```

客户端不得提交：

```text
publisherId
status
reviewerId
reviewedAt
rejectReason
```

新线索固定：

```text
status = PENDING_REVIEW
```

### 1.3.1 文件、缓存与数据库事务流程

缓存占用发生在数据库事务开始前，见 1.2。

数据库事务：

```text
1. BEGIN
2. 校验 imageTokens 数量 1～9、拒绝重复 token
3. 查询 temporary_file
4. 按 temporary_file.id ASC 加锁
5. 校验：
   - owner_id = 当前用户
   - status = UPLOADED
   - expires_at > NOW()
   - JPG/JPEG/PNG
   - 单张 ≤ 5MB
6. INSERT rescue_clue，状态 PENDING_REVIEW
7. 按客户端 imageTokens 原顺序生成 rescue_clue_image.sort_order
8. 服务端生成 formal_path
9. 每个 temporary_file 条件绑定：
   status = BOUND
   business_type = RESCUE_CLUE
   business_id = 新建 RescueClue.id
   formal_path = 对应正式相对路径
   bound_at = NOW()
   且 affectedRows 必须 = 1
10. 写 OperationLog：
    RESCUE_CLUE + CREATE
    before_status = NULL
    after_status = PENDING_REVIEW
11. copy 临时文件 → 正式文件
12. COMMIT
13. afterCommit：
    幂等缓存 PROCESSING → SUCCEEDED
    缓存首次成功响应
14. COMMIT 后删除原临时文件和可安全删除的 temporary_file 技术记录
```

失败：

```text
数据库事务 / formal copy 任一步失败
→ ROLLBACK
→ 仅删除本事务已经创建的正式副本
→ 原临时文件保留
→ 请求结束路径中将幂等缓存 PROCESSING → UNUSED
```

数据库提交成功但临时文件删除失败：

```text
不回滚业务
BOUND 记录保留
交由清理任务恢复
```

成功：

```http
HTTP/1.1 201 Created
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "clueId": "3001",
    "status": "PENDING_REVIEW",
    "createdAt": "2026-08-29T09:45:00+08:00"
  }
}
```

---

## 1.4 查看本人线索

```http
GET /api/rescue-clues/me?page=1&size=20&status=PENDING_REVIEW
```

权限：

```text
USER / RESCUER
```

`status` 可选，采用白名单：

```text
PENDING_REVIEW
REJECTED
WITHDRAWN
WAITING_ACCEPT
CONVERTED
CLOSED
```

排序：

```text
created_at DESC, id DESC
```

仅查询：

```text
publisher_id = 当前 userId
```

---

## 1.5 查看线索详情

```http
GET /api/rescue-clues/{clueId}
```

权限与可见性：

```text
ADMIN
→ 任意线索

USER
→ 仅本人发布线索

RESCUER
→ 本人发布线索
OR 当前状态 = WAITING_ACCEPT
OR 存在 rescuer_id = 当前用户的历史/当前 RescueTask
```

不满足可见性：

```text
40401 RESOURCE_NOT_FOUND
```

避免通过 403 泄露资源存在性。

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "3001",
    "location": "南京市玄武区某路口",
    "foundTime": "2026-08-29T08:20:00+08:00",
    "animalDescription": "一只受伤的橘猫",
    "sceneDescription": "躲在绿化带",
    "contact": "13800138000",
    "status": "PENDING_REVIEW",
    "rejectReason": null,
    "images": [
      {
        "id": "3101",
        "url": "/api/media/rescue-clue-images/3101",
        "sortOrder": 1
      }
    ],
    "createdAt": "2026-08-29T09:45:00+08:00",
    "updatedAt": "2026-08-29T09:45:00+08:00"
  }
}
```

API 不返回数据库 `image_path`。

---

## 1.6 修改本人待审核线索

```http
PATCH /api/rescue-clues/{clueId}
```

权限：

```text
USER / RESCUER
```

业务条件：

```text
publisher_id = 当前用户
AND status = PENDING_REVIEW
```

允许字段：

```text
location
foundTime
animalDescription
sceneDescription
contact
```

禁止直接通过本接口修改：

```text
images
status
publisherId
reviewerId
reviewedAt
rejectReason
```

字段校验复用发布接口规则，其中：

```text
foundTime 不得晚于服务端当前时间
```

PATCH 语义：

```text
字段未出现 → 保持原值

sceneDescription = null / ""
→ 保存 NULL

其余必填业务字段 = null / 空白
→ 40001 INVALID_PARAMETER

空请求体
→ 40001 INVALID_PARAMETER

出现禁止字段
→ 40001 INVALID_PARAMETER
```

并发策略：

```text
SELECT rescue_clue ... FOR UPDATE
→ 校验 publisher_id
→ 校验 status = PENDING_REVIEW
→ 仅更新实际发生变化的字段
```

采用行锁而不是仅依赖 `affectedRows`，避免“提交相同值导致 MySQL affectedRows=0”被误判为状态冲突。

成功固定返回：

```text
ApiResponse<ClueDetailResponse>
```

前端可直接以返回的完整详情覆盖当前页面数据。

---

## 1.7 给待审核线索追加图片

```http
POST /api/rescue-clues/{clueId}/images
```

权限：

```text
USER / RESCUER
```

请求：

```json
{
  "imageTokens": [
    "550e8400-e29b-41d4-a716-446655440001",
    "550e8400-e29b-41d4-a716-446655440002"
  ]
}
```

规则：

```text
publisher_id = 当前用户
status = PENDING_REVIEW
imageTokens 本次数量 1～9
本次 token 不重复
现有图片数 + 新增图片数 <= 9
```

锁顺序：

```text
1. rescue_clue FOR UPDATE
2. temporary_file 按 id ASC FOR UPDATE
```

图片展示顺序：

```text
从当前 max(sort_order)+1 开始追加
按客户端 imageTokens 原顺序确定 sort_order
```

不支持任意拖拽重排。

文件 copy / commit / cleanup 继续使用全局 Frozen 文件事务规则。

成功：

```http
201 Created
```

固定返回：

```text
ApiResponse<ClueDetailResponse>
```

前端可直接以返回的完整详情覆盖当前页面数据。

---

## 1.8 删除待审核线索图片

```http
DELETE /api/rescue-clues/{clueId}/images/{imageId}
```

权限：

```text
USER / RESCUER
```

规则：

```text
锁 rescue_clue FOR UPDATE
校验 publisher_id = 当前用户
校验 status = PENDING_REVIEW
校验 imageId 确属当前 clue
删除后必须至少保留 1 张图片
```

删除第 k 张后：

```text
1. 先删除目标 image 行
2. 按原 sort_order ASC 查询后续图片
3. 按 ASC 顺序逐行执行 sort_order = sort_order - 1
4. 保持 1..N 连续
```

不能依赖无序批量 UPDATE，因为存在：

```text
UNIQUE(clue_id, sort_order)
```

按原 `sort_order ASC` 依次前移可以避免中间唯一键冲突。

事务：

```text
数据库删除 / 重编号 COMMIT
→ 再删除正式物理图片
```

正式文件删除失败：

```text
数据库不回滚
记录 ERROR
由孤儿文件扫描任务后续清理
```

成功固定返回：

```text
ApiResponse<ClueDetailResponse>
```

前端可直接以返回的完整详情覆盖当前页面数据。

---

## 1.9 撤回本人待审核线索

```http
POST /api/rescue-clues/{clueId}/withdraw
```

权限：

```text
USER / RESCUER
```

条件更新：

```sql
UPDATE rescue_clue
SET status = 'WITHDRAWN'
WHERE id = ?
  AND publisher_id = ?
  AND status = 'PENDING_REVIEW';
```

成功时同事务写：

```text
OperationLog
business_type = RESCUE_CLUE
operation_type = WITHDRAW
before_status = PENDING_REVIEW
after_status = WITHDRAWN
```

`affectedRows = 0` 后重新查询：

```text
不存在 / 非本人 → 40401
存在但状态不再是 PENDING_REVIEW → 40901
```

成功返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "3001",
    "status": "WITHDRAWN",
    "updatedAt": "2026-08-29T10:18:00+08:00"
  }
}
```

---

## 1.10 ADMIN 查看救助线索审核列表

```http
GET /api/admin/rescue-clues?page=1&size=20&status=PENDING_REVIEW
```

权限：

```text
ADMIN
```

默认：

```text
status = PENDING_REVIEW
```

审核队列排序：

```text
created_at ASC, id ASC
```

便于先提交先审核。

允许 ADMIN 按状态查询其他线索用于管理，但 M08 后续只补充更高层的业务监管接口，不重复审核动作。

---

## 1.11 ADMIN 审核线索

```http
POST /api/admin/rescue-clues/{clueId}/audit
```

权限：

```text
ADMIN
```

批准：

```json
{
  "decision": "APPROVE"
}
```

驳回：

```json
{
  "decision": "REJECT",
  "rejectReason": "图片和地点信息不足，请补充后重新提交"
}
```

规则：

```text
decision = APPROVE
→ rejectReason 必须不存在或为 null
→ PENDING_REVIEW → WAITING_ACCEPT
→ operation_type = AUDIT_APPROVE

decision = REJECT
→ rejectReason trim 后 1～500
→ PENDING_REVIEW → REJECTED
→ operation_type = AUDIT_REJECT
```

同一次条件 UPDATE 必须同步写入：

```text
status
reviewer_id = 当前 ADMIN
reviewed_at = NOW()
reject_reason
```

并同事务 INSERT `operation_log`。

并发失败：

```text
affectedRows = 0
→ 重新查询
   ├─ 不存在 → 40401
   └─ 已被其他 ADMIN 审核 / 状态变化 → 40901
```

成功返回 `StateActionResponse`，例如批准：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "3001",
    "status": "WAITING_ACCEPT",
    "updatedAt": "2026-08-29T10:20:00+08:00"
  }
}
```

---

# 2. M03 救助任务 API

## 2.1 模块边界

M03 负责：

```text
RESCUER 查看 WAITING_ACCEPT 线索
RESCUER 接取任务
RESCUER 查看本人任务 / 任务详情
RESCUER 开始救助
RESCUER 添加救助过程记录
RESCUER 提交救助结果（成功 / 失败）
ADMIN 取消异常任务
ADMIN 对 FAILED 任务重新开放线索 / 关闭线索
```

---

## 2.2 RESCUER 查看待接取线索

```http
GET /api/rescue-clues/waiting-acceptance?page=1&size=20
```

权限：

```text
RESCUER
```

固定查询：

```text
status = WAITING_ACCEPT
```

排序：

```text
created_at ASC, id ASC
```

不允许客户端指定其他状态。

---

## 2.3 RESCUER 接取救助任务

```http
POST /api/rescue-clues/{clueId}/accept
```

权限：

```text
RESCUER
```

客户端不得提交：

```text
rescuerId
taskStatus
```

事务：

```text
1. 条件 UPDATE RescueClue：
   WAITING_ACCEPT → CONVERTED

2. affectedRows = 0：
   重新查询
   ├─ 不存在 / 不可见 → 40401
   └─ 已被他人接取 / 状态变化 → 40901

3. INSERT RescueTask：
   clue_id = clueId
   rescuer_id = 当前 RESCUER
   status = WAITING_START

4. INSERT OperationLog：
   RESCUE_CLUE + ACCEPT_RESCUE
   WAITING_ACCEPT → CONVERTED

5. COMMIT
```

`uk_rescue_task_active_clue` 作为数据库并发兜底。

成功：

```http
201 Created
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": "4001",
    "clueId": "3001",
    "status": "WAITING_START",
    "createdAt": "2026-08-29T10:05:00+08:00"
  }
}
```

---

## 2.4 查看本人救助任务

```http
GET /api/rescue-tasks/me?page=1&size=20&status=IN_PROGRESS
```

权限：

```text
RESCUER
```

可选状态：

```text
WAITING_START
IN_PROGRESS
SUCCESS
FAILED
CANCELED
```

固定：

```text
rescuer_id = 当前用户
```

排序：

```text
created_at DESC, id DESC
```

---

## 2.5 查看救助任务详情

```http
GET /api/rescue-tasks/{taskId}
```

权限：

```text
RESCUER：只能查看本人任务
ADMIN：任意任务
```

返回包含：

```text
任务基本信息
来源 clue 摘要
救助过程记录摘要
若已成功：由本任务创建的 Animal 摘要
```

`clueId` 来源于数据库 Task，不接受客户端覆盖。

---

## 2.6 开始救助

```http
POST /api/rescue-tasks/{taskId}/start
```

权限：

```text
RESCUER
```

条件：

```text
rescuer_id = 当前用户
WAITING_START → IN_PROGRESS
```

同一 UPDATE：

```text
status = IN_PROGRESS
started_at = NOW()
```

同事务日志：

```text
RESCUE_TASK + START_RESCUE
WAITING_START → IN_PROGRESS
```

`affectedRows = 0` 后区分 404 / 409。

成功返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "4001",
    "status": "IN_PROGRESS",
    "updatedAt": "2026-08-29T10:20:00+08:00"
  }
}
```

---

## 2.7 添加救助过程记录

```http
POST /api/rescue-tasks/{taskId}/records
```

权限：

```text
RESCUER
```

请求：

```json
{
  "content": "已到达现场，动物有明显后腿外伤，正在联系宠物医院。"
}
```

规则：

```text
content trim 后 1～2000
recorder_id = 当前用户
```

事务：

```text
1. SELECT RescueTask FOR UPDATE
2. 校验 task 存在
3. 校验 rescuer_id = 当前用户
4. 校验 status = IN_PROGRESS
5. INSERT RescueRecord
6. COMMIT
```

`RescueRecord` append-only，不提供修改和删除接口。

成功：

```http
201 Created
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "4101",
    "taskId": "4001",
    "content": "已到达现场，动物有明显后腿外伤，正在联系宠物医院。",
    "createdAt": "2026-08-29T10:30:00+08:00"
  }
}
```

---

## 2.8 查看救助过程记录

```http
GET /api/rescue-tasks/{taskId}/records
```

权限：

```text
RESCUER：仅本人任务
ADMIN：任意任务
```

V1.0 固定返回：

```text
ApiResponse<List<RescueRecordResponse>>
```

排序：

```text
created_at ASC, id ASC
```

V1.0 不分页；若后续单任务过程记录量显著增大，再扩展分页接口。

---

## 2.9 提交救助失败结果

统一入口：

```http
POST /api/rescue-tasks/{taskId}/result
```

失败请求：

```json
{
  "result": "FAILED",
  "failureReason": "动物受到惊吓逃离现场，多次搜寻未找到"
}
```

权限：

```text
RESCUER
```

条件：

```text
result = FAILED
rescuer_id = 当前用户
status = IN_PROGRESS
failureReason trim 后 1～500
请求中禁止出现 animals
```

同一条件 UPDATE：

```text
status = FAILED
failure_reason = ...
finished_at = NOW()
```

线索：

```text
RescueClue 保持 CONVERTED
```

同事务日志：

```text
RESCUE_TASK + RESCUE_FAILED
IN_PROGRESS → FAILED
reason = failureReason
```

`affectedRows = 0` 后区分 404 / 409。

成功返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "4001",
    "status": "FAILED",
    "updatedAt": "2026-08-29T11:00:00+08:00"
  }
}
```

---

## 2.10 提交救助成功结果并建档

同一入口：

```http
POST /api/rescue-tasks/{taskId}/result
```

成功请求示例：

```json
{
  "result": "SUCCESS",
  "animals": [
    {
      "name": "橘子",
      "species": "CAT",
      "sex": "UNKNOWN",
      "estimatedAgeMonths": 12,
      "color": "橘色",
      "healthCondition": "后腿外伤，已完成初步处理",
      "initialHealthRecord": "已清创并完成包扎，建议三天后复查",
      "imageTokens": [
        "550e8400-e29b-41d4-a716-446655440010"
      ]
    }
  ]
}
```

规则：

```text
result = SUCCESS
failureReason 禁止出现
animals 数量 >= 1
所有 Animal 的 imageTokens 总数 0～9
所有 token 在整次请求内不得重复

每个 Animal 的 imageTokens 独立排序：
→ 按该 Animal.imageTokens 的客户端原顺序创建 AnimalImage
→ sort_order = 1..N
→ 不同 Animal 的 sort_order 分别从 1 开始
```

每只 Animal：

| 字段 | 规则 |
|---|---|
| `name` | trim 后 1～100 |
| `species` | trim 后 1～50 |
| `sex` | `MALE / FEMALE / UNKNOWN` |
| `estimatedAgeMonths` | 可空；0～65535 |
| `color` | 可空；非空时 trim 后 1～100 |
| `healthCondition` | trim 后 1～1000 |
| `initialHealthRecord` | 可空；非空时 trim 后 1～2000 |
| `imageTokens` | 可空数组 |

客户端不得提交：

```text
rescueTaskId
status
suspendReason
version
createdAt
```

新 Animal 固定：

```text
rescue_task_id = 当前锁定 Task.id
status = TREATING
version = 0
suspend_reason = NULL
```

若某个 Animal 提供 `initialHealthRecord`：

```text
创建一条 HealthRecord
animal_id = 新建 Animal.id
recorder_id = 当前 RESCUER
content = trim 后 initialHealthRecord
created_at = NOW()
```

任一初始 HealthRecord 创建失败：

```text
整个成功救助事务回滚
```

### 2.10.1 冻结锁顺序

成功事务必须：

```text
1. SELECT RescueTask FOR UPDATE
2. 校验 rescuer_id = 当前用户
3. 校验 status = IN_PROGRESS
4. clueId 只能从已锁定 Task.clue_id 取得
5. SELECT RescueClue FOR UPDATE
6. 校验 clue.status = CONVERTED
7. imageTokens 去重
8. 查询 TemporaryFile，并按 temporary_file.id ASC FOR UPDATE
```

禁止：

```text
客户端提交 clueId
客户端覆盖 Task.clue_id
先锁 Clue 再锁 Task
```

### 2.10.2 成功事务内容

```text
1. 创建 >=1 Animal，初始 TREATING
2. 对提供 initialHealthRecord 的 Animal 创建 HealthRecord
3. 创建 AnimalImage（如有）：
   - 每个 Animal 独立按其 imageTokens 客户端原顺序创建
   - sort_order = 1..N
   - 不同 Animal 分别从 1 开始
4. 每个图片 token 对应 TemporaryFile 条件绑定：
   status = BOUND
   business_type = ANIMAL
   business_id = 对应新建 Animal.id
   formal_path = 对应正式相对路径
   bound_at = NOW()
   且 affectedRows 必须 = 1
5. RescueTask：
   IN_PROGRESS → SUCCESS
   finished_at = NOW()
6. RescueClue：
   CONVERTED → CLOSED
7. OperationLog：
   RESCUE_TASK + COMPLETE_RESCUE
   IN_PROGRESS → SUCCESS
8. OperationLog：
   RESCUE_CLUE + CLOSE_AFTER_RESCUE
   CONVERTED → CLOSED
9. copy 所有本事务临时图片 → 正式路径
10. COMMIT
11. COMMIT 后清理临时原文件
```

任意数据库写入、日志写入或正式文件 copy 失败：

```text
整个数据库事务 ROLLBACK
所有 Animal / AnimalImage / HealthRecord / 状态变更 / OperationLog 均不保留
仅删除本事务新建的正式副本
原临时文件保留
```

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": "4001",
    "status": "SUCCESS",
    "animalIds": [
      "5001"
    ],
    "finishedAt": "2026-08-29T11:20:00+08:00"
  }
}
```

---

## 2.11 ADMIN 取消异常救助任务

```http
POST /api/admin/rescue-tasks/{taskId}/cancel
```

权限：

```text
ADMIN
```

请求：

```json
{
  "cancelReason": "救助人员临时无法继续执行任务"
}
```

规则：

```text
cancelReason trim 后 1～500
Task 当前状态必须为 WAITING_START 或 IN_PROGRESS
```

锁顺序：

```text
1. RescueTask FOR UPDATE
2. clueId 从已锁定 Task 获取
3. RescueClue FOR UPDATE
```

校验：

```text
Task.status ∈ {WAITING_START, IN_PROGRESS}
Clue.status = CONVERTED
```

同事务：

```text
RescueTask → CANCELED
cancel_reason = request.cancelReason
finished_at = NOW()

RescueClue
CONVERTED → WAITING_ACCEPT

OperationLog：
RESCUE_TASK + CANCEL_RESCUE
原 Task 状态 → CANCELED
reason = cancelReason

OperationLog：
RESCUE_CLUE + REOPEN
CONVERTED → WAITING_ACCEPT
reason = cancelReason
```

任一步失败整体回滚。

成功返回 Task 的 `StateActionResponse`：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "4001",
    "status": "CANCELED",
    "updatedAt": "2026-08-29T11:30:00+08:00"
  }
}
```

---

## 2.12 ADMIN 处理 FAILED 任务

```http
POST /api/admin/rescue-tasks/{taskId}/failure-resolution
```

权限：

```text
ADMIN
```

重新开放：

```json
{
  "action": "REOPEN",
  "resolutionReason": "附近仍有目击信息，决定重新开放救助"
}
```

关闭线索：

```json
{
  "action": "CLOSE",
  "resolutionReason": "多次搜寻仍无结果，当前线索暂时关闭"
}
```

规则：

```text
action ∈ {REOPEN, CLOSE}
resolutionReason 必填
trim 后 1～500
```

锁顺序保持：

```text
1. RescueTask FOR UPDATE
2. clueId 只能从已锁定 Task 获取
3. RescueClue FOR UPDATE
```

基础前置：

```text
目标 Task.status = FAILED
Clue.status = CONVERTED
```

### 2.12.1 防止旧 FAILED Task 误操作新任务

仅有：

```text
Task = FAILED
Clue = CONVERTED
```

不足以执行决策。

在锁定目标 Task 与 Clue 后，必须以**当前读**校验：

```text
A. 目标 FAILED Task 必须是该 Clue 最新创建的 RescueTask
AND
B. 当前不存在其他活动 RescueTask
   status ∈ {WAITING_START, IN_PROGRESS}
```

“最新创建”排序固定：

```text
created_at DESC, id DESC
```

不满足任一条件：

```text
40901 BUSINESS_STATE_CONFLICT
```

典型被阻止场景：

```text
Task #1 FAILED
→ ADMIN REOPEN
→ RESCUER 接取并创建 Task #2
→ Clue 再次 CONVERTED
→ 再调用 Task #1 failure-resolution
→ 40901
```

这样旧失败任务不能重新开放或关闭正在由新任务处理的 Clue。

### 2.12.2 REOPEN

同事务：

```text
RescueClue：
CONVERTED → WAITING_ACCEPT

OperationLog：
business_type = RESCUE_CLUE
operation_type = REOPEN
before_status = CONVERTED
after_status = WAITING_ACCEPT
reason = resolutionReason
```

目标 FAILED Task 保持 FAILED。

未来再次接取：

```text
创建新的 RescueTask
```

### 2.12.3 CLOSE

同事务：

```text
RescueClue：
CONVERTED → CLOSED

OperationLog：
business_type = RESCUE_CLUE
operation_type = CLOSE_AFTER_FAILURE
before_status = CONVERTED
after_status = CLOSED
reason = resolutionReason
```

目标 FAILED Task 保持 FAILED。

`Task.failure_reason` 已经保存在：

```text
rescue_task.failure_reason
RESCUE_TASK + RESCUE_FAILED 日志
```

ADMIN 决策日志不再复制 failureReason，而记录本次人工决策的 `resolutionReason`。

成功返回 Clue 的 `StateActionResponse`。

REOPEN：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "3001",
    "status": "WAITING_ACCEPT",
    "updatedAt": "2026-08-29T11:40:00+08:00"
  }
}
```

CLOSE 同理返回：

```text
status = CLOSED
```

---

# 3. M02 接口汇总

| 编号 | Method | Path | 功能 | 权限 | 成功 HTTP |
|---|---|---|---|---|---:|
| M02-API-01 | POST | `/api/rescue-clues/idempotency-keys` | 获取发布短期幂等键 | USER/RESCUER | 201 |
| M02-API-02 | POST | `/api/rescue-clues` | 发布救助线索 | USER/RESCUER | 201 |
| M02-API-03 | GET | `/api/rescue-clues/me` | 查看本人线索 | USER/RESCUER | 200 |
| M02-API-04 | GET | `/api/rescue-clues/{id}` | 查看线索详情 | 按可见性规则 | 200 |
| M02-API-05 | PATCH | `/api/rescue-clues/{id}` | 修改本人待审核线索 | USER/RESCUER | 200 |
| M02-API-06 | POST | `/api/rescue-clues/{id}/images` | 追加线索图片 | USER/RESCUER | 201 |
| M02-API-07 | DELETE | `/api/rescue-clues/{id}/images/{imageId}` | 删除线索图片 | USER/RESCUER | 200 |
| M02-API-08 | POST | `/api/rescue-clues/{id}/withdraw` | 撤回待审核线索 | USER/RESCUER | 200 |
| M02-API-09 | GET | `/api/admin/rescue-clues` | ADMIN 查看审核队列/线索 | ADMIN | 200 |
| M02-API-10 | POST | `/api/admin/rescue-clues/{id}/audit` | ADMIN 审核线索 | ADMIN | 200 |
| COMMON-MEDIA-01 | GET | `/api/media/rescue-clue-images/{imageId}` | 读取线索图片 | 复用 Clue 可见性 | 200 |

---

# 4. M03 接口汇总

| 编号 | Method | Path | 功能 | 权限 | 成功 HTTP |
|---|---|---|---|---|---:|
| M03-API-01 | GET | `/api/rescue-clues/waiting-acceptance` | 查看待接取线索 | RESCUER | 200 |
| M03-API-02 | POST | `/api/rescue-clues/{id}/accept` | 接取救助任务 | RESCUER | 201 |
| M03-API-03 | GET | `/api/rescue-tasks/me` | 查看本人救助任务 | RESCUER | 200 |
| M03-API-04 | GET | `/api/rescue-tasks/{id}` | 查看救助任务详情 | RESCUER本人/ADMIN | 200 |
| M03-API-05 | POST | `/api/rescue-tasks/{id}/start` | 开始救助 | RESCUER本人 | 200 |
| M03-API-06 | POST | `/api/rescue-tasks/{id}/records` | 添加救助过程记录 | RESCUER本人 | 201 |
| M03-API-07 | GET | `/api/rescue-tasks/{id}/records` | 查看救助过程记录 | RESCUER本人/ADMIN | 200 |
| M03-API-08 | POST | `/api/rescue-tasks/{id}/result` | 提交成功/失败救助结果 | RESCUER本人 | 200 |
| M03-API-09 | POST | `/api/admin/rescue-tasks/{id}/cancel` | ADMIN 取消异常任务 | ADMIN | 200 |
| M03-API-10 | POST | `/api/admin/rescue-tasks/{id}/failure-resolution` | ADMIN 处理 FAILED 任务 | ADMIN | 200 |

---

# 5. M02/M03 新增业务错误码

在全局错误码基础上增加：

```text
40005 INVALID_IDEMPOTENCY_KEY
40905 IDEMPOTENCY_REQUEST_IN_PROGRESS
40906 IDEMPOTENCY_KEY_REUSED
```

其余状态冲突统一：

```text
40901 BUSINESS_STATE_CONFLICT
```

文件错误继续：

```text
40004 INVALID_FILE
41301 FILE_TOO_LARGE
```

---

# 6. M02/M03 API V1.0 最终冻结结论

本轮没有修改 Frozen 数据库结构。

最终固定：

```text
1. RescueClue 状态机按分支迁移表达，不采用错误的线性列表。

2. 以下三个线索修改类接口成功响应统一为：
   ApiResponse<ClueDetailResponse>

   PATCH  /api/rescue-clues/{id}
   POST   /api/rescue-clues/{id}/images
   DELETE /api/rescue-clues/{id}/images/{imageId}

3. ClueDetailResponse.images[] 固定字段：
   id
   url
   sortOrder

4. SUCCESS 建档：
   每个 Animal 的 imageTokens 独立按客户端原顺序排序，
   AnimalImage.sort_order = 1..N，
   不同 Animal 分别从 1 开始，
   所有 Animal 图片合计仍最多 9 张。

5. GET /api/media/rescue-clue-images/{imageId}
   成功直接返回图片二进制，不套 ApiResponse；
   失败继续使用全局 ApiResponse 错误结构。

6. GET /api/rescue-tasks/{taskId}/records
   V1.0 返回 ApiResponse<List<RescueRecordResponse>>，
   created_at ASC, id ASC。
```

此前已确认的规则继续有效：

```text
UC-01 短期幂等 TTL = PT10M
点击提交时申请幂等键
规范化 DTO + 当前用户 + imageTokens 原顺序计算 SHA-256
原子 compute：UNUSED → PROCESSING
数据库事务与缓存状态分离
afterCommit → SUCCEEDED
JVM 重启不承诺跨重启幂等

FAILED failure-resolution：
目标 FAILED Task 必须是该 Clue 最新 RescueTask
且不存在其他 WAITING_START / IN_PROGRESS Task
resolutionReason 必填
OperationLog.reason = resolutionReason

SUCCESS：
支持可选 initialHealthRecord
HealthRecord 创建失败则整个成功事务回滚

锁顺序：
Task → Clue
TemporaryFile 多 token 按 id ASC
```

> **✅ M02 救助线索 API V1.0 — Frozen**  
> **✅ M03 救助任务 API V1.0 — Frozen**
