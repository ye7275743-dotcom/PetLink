# PetLink REST API 设计 V1.0 — M06 回访管理 + M07 收藏与公告 + M08 后台管理

> 状态：Frozen  
> 前置依赖：全局 API、M01～M05 已 Frozen  
> Base URL：`/api`

---

# 0. 通用 DTO

## 0.1 `FollowUpImageResponse`

```json
{
  "id": "8101",
  "url": "/api/media/follow-up-images/8101",
  "sortOrder": 1
}
```

固定字段：

```text
id
url
sortOrder
```

## 0.2 `FollowUpRecordResponse`

```json
{
  "id": "8001",
  "adoptionRecordId": "7001",
  "submitterId": "1001",
  "content": "最近适应得很好，已经会主动亲近家人。",
  "healthCondition": "精神和食欲正常",
  "images": [
    {
      "id": "8101",
      "url": "/api/media/follow-up-images/8101",
      "sortOrder": 1
    }
  ],
  "createdAt": "2026-08-29T16:00:00+08:00"
}
```

`FollowUpRecord` 为追加式事实记录：

```text
无 status
无 updatedAt
无 version
无 deleted
不提供 PATCH / DELETE
```

## 0.3 `FavoriteAnimalResponse`

```json
{
  "favoriteId": "9001",
  "createdAt": "2026-08-29T16:20:00+08:00",
  "animal": {
    "id": "5001",
    "name": "橘子",
    "species": "CAT",
    "sex": "MALE",
    "estimatedAgeMonths": 14,
    "color": "橘白",
    "healthCondition": "恢复良好",
    "status": "AVAILABLE",
    "coverImageUrl": "/api/media/animal-images/5101"
  }
}
```

`coverImageUrl` 必须按当前用户的 **AnimalImage 媒体读取权限**投影：

```text
当前用户对该 AnimalImage 有读取权限
→ 返回实际媒体 URL

当前用户无 AnimalImage 读取权限
→ coverImageUrl = null
```

**Favorite 关系本身不会额外授予图片读取权限。**

因此 Animal 后续变为 `SUSPENDED / ADOPTED` 后，普通收藏者若没有 M04/M05 已定义的其他授权关系：

```text
coverImageUrl = null
```

## 0.4 公告响应 DTO

公开接口与 ADMIN 管理接口使用不同 DTO，响应字段固定，不使用“可省略”。

### `AnnouncementPublicSummaryResponse`

```json
{
  "id": "10001",
  "title": "周末流浪动物义诊活动",
  "publishedAt": "2026-08-29T17:00:00+08:00"
}
```

固定字段：

```text
id
title
publishedAt
```

### `AnnouncementPublicDetailResponse`

```json
{
  "id": "10001",
  "title": "周末流浪动物义诊活动",
  "content": "本周末将开展流浪动物义诊活动……",
  "publishedAt": "2026-08-29T17:00:00+08:00"
}
```

固定字段：

```text
id
title
content
publishedAt
```

### `AnnouncementAdminSummaryResponse`

```json
{
  "id": "10001",
  "title": "周末流浪动物义诊活动",
  "status": "PUBLISHED",
  "publishedAt": "2026-08-29T17:00:00+08:00",
  "version": 1,
  "createdAt": "2026-08-29T16:30:00+08:00",
  "updatedAt": "2026-08-29T17:00:00+08:00"
}
```

固定字段：

```text
id
title
status
publishedAt
version
createdAt
updatedAt
```

### `AnnouncementAdminDetailResponse`

```json
{
  "id": "10001",
  "title": "周末流浪动物义诊活动",
  "content": "本周末将开展流浪动物义诊活动……",
  "status": "PUBLISHED",
  "createdBy": "1003",
  "updatedBy": "1003",
  "publishedAt": "2026-08-29T17:00:00+08:00",
  "version": 1,
  "createdAt": "2026-08-29T16:30:00+08:00",
  "updatedAt": "2026-08-29T17:00:00+08:00"
}
```

固定字段：

```text
id
title
content
status
createdBy
updatedBy
publishedAt
version
createdAt
updatedAt
```

## 0.5 M08 后台 DTO

### `AdminUserSummaryResponse`

```json
{
  "id": "1001",
  "account": "petlink_user01",
  "nickname": "小明",
  "phone": "13800138000",
  "roleCode": "USER",
  "status": "ENABLED",
  "createdAt": "2026-08-29T09:30:00+08:00",
  "updatedAt": "2026-08-29T09:45:00+08:00"
}
```

固定不返回：

```text
passwordHash
```

### `AdminUserDetailResponse`

```json
{
  "id": "1001",
  "account": "petlink_user01",
  "nickname": "小明",
  "phone": "13800138000",
  "roleCode": "USER",
  "status": "ENABLED",
  "createdAt": "2026-08-29T09:30:00+08:00",
  "updatedAt": "2026-08-29T09:45:00+08:00",
  "statistics": {
    "publishedClueCount": 3,
    "rescueTaskCount": 0,
    "adoptionApplicationCount": 2,
    "adoptionRecordCount": 1
  }
}
```

四个统计字段始终存在，没有对应业务时返回 `0`。

### `AdminUserActionResponse`

启用、禁用、晋升统一返回：

```json
{
  "id": "1001",
  "roleCode": "RESCUER",
  "status": "ENABLED",
  "updatedAt": "2026-08-29T17:30:00+08:00"
}
```

### `AdminStatsOverviewResponse`

固定由：

```text
users
rescueClues
rescueTasks
animals
adoptionApplications
adoptionRecords
followUps
```

七部分组成，具体字段见 4.10。

### `AdminStatsTrendPointResponse`

```json
{
  "date": "2026-08-01",
  "rescueSuccessCount": 2,
  "adoptionCount": 1
}
```

### `AdminStatsTrendsResponse`

```json
{
  "from": "2026-08-01",
  "to": "2026-08-29",
  "granularity": "DAY",
  "zoneId": "Asia/Shanghai",
  "points": []
}
```


---

# 1. M06 回访管理 API

## 1.1 模块边界

M06 负责：

```text
最终领养人提交回访
最终领养人查看本人回访
负责 RESCUER 查看其救助动物的回访
ADMIN 查看全部回访
回访图片读取
```

不设计：

```text
FollowUpTask
FollowUpRecord 修改
FollowUpRecord 删除
```

回访提交人必须是：

```text
AdoptionRecord.user_id
```

角色可以是：

```text
USER
RESCUER
```

ADMIN 不以“领养人身份”提交回访。

---

## 1.2 提交回访

```http
POST /api/adoption-records/{adoptionRecordId}/follow-ups
```

权限：

```text
USER / RESCUER
```

请求：

```json
{
  "idempotencyKey": "61f3576a-432f-4e08-8b69-642d3a6776d9",
  "content": "最近已经完全适应新家。",
  "healthCondition": "食欲、精神状态正常",
  "imageTokens": [
    "550e8400-e29b-41d4-a716-446655440030",
    "550e8400-e29b-41d4-a716-446655440031"
  ]
}
```

### 1.2.1 字段规则

```text
idempotencyKey：
- 必填
- 标准 UUID
- 服务端校验后转小写
- 永久幂等键

content：
- 可空
- 非空时 trim 后 1～2000

healthCondition：
- 可空
- 非空时 trim 后 1～1000

imageTokens：
- 可省略或空数组
- 0～9
- 同一请求不得重复
```

有效内容要求：

```text
trim 后 content 非空
OR
trim 后 healthCondition 非空
OR
至少 1 张有效图片
```

三者都无有效内容：

```text
40001 INVALID_PARAMETER
```

### 1.2.2 身份与归属

事务开始前/事务内必须验证：

```text
AdoptionRecord.id = URL adoptionRecordId
AND AdoptionRecord.user_id = 当前认证用户.id
```

客户端不得提交：

```text
submitterId
adopterId
userId
```

正式写入：

```text
submitter_id = 当前认证用户.id
```

不满足归属：

```text
40401 RESOURCE_NOT_FOUND
```

---

## 1.3 回访永久幂等规则

唯一约束：

```text
uk_follow_up_submitter_idempotency
(submitter_id, idempotency_key)
```

回访幂等的核心原则是：

```text
重复请求必须在校验原 imageTokens 之前被识别。
```

否则第一次提交已将临时文件改为 `BOUND` 后，正常重试会被文件校验错误挡住。

### 1.3.1 正式处理顺序

```text
1. 校验并规范化 idempotencyKey
   - 标准 UUID
   - 转为小写 canonical form

2. 事务外快速查询：
   WHERE submitter_id = 当前用户.id
     AND idempotency_key = canonicalKey

3. 若已存在：
   ├─ existing.adoption_record_id = URL adoptionRecordId
   │  → HTTP 200
   │  → 返回第一次 FollowUpRecordResponse
   │  → 不再校验 imageTokens
   │
   └─ existing.adoption_record_id != URL adoptionRecordId
      → 40908 IDEMPOTENCY_KEY_CONFLICT

4. 若不存在：
   → BEGIN 数据库事务

5. 事务内校验 AdoptionRecord 所有权

6. 先 INSERT FollowUpRecord

7. INSERT 成功后，再进入 TemporaryFile 锁定、文件校验、图片创建与绑定流程

8. copy 正式文件

9. COMMIT
```

这里**不使用 `request_hash`**。

同一个：

```text
submitter_id + idempotency_key
```

永久代表第一次成功插入的 FollowUpRecord。

新回访必须使用新的 `idempotencyKey`。

### 1.3.2 并发重复请求恢复

两个相同 key 的请求并发时，都可能在事务外快速查询阶段看到“不存在”。

因此：

```text
INSERT FollowUpRecord
```

仍以数据库命名唯一约束作为最终并发裁决。

仅当异常明确命中：

```text
uk_follow_up_submitter_idempotency
```

时：

```text
ROLLBACK 当前事务
↓
按 submitter_id + idempotency_key 重新查询
↓
同 adoptionRecordId
→ HTTP 200 返回第一次结果

不同 adoptionRecordId
→ 40908 IDEMPOTENCY_KEY_CONFLICT
```

其他数据库错误：

```text
不得映射成幂等重放
```

---

## 1.4 回访图片与事务规则

只有在 1.3 确认“当前 key 尚未存在”，并且事务内成功 `INSERT FollowUpRecord` 后，才处理原请求的 `imageTokens`。

若 `imageTokens` 非空：

```text
1. 校验数量 1～9
2. 请求内拒绝重复 token
3. 查询 temporary_file
4. 返回行数必须等于 token 数量
5. 按 temporary_file.id ASC FOR UPDATE
6. 校验：
   - owner_id = 当前用户
   - status = UPLOADED
   - expires_at > NOW()
   - 文件类型/大小合法
```

图片排序：

```text
按客户端 imageTokens 原顺序创建 FollowUpImage
sort_order = 1..N
```

正式事务顺序固定为：

```text
1. 校验 AdoptionRecord 所有权
   AdoptionRecord.user_id = 当前认证用户.id
   （AdoptionRecord 为不可变事实记录，不要求 FOR UPDATE）

2. INSERT FollowUpRecord

3. 若 imageTokens 非空：
   → 按 temporary_file.id ASC FOR UPDATE
   → 校验 token / owner / UPLOADED / expiry / 文件属性

4. 按客户端 imageTokens 原顺序 INSERT FollowUpImage
   sort_order = 1..N

5. 每个 TemporaryFile 条件绑定：
   status = BOUND
   business_type = FOLLOW_UP
   business_id = 新建 FollowUpRecord.id
   formal_path = 对应正式相对路径
   bound_at = NOW()
   affectedRows 必须 = 1

6. copy 临时文件 → 正式文件

7. COMMIT

8. COMMIT 后删除原临时文件

9. 安全删除对应 temporary_file 技术记录
```

失败：

```text
任何数据库写入、文件校验或 formal copy 失败
→ ROLLBACK
→ 删除仅由本事务创建的正式副本
→ 原临时文件保留
```

数据库已提交但临时文件删除失败：

```text
不回滚业务
保留 BOUND technical row
交给清理任务
```

成功：

```text
首次成功创建 → HTTP 201 Created
幂等重放     → HTTP 200 OK
```

两种情况都返回：

```text
ApiResponse<FollowUpRecordResponse>
```

---

## 1.5 查看本人领养记录下的回访

```http
GET /api/adoption-records/{adoptionRecordId}/follow-ups
```

权限：

```text
领养人本人
ADMIN
负责该 Animal 的 RESCUER
```

授权关系：

### 领养人

```text
AdoptionRecord.user_id = 当前用户.id
```

### 负责 RESCUER

```text
AdoptionRecord.animal_id
→ Animal.rescue_task_id
→ RescueTask.rescuer_id = 当前用户.id
```

### ADMIN

```text
全部可读
```

不可见：

```text
40401 RESOURCE_NOT_FOUND
```

V1.0 返回：

```text
ApiResponse<List<FollowUpRecordResponse>>
```

排序：

```text
created_at ASC, id ASC
```

---

## 1.6 查看单条回访详情

```http
GET /api/follow-ups/{followUpId}
```

权限：

```text
领养人本人
负责该 Animal 的 RESCUER
ADMIN
```

授权逻辑同 1.5。

返回：

```text
ApiResponse<FollowUpRecordResponse>
```

不可见：

```text
40401 RESOURCE_NOT_FOUND
```

---

## 1.7 ADMIN 查看全部回访

```http
GET /api/admin/follow-ups?page=1&size=20&animalId=5001&userId=1001
```

权限：

```text
ADMIN
```

可选筛选：

```text
animalId
userId
adoptionRecordId
```

排序：

```text
created_at DESC, id DESC
```

返回：

```text
ApiResponse<PageResponse<FollowUpRecordResponse>>
```

---

## 1.8 RESCUER 查看本人负责动物的回访列表

```http
GET /api/rescuer/follow-ups?page=1&size=20&animalId=5001
```

权限：

```text
RESCUER
```

数据库必须固定关联：

```text
FollowUpRecord
→ AdoptionRecord
→ Animal
→ RescueTask
→ rescuer_id = 当前用户.id
```

可选：

```text
animalId
```

但 animalId 仍必须属于当前 RESCUER 的负责范围。

排序：

```text
created_at DESC, id DESC
```

返回：

```text
ApiResponse<PageResponse<FollowUpRecordResponse>>
```

---

## 1.9 回访图片媒体接口

```http
GET /api/media/follow-up-images/{imageId}
```

可见性：

```text
找到 FollowUpImage
→ FollowUpRecord
→ AdoptionRecord
```

允许：

```text
最终领养人本人
负责该 Animal 的 RESCUER
ADMIN
```

其他访问者：

```text
40401 RESOURCE_NOT_FOUND
```

实现：

```text
从数据库 image_path 获取路径
canonicalize
确认仍在 uploads 根目录内
防止 ../ / 绝对路径 / symlink escape
```

成功：

```text
HTTP 200
响应体为图片二进制
不使用 ApiResponse 成功包装
Content-Type 根据实际文件返回
```

失败：

```text
使用全局 ApiResponse 错误结构
```

---

# 2. M07 收藏 API

## 2.1 收藏 Animal

```http
POST /api/animals/{animalId}/favorite
```

权限：

```text
USER / RESCUER
```

语义：

```text
幂等创建
```

正式流程：

```text
1. 先普通查询：
   SELECT Favorite
   WHERE user_id = 当前用户
     AND animal_id = animalId

2. 如果已存在：
   → 直接返回当前 FavoriteAnimalResponse
   → 不重新检查 Animal.status

3. 如果不存在：
   BEGIN
   → SELECT Animal ... FOR SHARE
   → 查询不到：40401
   → status != AVAILABLE：40901
   → INSERT Favorite
   → COMMIT
```

并发插入若命中命名唯一约束：

```text
uk_favorite_user_animal
```

则：

```text
重新查询已存在 Favorite
→ 正常返回
```

其他数据库错误不能映射为“已收藏”。

成功：

```http
首次创建 → 201 Created
已存在   → 200 OK
```

返回：

```text
ApiResponse<FavoriteAnimalResponse>
```

Animal 后续：

```text
SUSPENDED
ADOPTED
```

不会自动删除 Favorite。

---

## 2.2 取消收藏

```http
DELETE /api/animals/{animalId}/favorite
```

权限：

```text
USER / RESCUER
```

SQL：

```sql
DELETE FROM favorite
WHERE user_id = ?
  AND animal_id = ?;
```

语义：

```text
affectedRows = 1 → 已取消
affectedRows = 0 → 本来就未收藏
```

两种都视为成功。

成功：

```text
HTTP 200
```

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "animalId": "5001",
    "favorited": false
  }
}
```

不写 `OperationLog`。

---

## 2.3 查看本人收藏

```http
GET /api/favorites/me?page=1&size=20
```

权限：

```text
USER / RESCUER
```

固定：

```text
favorite.user_id = 当前用户
```

排序：

```text
favorite.created_at DESC, favorite.id DESC
```

返回：

```text
ApiResponse<PageResponse<FavoriteAnimalResponse>>
```

注意：

```text
Favorite 历史记录不会因为 Animal 后续 SUSPENDED / ADOPTED 自动删除。
```

因此列表允许展示已收藏但当前不可公开领养的 Animal 摘要。

若当前用户本身对该 Animal 没有额外详情/媒体权限：

```text
只返回收藏列表需要的有限展示字段
coverImageUrl = null
不要由前端再调用公开 GET /api/animals/{id} 强行获取 ADOPTED 详情
```

收藏关系本身**不授予** AnimalImage 读取权限。

---

# 3. M07 公告 API

## 3.1 公告状态机

```text
DRAFT
→ PUBLISHED
→ WITHDRAWN
```

规则：

```text
DRAFT：
可编辑
可发布

PUBLISHED：
可编辑
可撤回

WITHDRAWN：
不可编辑
不可重新发布
```

不允许：

```text
WITHDRAWN → PUBLISHED
```

---

## 3.2 公开公告列表

```http
GET /api/announcements?page=1&size=20
```

权限：

```text
permitAll
```

固定：

```text
status = PUBLISHED
```

排序：

```text
published_at DESC, id DESC
```

返回：

```text
ApiResponse<PageResponse<AnnouncementPublicSummaryResponse>>
```

公开投影不返回：

```text
createdBy
updatedBy
version
```

---

## 3.3 公开公告详情

```http
GET /api/announcements/{announcementId}
```

权限：

```text
permitAll
```

固定：

```text
status = PUBLISHED
```

若：

```text
DRAFT
WITHDRAWN
不存在
```

统一：

```text
40401 RESOURCE_NOT_FOUND
```

避免泄露非公开公告存在性。

固定返回 `ApiResponse<AnnouncementPublicDetailResponse>`。

---

## 3.4 ADMIN 创建公告草稿

```http
POST /api/admin/announcements
```

权限：

```text
ADMIN
```

请求：

```json
{
  "title": "周末流浪动物义诊活动",
  "content": "本周末将开展流浪动物义诊活动……"
}
```

规则：

```text
title trim 后 1～200
content trim 后必须非空
客户端不得提交 status = PUBLISHED
```

创建固定：

```text
status = DRAFT
created_by = 当前 ADMIN
updated_by = 当前 ADMIN
version = 0
published_at = NULL
```

同事务日志：

```text
ANNOUNCEMENT + CREATE
before_status = NULL
after_status = DRAFT
```

成功：

```http
201 Created
```

返回：

```text
ApiResponse<AnnouncementAdminDetailResponse>
```

---

## 3.5 ADMIN 查看公告管理列表

```http
GET /api/admin/announcements?page=1&size=20&status=DRAFT
```

权限：

```text
ADMIN
```

可选状态：

```text
DRAFT
PUBLISHED
WITHDRAWN
```

排序：

```text
created_at DESC, id DESC
```

返回完整管理投影：

```text
ApiResponse<PageResponse<AnnouncementAdminSummaryResponse>>
```

---

## 3.6 ADMIN 查看公告管理详情

```http
GET /api/admin/announcements/{announcementId}
```

权限：

```text
ADMIN
```

可查看：

```text
DRAFT
PUBLISHED
WITHDRAWN
```

返回完整：

```text
ApiResponse<AnnouncementAdminDetailResponse>
```

---

## 3.7 ADMIN 编辑公告

```http
PATCH /api/admin/announcements/{announcementId}
```

权限：

```text
ADMIN
```

请求：

```json
{
  "title": "更新后的标题",
  "content": "更新后的公告正文",
  "version": 2
}
```

字段：

```text
title：
字段未出现 → 保持
出现 → trim 后 1～200

content：
字段未出现 → 保持
出现 → trim 后必须非空

version：
必填，非负整数

title / content：
至少出现一个
禁止只提交 version
```

因此：

```json
{
  "version": 2
}
```

属于无意义 PATCH：

```text
40001 INVALID_PARAMETER
```

禁止：

```text
status
createdBy
updatedBy
publishedAt
```

状态条件：

```text
DRAFT / PUBLISHED
→ 允许编辑

WITHDRAWN
→ 40901 BUSINESS_STATE_CONFLICT
```

乐观锁：

```text
WHERE id = ?
AND version = ?
```

更新：

```text
updated_by = 当前 ADMIN
updated_at = NOW()
version = version + 1
```

普通内容编辑：

```text
不写 OperationLog
```

`affectedRows = 0`：

```text
重新查询
├─ 不存在 → 40401
├─ WITHDRAWN → 40901
└─ version 不一致 → 40903
```

成功返回完整：

```text
ApiResponse<AnnouncementAdminDetailResponse>
```

---

## 3.8 ADMIN 发布公告

```http
POST /api/admin/announcements/{announcementId}/publish
```

权限：

```text
ADMIN
```

请求：

```json
{
  "version": 0
}
```

规则：

```text
version 必填且非负
DRAFT → PUBLISHED
published_at = NOW()
updated_by = 当前 ADMIN
version = version + 1
```

`published_at`：

```text
记录第一次发布时刻
```

同事务日志：

```text
ANNOUNCEMENT + PUBLISH
DRAFT → PUBLISHED
```

`affectedRows = 0` 后：

```text
不存在 → 40401
状态非 DRAFT → 40901
version 不一致 → 40903
```

成功返回：

```text
ApiResponse<AnnouncementAdminDetailResponse>
```

---

## 3.9 ADMIN 撤回公告

```http
POST /api/admin/announcements/{announcementId}/withdraw
```

权限：

```text
ADMIN
```

请求：

```json
{
  "version": 1
}
```

规则：

```text
version 必填且为非负整数

PUBLISHED → WITHDRAWN
published_at 保持第一次发布时间，不清空
updated_by = 当前 ADMIN
updated_at = NOW()
version = version + 1
```

条件更新必须同时包含：

```text
WHERE id = ?
  AND status = PUBLISHED
  AND version = ?
```

同事务：

```text
ANNOUNCEMENT + WITHDRAW
PUBLISHED → WITHDRAWN
```

`affectedRows = 0`：

```text
重新查询
├─ 不存在             → 40401 RESOURCE_NOT_FOUND
├─ 状态非 PUBLISHED   → 40901 BUSINESS_STATE_CONFLICT
└─ version 不一致     → 40903 OPTIMISTIC_LOCK_CONFLICT
```

WITHDRAWN 后：

```text
不可编辑
不可再次发布
```

成功：

```text
ApiResponse<AnnouncementAdminDetailResponse>
```

---

# 4. M08 后台管理 API

## 4.1 模块边界

M08 负责：

```text
用户管理
救助人员管理
跨业务监管查询
数据统计
```

已有业务动作不在 M08 重复定义：

```text
线索审核 → M02
取消异常任务 / FAILED 处理 → M03
领养审核 → M05
公告管理 → M07
回访全局查看 → M06
```

M08 主要补齐：

```text
用户管理
跨模块查询入口
统计看板
```

---

## 4.2 ADMIN 用户列表

```http
GET /api/admin/users?page=1&size=20&roleCode=USER&status=ENABLED&keyword=zhang
```

权限：

```text
ADMIN
```

可选筛选：

```text
roleCode = USER / RESCUER / ADMIN
status = ENABLED / DISABLED
keyword = account / nickname 模糊匹配
```

排序：

```text
created_at DESC, id DESC
```

固定返回：

```text
ApiResponse<PageResponse<AdminUserSummaryResponse>>
```

`AdminUserSummaryResponse` 字段按 0.5 固定，不返回 `passwordHash`。

---

## 4.3 ADMIN 用户详情

```http
GET /api/admin/users/{userId}
```

权限：

```text
ADMIN
```

固定返回：

```text
ApiResponse<AdminUserDetailResponse>
```

固定包括：

```text
基本用户字段
statistics.publishedClueCount
statistics.rescueTaskCount
statistics.adoptionApplicationCount
statistics.adoptionRecordCount
```

这些统计均为只读派生字段，不写入 `sys_user`；没有对应记录时返回 `0`。

---

## 4.4 启用用户

```http
POST /api/admin/users/{userId}/enable
```

权限：

```text
ADMIN
```

状态：

```text
DISABLED → ENABLED
```

同一条件更新：

```text
status = ENABLED
updated_at = NOW()
```

同事务：

```text
OperationLog
business_type = SYS_USER
operation_type = ENABLE
before_status = DISABLED
after_status = ENABLED
operator_id = 当前 ADMIN
```

并发失败：

```text
不存在 → 40401
状态已不是 DISABLED → 40901
```

成功固定返回：

```text
ApiResponse<AdminUserActionResponse>
```

---

## 4.5 禁用用户

```http
POST /api/admin/users/{userId}/disable
```

权限：

```text
ADMIN
```

状态：

```text
ENABLED → DISABLED
```

同事务：

```text
OperationLog
SYS_USER + DISABLE
ENABLED → DISABLED
```

禁用后：

```text
该用户现有 JWT 在下一次受保护请求时，
因全局“每次确认当前 status”规则立即失效。
```

保护规则正式固定：

```text
ADMIN 不允许禁用自己
```

若：

```text
target.id = 当前 ADMIN.id
→ 40301 FORBIDDEN
```

禁用 `RESCUER` 时：

```text
只禁用 sys_user 账号
不自动修改其现有 RescueTask
```

如果该 RESCUER 仍存在：

```text
WAITING_START
IN_PROGRESS
```

活动任务，ADMIN 后续通过 M03 Frozen 接口：

```text
POST /api/admin/rescue-tasks/{id}/cancel
```

显式取消异常任务并使对应 RescueClue 重新开放。

成功固定返回：

```text
ApiResponse<AdminUserActionResponse>
```

---

## 4.6 USER 晋升 RESCUER

```http
POST /api/admin/users/{userId}/promote-rescuer
```

权限：

```text
ADMIN
```

前置：

```text
target.role_code = USER
```

更新：

```text
USER → RESCUER
```

不支持：

```text
RESCUER → USER
ADMIN → RESCUER
USER → ADMIN
```

同事务日志：

```text
OperationLog
business_type = SYS_USER
operation_type = PROMOTE_RESCUER
before_status = NULL
after_status = NULL
```

角色变化历史由 OperationLog 记录操作类型和操作者。

受保护请求：

```text
立即按数据库当前 role_code = RESCUER 授权
无需等待旧 JWT 过期
```

并发失败：

```text
不存在 → 40401
role 已不是 USER → 40901
```

成功固定返回：

```text
ApiResponse<AdminUserActionResponse>
```

其中：

```text
roleCode = RESCUER
```

---

## 4.7 ADMIN 救助任务监管列表

```http
GET /api/admin/rescue-tasks?page=1&size=20&status=IN_PROGRESS&rescuerId=1002
```

权限：

```text
ADMIN
```

可选：

```text
status
rescuerId
clueId
```

排序：

```text
created_at DESC, id DESC
```

只读：

```text
不在本接口执行取消任务或 FAILED 处理
```

对应动作仍使用 M03 Frozen 接口。

固定返回：

```text
ApiResponse<PageResponse<TaskSummaryResponse>>
```

---



## 4.8 ADMIN Animal 监管列表

```http
GET /api/admin/animals?page=1&size=20&status=AVAILABLE&species=CAT
```

权限：

```text
ADMIN
```

可选：

```text
status
species
rescueTaskId
```

排序：

```text
created_at DESC, id DESC
```

返回：

```text
ApiResponse<PageResponse<AnimalSummaryResponse>>
```

ADMIN 可再调用：

```text
GET /api/animals/{id}
```

查看完整详情。

---

## 4.9 ADMIN 领养记录监管列表

```http
GET /api/admin/adoption-records?page=1&size=20&userId=1001&animalId=5001
```

权限：

```text
ADMIN
```

可选：

```text
userId
animalId
applicationId
```

排序：

```text
adopted_at DESC, id DESC
```

返回：

```text
ApiResponse<PageResponse<AdoptionRecordResponse>>
```

AdoptionRecord：

```text
只读
不可修改
不可删除
```

---

## 4.10 ADMIN 总览统计

```http
GET /api/admin/stats/overview
```

权限：

```text
ADMIN
```

固定返回：

```text
ApiResponse<AdminStatsOverviewResponse>
```

完整状态统计必须覆盖 Frozen FR-STAT。

示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "users": {
      "total": 120,
      "enabled": 115,
      "disabled": 5,
      "users": 99,
      "rescuers": 18,
      "admins": 3
    },
    "rescueClues": {
      "PENDING_REVIEW": 6,
      "REJECTED": 12,
      "WITHDRAWN": 4,
      "WAITING_ACCEPT": 5,
      "CONVERTED": 3,
      "CLOSED": 78
    },
    "rescueTasks": {
      "WAITING_START": 2,
      "IN_PROGRESS": 3,
      "SUCCESS": 72,
      "FAILED": 9,
      "CANCELED": 5
    },
    "animals": {
      "TREATING": 5,
      "OBSERVING": 4,
      "AVAILABLE": 22,
      "SUSPENDED": 2,
      "ADOPTED": 61
    },
    "adoptionApplications": {
      "PENDING": 13,
      "APPROVED": 61,
      "REJECTED": 34,
      "WITHDRAWN": 8,
      "INVALIDATED": 47
    },
    "adoptionRecords": {
      "total": 61
    },
    "followUps": {
      "total": 148
    }
  }
}
```

统计口径：

```text
RescueClue / RescueTask / Animal / AdoptionApplication
→ 按当前 status 全量计数

AdoptionRecord / FollowUpRecord
→ 历史累计记录数
```

所有状态字段都必须返回，即使数量为 `0` 也不能省略。

---

## 4.11 ADMIN 救助与领养趋势统计

```http
GET /api/admin/stats/trends?from=2026-08-01&to=2026-08-29&granularity=DAY
```

权限：

```text
ADMIN
```

V1.0 固定：

```text
granularity = DAY
zoneId = Asia/Shanghai
from / to 使用 yyyy-MM-dd
from、to 均包含在统计范围内
from <= to
```

其他 granularity：

```text
40001 INVALID_PARAMETER
```

统计口径正式固定：

```text
rescueSuccessCount
= RescueTask.status = SUCCESS
  AND finished_at 落入该 Asia/Shanghai 日期桶
  的任务数量

adoptionCount
= AdoptionRecord.adopted_at 落入该 Asia/Shanghai 日期桶
  的记录数量
```

返回：

```text
ApiResponse<AdminStatsTrendsResponse>
```

示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "from": "2026-08-01",
    "to": "2026-08-03",
    "granularity": "DAY",
    "zoneId": "Asia/Shanghai",
    "points": [
      {
        "date": "2026-08-01",
        "rescueSuccessCount": 2,
        "adoptionCount": 1
      },
      {
        "date": "2026-08-02",
        "rescueSuccessCount": 0,
        "adoptionCount": 0
      },
      {
        "date": "2026-08-03",
        "rescueSuccessCount": 1,
        "adoptionCount": 2
      }
    ]
  }
}
```

无数据日期：

```text
必须补零
```

不能直接省略日期点。

V1.0 不设计复杂 BI、预测或推荐。

---

# 5. M06 接口汇总

| 编号 | Method | Path | 功能 | 权限 | 成功 HTTP |
|---|---|---|---|---|---:|
| M06-API-01 | POST | `/api/adoption-records/{id}/follow-ups` | 提交回访 | 最终领养人 USER/RESCUER | 201/200(幂等重放) |
| M06-API-02 | GET | `/api/adoption-records/{id}/follow-ups` | 查看某领养记录全部回访 | 领养人/负责 RESCUER/ADMIN | 200 |
| M06-API-03 | GET | `/api/follow-ups/{id}` | 查看单条回访 | 领养人/负责 RESCUER/ADMIN | 200 |
| M06-API-04 | GET | `/api/admin/follow-ups` | ADMIN 查看全部回访 | ADMIN | 200 |
| M06-API-05 | GET | `/api/rescuer/follow-ups` | RESCUER 查看负责动物回访 | RESCUER | 200 |
| COMMON-MEDIA-03 | GET | `/api/media/follow-up-images/{imageId}` | 读取回访图片 | 领养人/负责 RESCUER/ADMIN | 200 |

---

# 6. M07 接口汇总

## 收藏

| 编号 | Method | Path | 功能 | 权限 | 成功 HTTP |
|---|---|---|---|---|---:|
| M07-API-01 | POST | `/api/animals/{id}/favorite` | 收藏 Animal | USER/RESCUER | 201/200 |
| M07-API-02 | DELETE | `/api/animals/{id}/favorite` | 取消收藏 | USER/RESCUER | 200 |
| M07-API-03 | GET | `/api/favorites/me` | 查看本人收藏 | USER/RESCUER | 200 |

## 公告

| 编号 | Method | Path | 功能 | 权限 | 成功 HTTP |
|---|---|---|---|---|---:|
| M07-API-04 | GET | `/api/announcements` | 公开公告列表 | permitAll | 200 |
| M07-API-05 | GET | `/api/announcements/{id}` | 公开公告详情 | permitAll | 200 |
| M07-API-06 | POST | `/api/admin/announcements` | 创建 DRAFT | ADMIN | 201 |
| M07-API-07 | GET | `/api/admin/announcements` | 管理公告列表 | ADMIN | 200 |
| M07-API-08 | GET | `/api/admin/announcements/{id}` | 管理公告详情 | ADMIN | 200 |
| M07-API-09 | PATCH | `/api/admin/announcements/{id}` | 编辑 DRAFT/PUBLISHED | ADMIN | 200 |
| M07-API-10 | POST | `/api/admin/announcements/{id}/publish` | 发布公告 | ADMIN | 200 |
| M07-API-11 | POST | `/api/admin/announcements/{id}/withdraw` | 撤回公告 | ADMIN | 200 |

---

# 7. M08 接口汇总

| 编号 | Method | Path | 功能 | 权限 | 成功 HTTP |
|---|---|---|---|---|---:|
| M08-API-01 | GET | `/api/admin/users` | 用户列表 | ADMIN | 200 |
| M08-API-02 | GET | `/api/admin/users/{id}` | 用户详情 | ADMIN | 200 |
| M08-API-03 | POST | `/api/admin/users/{id}/enable` | 启用用户 | ADMIN | 200 |
| M08-API-04 | POST | `/api/admin/users/{id}/disable` | 禁用用户 | ADMIN | 200 |
| M08-API-05 | POST | `/api/admin/users/{id}/promote-rescuer` | USER 晋升 RESCUER | ADMIN | 200 |
| M08-API-06 | GET | `/api/admin/rescue-tasks` | 救助任务监管 | ADMIN | 200 |
| M08-API-07 | GET | `/api/admin/animals` | Animal 监管 | ADMIN | 200 |
| M08-API-08 | GET | `/api/admin/adoption-records` | 领养记录监管 | ADMIN | 200 |
| M08-API-09 | GET | `/api/admin/stats/overview` | 后台总览统计 | ADMIN | 200 |
| M08-API-10 | GET | `/api/admin/stats/trends` | 救助/领养时间趋势 | ADMIN | 200 |

---

# 8. 新增错误码

```text
40908 IDEMPOTENCY_KEY_CONFLICT
```

其余复用全局错误码。

---

# 9. M06～M08 API V1.0 最终冻结结论

本轮没有修改 Frozen 数据库结构。

最终固定：

```text
1. FollowUp 永久幂等：
   先规范化 key 并事务外快速查询；
   已存在时直接执行幂等返回，不校验旧 imageTokens；
   不存在时 BEGIN → 校验 AdoptionRecord 所有权
   → 先 INSERT FollowUpRecord
   → 再锁 TemporaryFile(id ASC)
   → FollowUpImage → BOUND → copy → COMMIT。
   并发唯一冲突仅识别命名唯一约束后做幂等恢复。

2. FollowUp 三类读取权限：
   最终领养人
   负责该 Animal 的 RESCUER
   ADMIN。
   FollowUpRecord 永久 append-only，不提供修改/删除。

3. Favorite：
   已收藏直接返回且不重查 Animal.status；
   未收藏才 Animal FOR SHARE 并要求 AVAILABLE；
   Animal 状态变化不自动删除 Favorite。

4. FavoriteAnimalResponse.coverImageUrl：
   仅当前用户具有 AnimalImage 媒体权限时返回；
   Favorite 本身不授予媒体权限；
   无权限时固定返回 null。

5. Announcement：
   使用四个固定 DTO：
   AnnouncementPublicSummaryResponse
   AnnouncementPublicDetailResponse
   AnnouncementAdminSummaryResponse
   AnnouncementAdminDetailResponse。

6. Announcement PATCH：
   version 必填；
   title / content 至少出现一个；
   禁止只提交 version。

7. Announcement WITHDRAW：
   version 必填且非负；
   WHERE id + status=PUBLISHED + version；
   affectedRows=0 后区分 40401 / 40901 / 40903。

8. M08 用户 DTO 与动作响应固定：
   AdminUserSummaryResponse
   AdminUserDetailResponse
   AdminUserActionResponse。
   启用、禁用、晋升统一返回 AdminUserActionResponse。

9. ADMIN 禁止禁用自己。
   禁用 RESCUER 只禁用账号，不自动修改 RescueTask；
   活动任务由 ADMIN 使用 M03 cancel 接口显式处理。

10. M08 监管列表：
    救助任务固定返回 PageResponse<TaskSummaryResponse>；
    Animal 固定返回 PageResponse<AnimalSummaryResponse>；
    AdoptionRecord 固定返回 PageResponse<AdoptionRecordResponse>。

11. FR-STAT 总览统计必须覆盖全部状态：
    RescueClue 6 状态；
    RescueTask 5 状态；
    Animal 5 状态；
    AdoptionApplication 5 状态。

12. 增加：
    GET /api/admin/stats/trends
    V1.0 granularity 固定 DAY；
    日期桶使用 Asia/Shanghai；
    rescueSuccessCount 按 SUCCESS Task.finished_at；
    adoptionCount 按 AdoptionRecord.adopted_at；
    无数据日期补零。
```

> **✅ M06 回访管理 API V1.0 — Frozen**  
> **✅ M07 收藏与公告 API V1.0 — Frozen**  
> **✅ M08 后台管理 API V1.0 — Frozen**

