# PetLink REST API 设计 V1.0 — M04 动物档案 + M05 领养管理

> 状态：Frozen  
> 前置依赖：全局 API、M01、M02、M03 已 Frozen  
> Base URL：`/api`

---

# 0. M04/M05 通用 DTO

## 0.1 `AnimalSummaryResponse`

```json
{
  "id": "5001",
  "name": "橘子",
  "species": "CAT",
  "sex": "UNKNOWN",
  "estimatedAgeMonths": 12,
  "color": "橘色",
  "healthCondition": "后腿外伤，恢复中",
  "status": "AVAILABLE",
  "coverImageUrl": "/api/media/animal-images/5101",
  "version": 3,
  "createdAt": "2026-08-29T11:20:00+08:00",
  "updatedAt": "2026-08-29T13:00:00+08:00"
}
```

## 0.2 `AnimalDetailResponse`

在 `AnimalSummaryResponse` 基础上补充：

```text
rescueTaskId
suspendReason
images[]
healthRecords[]
```

其中 `images[]` 固定为：

```json
[
  {
    "id": "5101",
    "url": "/api/media/animal-images/5101",
    "sortOrder": 1
  }
]
```

健康记录按访问权限使用不同投影。

### `HealthRecordPublicResponse`

用于 VISITOR、普通 USER，以及不是该 Animal 负责人的 RESCUER：

```json
{
  "id": "5201",
  "content": "已完成清创并包扎",
  "createdAt": "2026-08-29T11:25:00+08:00"
}
```

固定字段：

```text
id
content
createdAt
```

不暴露：

```text
recorderId
```

### `HealthRecordResponse`

仅用于负责该 Animal 的 RESCUER 或 ADMIN：

```json
{
  "id": "5201",
  "recorderId": "1002",
  "content": "已完成清创并包扎",
  "createdAt": "2026-08-29T11:25:00+08:00"
}
```

固定字段：

```text
id
recorderId
content
createdAt
```

因此 `AnimalDetailResponse.healthRecords[]` 的字段投影取决于当前访问者权限：

```text
公开/普通访问 → HealthRecordPublicResponse
负责 RESCUER / ADMIN → HealthRecordResponse
```

## 0.3 `AdoptionApplicationSummaryResponse`

```json
{
  "id": "6001",
  "animalId": "5001",
  "animalName": "橘子",
  "status": "PENDING",
  "createdAt": "2026-08-29T14:00:00+08:00",
  "updatedAt": "2026-08-29T14:00:00+08:00"
}
```

## 0.4 `AdoptionApplicationDetailResponse`

在 Summary 基础上补充：

```text
userId
adoptionReason
housingCondition
familyMembers
petExperience
contact
reviewerId
reviewedAt
rejectReason
animal
```

`animal` 使用精简动物展示投影。

`coverImageUrl` 的返回规则：

```text
若当前查看者对该 Animal 图片具有媒体读取权限
→ 正常返回 coverImageUrl

若当前查看者只是历史申请人，
且不是最终领养人，
同时 Animal 已因 ADOPTED / SUSPENDED 等状态对其不可见
→ coverImageUrl = null
```

例如：

```text
Application A → APPROVED → 产生 AdoptionRecord
Application B → INVALIDATED

Animal → ADOPTED

A 的领养人查看：
coverImageUrl 正常返回，可读取

B 的历史申请人查看自己的申请详情：
coverImageUrl = null
```

不返回一个当前用户必然无法访问的媒体 URL。

## 0.5 `AdoptionRecordResponse`

```json
{
  "id": "7001",
  "applicationId": "6001",
  "animalId": "5001",
  "userId": "1001",
  "adoptedAt": "2026-08-29T15:00:00+08:00",
  "createdAt": "2026-08-29T15:00:00+08:00",
  "animal": {
    "id": "5001",
    "name": "橘子",
    "species": "CAT",
    "sex": "MALE",
    "coverImageUrl": "/api/media/animal-images/5101"
  }
}
```

其中 `animal` 是领养记录授权场景下的展示摘要：

```text
id
name
species
sex
coverImageUrl
```

这不代表将 `ADOPTED` Animal 重新暴露到公开动物接口。

## 0.6 `AdoptionAuditResponse`

同一个审核接口无论批准还是拒绝，都固定返回：

```text
ApiResponse<AdoptionAuditResponse>
```

结构：

```json
{
  "application": {
    "id": "6001",
    "status": "REJECTED"
  },
  "adoptionRecord": null
}
```

字段：

```text
application: AdoptionApplicationDetailResponse
adoptionRecord: AdoptionRecordResponse | null
```

语义：

```text
REJECT  → adoptionRecord = null
APPROVE → adoptionRecord = 新建 AdoptionRecordResponse
```

所有 BIGINT ID 继续按全局规范输出为字符串。

---

# 1. M04 动物档案 API

## 1.1 模块边界

M04 负责：

```text
公开浏览 AVAILABLE 动物
查看动物详情
负责 RESCUER / ADMIN 维护动物基本信息
负责 RESCUER / ADMIN 管理动物图片
负责 RESCUER / ADMIN 添加健康记录
负责 RESCUER / ADMIN 查看健康记录
维护动物状态：
TREATING → OBSERVING
OBSERVING → AVAILABLE
AVAILABLE → SUSPENDED
SUSPENDED → AVAILABLE
```

注意：

```text
AVAILABLE → ADOPTED
```

不能由 M04 普通状态接口执行，只能在 M05 的 ADMIN 领养批准事务中自动发生。

---

## 1.2 Animal 可见性

### VISITOR / USER

公开列表、公开详情仅允许：

```text
Animal.status = AVAILABLE
```

### RESCUER

可查看：

```text
AVAILABLE 公共动物
OR
该 Animal.rescue_task_id 对应 RescueTask.rescuer_id = 当前用户
```

负责动物即使处于：

```text
TREATING
OBSERVING
SUSPENDED
ADOPTED
```

也可查看。

### ADMIN

可查看全部 Animal。

对无权查看的资源统一返回：

```text
40401 RESOURCE_NOT_FOUND
```

---

## 1.3 公开动物列表

```http
GET /api/animals?page=1&size=20&species=CAT&sex=FEMALE
```

权限：

```text
permitAll
```

数据库固定条件：

```text
status = AVAILABLE
```

可选筛选：

```text
species
sex
```

排序：

```text
created_at DESC, id DESC
```

返回：

```text
ApiResponse<PageResponse<AnimalSummaryResponse>>
```

任何客户端传入的：

```text
status
rescueTaskId
```

均不作为公开查询条件。

---

## 1.4 查看动物详情

```http
GET /api/animals/{animalId}
```

权限：

```text
permitAll / USER / RESCUER / ADMIN
```

按 1.2 可见性判断。

返回：

```text
ApiResponse<AnimalDetailResponse>
```

公开访问 AVAILABLE Animal 时：

```text
不返回内部救助人员隐私字段
healthRecords[] 使用 HealthRecordPublicResponse
```

负责该 Animal 的 RESCUER / ADMIN：

```text
healthRecords[] 使用 HealthRecordResponse
```

`rescueTaskId` 仅在负责 RESCUER 或 ADMIN 视图中返回。

---

## 1.5 RESCUER 查看本人负责动物

```http
GET /api/animals/responsible/me?page=1&size=20&status=TREATING
```

权限：

```text
RESCUER
```

固定关联：

```text
Animal.rescue_task_id
→ RescueTask.id
→ RescueTask.rescuer_id = 当前用户
```

可选状态：

```text
TREATING
OBSERVING
AVAILABLE
SUSPENDED
ADOPTED
```

排序：

```text
created_at DESC, id DESC
```

返回：

```text
ApiResponse<PageResponse<AnimalSummaryResponse>>
```

---

## 1.6 修改动物基本信息

```http
PATCH /api/animals/{animalId}
```

权限：

```text
负责该 Animal 的 RESCUER
ADMIN
```

请求示例：

```json
{
  "name": "橘子",
  "species": "CAT",
  "sex": "MALE",
  "estimatedAgeMonths": 14,
  "color": "橘白",
  "healthCondition": "恢复良好",
  "version": 3
}
```

允许字段：

```text
name
species
sex
estimatedAgeMonths
color
healthCondition
version
```

禁止字段：

```text
id
rescueTaskId
status
suspendReason
createdAt
updatedAt
```

字段规则：

```text
name：trim 后 1～100
species：trim 后 1～50
sex：MALE / FEMALE / UNKNOWN
estimatedAgeMonths：null 或 0～65535
color：null 或 trim 后 1～100
healthCondition：trim 后 1～1000
version：必填
```

更新必须走乐观锁：

```sql
UPDATE animal
SET ...,
    version = version + 1,
    updated_at = NOW()
WHERE id = ?
  AND version = ?;
```

`affectedRows = 0`：

```text
重新查询
├─ 不存在 / 不可见 → 40401
└─ 存在但 version 不一致 → 40903
```

成功：

```text
ApiResponse<AnimalDetailResponse>
```

注意：

```text
Animal.health_condition
```

是当前摘要，不自动改写历史 `HealthRecord`。

---

## 1.7 添加动物图片

```http
POST /api/animals/{animalId}/images
```

权限：

```text
负责该 Animal 的 RESCUER
ADMIN
```

请求：

```json
{
  "imageTokens": [
    "550e8400-e29b-41d4-a716-446655440020"
  ]
}
```

规则：

```text
imageTokens 本次数量 1～9
本次 token 不得重复
Animal 无“生命周期最多 9 张图片”限制
```

也就是说：

```text
一次追加最多 9 张
但该 Animal 历史累计图片可以超过 9 张
```

锁顺序：

```text
1. Animal FOR UPDATE
2. plain SELECT RescueTask，仅用于负责关系权限校验
3. TemporaryFile 按 id ASC FOR UPDATE
```

禁止：

```text
先锁 RescueTask，再锁 Animal
```

因为 Animal 图片操作已经冻结为：

```text
锁父 Animal
→ plain-read Task 做权限判断
```

图片排序：

```text
当前 max(sort_order) + 1 开始
按客户端 imageTokens 原顺序追加
```

TemporaryFile 条件绑定：

```text
status = BOUND
business_type = ANIMAL
business_id = Animal.id
formal_path = 服务端正式相对路径
bound_at = NOW()
affectedRows = 1
```

文件 copy / commit / cleanup 使用 Frozen 通用文件规则。

成功：

```http
201 Created
```

返回：

```text
ApiResponse<AnimalDetailResponse>
```

---

## 1.8 删除动物图片

```http
DELETE /api/animals/{animalId}/images/{imageId}
```

权限：

```text
负责该 Animal 的 RESCUER
ADMIN
```

流程：

```text
1. Animal FOR UPDATE
2. plain-read RescueTask 做权限校验
3. 校验 imageId 属于当前 Animal
4. DELETE AnimalImage
5. 按原 sort_order ASC 依次将后续图片前移 1
6. COMMIT
7. COMMIT 后删除正式物理文件
```

因为：

```text
UNIQUE(animal_id, sort_order)
```

不能无序重排。

Animal 图片没有“至少保留 1 张”的全局要求。

成功返回：

```text
ApiResponse<AnimalDetailResponse>
```

---

## 1.9 Animal 图片媒体接口

```http
GET /api/media/animal-images/{imageId}
```

允许读取 `AnimalImage`，当且仅当满足以下任一条件：

```text
A. 当前访问者符合所属 Animal 的 1.2 可见性规则；

OR

B. 当前用户已认证，且存在对应 AdoptionRecord：
   AdoptionRecord.animal_id = Animal.id
   AND AdoptionRecord.user_id = 当前认证用户.id
```

因此：

```text
VISITOR / 普通历史申请人
→ Animal 已非 AVAILABLE 且无其他可见权限时，不能读取图片

最终领养人
→ 即使 Animal.status = ADOPTED，
  仍可通过自己的 AdoptionRecord 授权读取该 Animal 的图片

负责 RESCUER / ADMIN
→ 继续按原 Animal 可见性规则读取
```

不满足任一条件：

```text
40401 RESOURCE_NOT_FOUND
```

该授权只作用于图片媒体读取，不会把 `ADOPTED` Animal 重新加入公开动物列表或公开详情。

成功：

```text
HTTP 200
图片二进制
不使用 ApiResponse 成功包装
Content-Type 根据实际图片类型返回
```

失败：

```text
仍使用全局 ApiResponse 错误结构
```

路径规则：

```text
从数据库 image_path 取得
canonicalize
必须位于 uploads 根目录
客户端不得提交物理路径
防止 ../、绝对路径、符号链接逃逸
```

---

## 1.10 添加健康记录

```http
POST /api/animals/{animalId}/health-records
```

权限：

```text
负责该 Animal 的 RESCUER
ADMIN
```

请求：

```json
{
  "content": "今日复查，伤口恢复良好，继续换药三天。"
}
```

规则：

```text
content trim 后 1～2000
recorder_id = 当前认证用户
```

事务：

```text
1. Animal FOR UPDATE
2. plain-read RescueTask 做负责关系权限校验
3. INSERT HealthRecord
4. COMMIT
```

`HealthRecord`：

```text
append-only
不提供修改
不提供删除
```

成功：

```http
201 Created
```

返回新建：

```text
ApiResponse<HealthRecordResponse>
```

本接口**不会自动更新**：

```text
Animal.health_condition
```

若前端希望同时修改当前健康摘要，应另行调用：

```text
PATCH /api/animals/{animalId}
```

V1.0 不设计跨接口自动联动。

---

## 1.11 查看健康记录

```http
GET /api/animals/{animalId}/health-records
```

权限：

```text
复用所属 Animal 可见性规则
```

V1.0 根据权限固定投影：

```text
VISITOR / 普通 USER / 非负责 RESCUER
→ ApiResponse<List<HealthRecordPublicResponse>>

负责该 Animal 的 RESCUER / ADMIN
→ ApiResponse<List<HealthRecordResponse>>
```

排序：

```text
created_at ASC, id ASC
```

---

## 1.12 动物状态动作

状态动作统一使用：

```http
POST /api/animals/{animalId}/status-actions
```

权限：

```text
负责该 Animal 的 RESCUER
ADMIN
```

请求只允许以下动作：

```text
TO_OBSERVING
OPEN_ADOPTION
SUSPEND_ADOPTION
RESUME_ADOPTION
```

字段矩阵正式固定：

| action | version | suspendReason |
|---|---|---|
| `TO_OBSERVING` | 必填，非负整数 | 禁止出现 |
| `OPEN_ADOPTION` | 必填，非负整数 | 禁止出现 |
| `SUSPEND_ADOPTION` | 必填，非负整数 | 必填，trim 后 1～500 |
| `RESUME_ADOPTION` | 必填，非负整数 | 禁止出现 |

统一规则：

```text
version < 0
未知 action
出现当前 action 不允许的字段
缺少当前 action 必填字段
→ 40001 INVALID_PARAMETER
```

### 1.12.1 `TO_OBSERVING`

请求：

```json
{
  "action": "TO_OBSERVING",
  "version": 3
}
```

迁移：

```text
TREATING → OBSERVING
```

日志：

```text
ANIMAL + TO_OBSERVING
```

### 1.12.2 `OPEN_ADOPTION`

请求：

```json
{
  "action": "OPEN_ADOPTION",
  "version": 4
}
```

迁移：

```text
OBSERVING → AVAILABLE
```

日志：

```text
ANIMAL + OPEN_ADOPTION
```

### 1.12.3 `SUSPEND_ADOPTION`

请求：

```json
{
  "action": "SUSPEND_ADOPTION",
  "suspendReason": "近期需要进一步治疗，暂缓领养",
  "version": 5
}
```

规则：

```text
AVAILABLE → SUSPENDED
suspendReason trim 后 1～500
```

日志：

```text
ANIMAL + SUSPEND_ADOPTION
reason = suspendReason
```

### 1.12.4 `RESUME_ADOPTION`

请求：

```json
{
  "action": "RESUME_ADOPTION",
  "version": 6
}
```

迁移：

```text
SUSPENDED → AVAILABLE
suspend_reason → NULL
```

日志：

```text
ANIMAL + RESUME_ADOPTION
```

### 1.12.5 状态动作并发规则

Animal 带 `version`，状态动作采用乐观锁：

```text
WHERE id = ?
  AND status = 原状态
  AND version = ?
```

同一 UPDATE：

```text
status
suspend_reason
version = version + 1
updated_at = NOW()
```

同事务 INSERT 对应 OperationLog。

`affectedRows = 0`：

```text
重新查询
├─ 不存在 / 不可见 → 40401
├─ status 已变化   → 40901 BUSINESS_STATE_CONFLICT
└─ version 不一致  → 40903 OPTIMISTIC_LOCK_CONFLICT
```

成功统一返回：

```text
ApiResponse<AnimalDetailResponse>
```

禁止通过该接口执行：

```text
ADOPT
```

---

# 2. M05 领养管理 API

## 2.1 模块边界

M05 负责：

```text
USER / RESCUER 提交领养申请
查看本人申请
查看本人申请详情
撤回本人 PENDING 申请
ADMIN 查看申请审核队列
ADMIN 审核领养申请
批准后自动生成 AdoptionRecord
查看本人领养记录
RESCUER 查看其负责动物的领养情况（只读）
```

申请人：

```text
USER
RESCUER
```

都可以申请任意：

```text
AVAILABLE Animal
```

包括：

```text
RESCUER 自己曾救助的 Animal
```

ADMIN 不提交领养申请。

---

## 2.2 领养申请状态机

```text
PENDING
├── APPROVED
├── REJECTED
├── WITHDRAWN
└── INVALIDATED
```

只有：

```text
PENDING
```

可以人工审核或由申请人撤回。

客户端不得直接更新 `status`。

---

## 2.3 提交领养申请

```http
POST /api/animals/{animalId}/adoption-applications
```

权限：

```text
USER / RESCUER
```

请求：

```json
{
  "adoptionReason": "希望长期照顾它，并能承担医疗与生活费用",
  "housingCondition": "自有住房，可养宠",
  "familyMembers": "3人，家人均同意",
  "petExperience": "曾养猫5年",
  "contact": "13800138000"
}
```

字段：

```text
adoptionReason：trim 后 1～1000
housingCondition：trim 后 1～1000
familyMembers：trim 后 1～1000
petExperience：trim 后 1～1000
contact：trim 后 1～100
```

身份：

```text
user_id = 当前 JWT userId
```

客户端不得提交：

```text
userId
status
reviewerId
reviewedAt
rejectReason
approvedAnimalId
```

事务：

```text
1. SELECT Animal FOR UPDATE

2. 若按 animalId 查询不到：
   → 40401 RESOURCE_NOT_FOUND

3. 若查询到但 Animal.status != AVAILABLE：
   → 40901 BUSINESS_STATE_CONFLICT

4. status = AVAILABLE：
   → INSERT AdoptionApplication
      user_id = 当前用户
      animal_id = URL animalId
      status = PENDING

5. INSERT OperationLog：
   ADOPTION_APPLICATION + CREATE
   before_status = NULL
   after_status = PENDING

6. COMMIT
```

这里不再先使用“公开可见性”过滤 Animal，因为申请事务需要明确区分：

```text
资源不存在 → 404
资源存在但已不可领养 → 409
```

唯一约束：

```text
UNIQUE(user_id, animal_id)
```

含义：

```text
同一用户对同一 Animal 一生最多提交一次申请
```

若已存在历史：

```text
PENDING
APPROVED
REJECTED
WITHDRAWN
INVALIDATED
```

均不能再次申请。

并发唯一冲突只映射已知命名唯一约束：

```text
uk_adoption_application_user_animal
→ 40907 ADOPTION_APPLICATION_ALREADY_EXISTS
```

不能把其他数据库异常都映射成重复申请。

成功：

```http
201 Created
```

返回：

```text
ApiResponse<AdoptionApplicationDetailResponse>
```

---

## 2.4 查看本人领养申请

```http
GET /api/adoption-applications/me?page=1&size=20&status=PENDING
```

权限：

```text
USER / RESCUER
```

固定：

```text
user_id = 当前用户
```

可选状态：

```text
PENDING
APPROVED
REJECTED
WITHDRAWN
INVALIDATED
```

排序：

```text
created_at DESC, id DESC
```

返回：

```text
ApiResponse<PageResponse<AdoptionApplicationSummaryResponse>>
```

---

## 2.5 查看本人申请详情

```http
GET /api/adoption-applications/{applicationId}
```

权限：

```text
申请人本人
ADMIN
```

RESCUER 若只是“负责该 Animal”但不是申请人：

```text
不通过本接口读取完整申请隐私
```

负责 RESCUER 的只读领养情况通过后面的专用接口获取。

不可见：

```text
40401 RESOURCE_NOT_FOUND
```

返回：

```text
ApiResponse<AdoptionApplicationDetailResponse>
```

---

## 2.6 撤回本人领养申请

```http
POST /api/adoption-applications/{applicationId}/withdraw
```

权限：

```text
USER / RESCUER
```

条件更新：

```sql
UPDATE adoption_application
SET status = 'WITHDRAWN',
    updated_at = NOW()
WHERE id = ?
  AND user_id = ?
  AND status = 'PENDING';
```

同事务日志：

```text
ADOPTION_APPLICATION + WITHDRAW
PENDING → WITHDRAWN
```

`reviewer_id / reviewed_at / reject_reason`：

```text
仍保持 NULL
```

`affectedRows = 0`：

```text
重新查询
├─ 不存在 / 非本人 → 40401
└─ 状态已变化     → 40901
```

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "6001",
    "status": "WITHDRAWN",
    "updatedAt": "2026-08-29T14:20:00+08:00"
  }
}
```

---

## 2.7 ADMIN 查看领养审核队列

```http
GET /api/admin/adoption-applications?page=1&size=20&status=PENDING
```

权限：

```text
ADMIN
```

默认：

```text
status = PENDING
```

PENDING 审核队列排序：

```text
created_at ASC, id ASC
```

返回：

```text
ApiResponse<PageResponse<AdoptionApplicationSummaryResponse>>
```

---

## 2.8 ADMIN 审核领养申请

```http
POST /api/admin/adoption-applications/{applicationId}/audit
```

权限：

```text
ADMIN
```

### 2.8.1 拒绝

请求：

```json
{
  "decision": "REJECT",
  "rejectReason": "居住条件暂不适合该动物"
}
```

规则：

```text
rejectReason trim 后 1～500
```

锁：

```text
SELECT target AdoptionApplication FOR UPDATE
```

**拒绝分支不锁 Animal。**

前置：

```text
Application.status = PENDING
target.user_id != 当前认证 ADMIN.id
```

若：

```text
target.user_id = 当前认证 ADMIN.id
```

说明该 ADMIN 在历史角色阶段曾提交过此申请。Service 必须主动拒绝：

```text
40301 FORBIDDEN
→ 回滚事务
→ 不写 OperationLog
```

同事务：

```text
Application：
PENDING → REJECTED
reviewer_id = 当前 ADMIN
reviewed_at = NOW()
reject_reason = rejectReason

OperationLog：
ADOPTION_APPLICATION + AUDIT_REJECT
PENDING → REJECTED
reason = rejectReason
```

成功：

```text
ApiResponse<AdoptionAuditResponse>
```

其中：

```text
adoptionRecord = null
```

### 2.8.2 批准

请求：

```json
{
  "decision": "APPROVE"
}
```

规则：

```text
rejectReason 禁止出现
```

#### 冻结锁顺序

批准事务必须：

```text
1. 根据 applicationId 普通读取/定位 animalId
2. SELECT Animal FOR UPDATE
3. SELECT target AdoptionApplication FOR UPDATE
4. 校验：
   - target.id = applicationId
   - target.animal_id = 已锁 Animal.id
   - target.status = PENDING
   - Animal.status = AVAILABLE
   - target.user_id != 当前认证 ADMIN.id

   若 target.user_id = 当前 ADMIN.id：
   → 40301 FORBIDDEN
   → 回滚事务
   → 不写 OperationLog

5. SELECT 同 Animal 其他 PENDING AdoptionApplication
   ORDER BY id ASC
   FOR UPDATE
```

即：

```text
Animal
→ target Application
→ other same-animal PENDING Applications by id ASC
```

禁止改变锁顺序。

#### 批准事务内容

同事务：

```text
1. target Application：
   PENDING → APPROVED
   reviewer_id = 当前 ADMIN
   reviewed_at = NOW()
   reject_reason = NULL

2. INSERT AdoptionRecord：
   application_id = target.id
   animal_id = target.animal_id
   user_id = target.user_id
   adopted_at = NOW()

3. Animal：
   AVAILABLE → ADOPTED
   version = version + 1
   updated_at = NOW()

4. 其他同 Animal 的 PENDING Application：
   PENDING → INVALIDATED
   reviewer_id = NULL
   reviewed_at = NULL
   reject_reason = NULL

5. OperationLog：
   ADOPTION_APPLICATION + AUDIT_APPROVE
   PENDING → APPROVED

6. OperationLog：
   ANIMAL + ADOPT
   AVAILABLE → ADOPTED

7. 对每个被自动失效的申请写：
   ADOPTION_APPLICATION + AUTO_INVALIDATE
   PENDING → INVALIDATED

8. COMMIT
```

来源字段全部从已锁 target Application 复制：

```text
AdoptionRecord.application_id
AdoptionRecord.animal_id
AdoptionRecord.user_id
```

客户端不能提供或覆盖。

数据库唯一约束作为最终兜底：

```text
AdoptionRecord.application_id UNIQUE
AdoptionRecord.animal_id UNIQUE
AdoptionApplication.approved_animal_id UNIQUE
```

任意：

```text
Application 更新
AdoptionRecord 创建
Animal 更新
AUTO_INVALIDATE
OperationLog
```

失败：

```text
整个批准事务回滚
```

成功固定返回：

```text
ApiResponse<AdoptionAuditResponse>
```

示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "application": {
      "id": "6001",
      "status": "APPROVED",
      "reviewedAt": "2026-08-29T15:00:00+08:00"
    },
    "adoptionRecord": {
      "id": "7001",
      "applicationId": "6001",
      "animalId": "5001",
      "userId": "1001",
      "adoptedAt": "2026-08-29T15:00:00+08:00",
      "createdAt": "2026-08-29T15:00:00+08:00",
      "animal": {
        "id": "5001",
        "name": "橘子",
        "species": "CAT",
        "sex": "MALE",
        "coverImageUrl": "/api/media/animal-images/5101"
      }
    }
  }
}
```

---

## 2.9 审核并发与错误规则

REJECT：

```text
target Application FOR UPDATE
```

APPROVE：

```text
Animal
→ target Application
→ other PENDING Applications id ASC
```

如果审批时：

```text
Application 已非 PENDING
→ 40901 BUSINESS_STATE_CONFLICT

Animal 已非 AVAILABLE
→ 40901 BUSINESS_STATE_CONFLICT
```

target.user_id = 当前 ADMIN.id：

```text
40301 FORBIDDEN
```

且不写 OperationLog。

不存在或 ADMIN 无权访问：

```text
40401 RESOURCE_NOT_FOUND
```

---

## 2.10 查看本人领养记录

```http
GET /api/adoption-records/me?page=1&size=20
```

权限：

```text
USER / RESCUER
```

固定：

```text
user_id = 当前用户
```

排序：

```text
adopted_at DESC, id DESC
```

返回：

```text
ApiResponse<PageResponse<AdoptionRecordResponse>>
```

每条记录包含授权后的 `animal` 展示摘要，因此即使 Animal 已为 `ADOPTED`，领养人仍可在自己的领养记录中展示动物基本信息。

---

## 2.11 查看本人领养记录详情

```http
GET /api/adoption-records/{recordId}
```

权限：

```text
领养人本人
ADMIN
```

不可见返回：

```text
40401 RESOURCE_NOT_FOUND
```

返回：

```text
ApiResponse<AdoptionRecordResponse>
```

AdoptionRecord 为不可变事实记录：

```text
不提供 PATCH
不提供 DELETE
```

---

## 2.12 RESCUER 查看负责动物的领养情况

```http
GET /api/animals/{animalId}/adoption-overview
```

权限：

```text
RESCUER
```

前置：

```text
Animal.rescue_task_id
→ RescueTask.rescuer_id = 当前用户
```

返回只读摘要，不暴露申请人的全部敏感申请内容：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "animalId": "5001",
    "animalStatus": "AVAILABLE",
    "pendingApplicationCount": 3,
    "approvedApplicationId": null,
    "adoptionRecordId": null,
    "adoptedAt": null
  }
}
```

若 Animal 已 ADOPTED：

```json
{
  "animalId": "5001",
  "animalStatus": "ADOPTED",
  "pendingApplicationCount": 0,
  "approvedApplicationId": "6001",
  "adoptionRecordId": "7001",
  "adoptedAt": "2026-08-29T15:00:00+08:00"
}
```

不返回：

```text
其他申请人的 adoptionReason
housingCondition
familyMembers
petExperience
contact
```

---

# 3. M04 接口汇总

| 编号 | Method | Path | 功能 | 权限 | 成功 HTTP |
|---|---|---|---|---|---:|
| M04-API-01 | GET | `/api/animals` | 公开 AVAILABLE 动物列表 | permitAll | 200 |
| M04-API-02 | GET | `/api/animals/{id}` | 查看动物详情 | 按 Animal 可见性 | 200 |
| M04-API-03 | GET | `/api/animals/responsible/me` | 查看本人负责动物 | RESCUER | 200 |
| M04-API-04 | PATCH | `/api/animals/{id}` | 修改动物基本信息 | 负责 RESCUER / ADMIN | 200 |
| M04-API-05 | POST | `/api/animals/{id}/images` | 添加动物图片 | 负责 RESCUER / ADMIN | 201 |
| M04-API-06 | DELETE | `/api/animals/{id}/images/{imageId}` | 删除动物图片 | 负责 RESCUER / ADMIN | 200 |
| COMMON-MEDIA-02 | GET | `/api/media/animal-images/{imageId}` | 读取动物图片 | 复用 Animal 可见性 | 200 |
| M04-API-07 | POST | `/api/animals/{id}/health-records` | 添加健康记录 | 负责 RESCUER / ADMIN | 201 |
| M04-API-08 | GET | `/api/animals/{id}/health-records` | 查看健康记录 | 按 Animal 可见性 | 200 |
| M04-API-09 | POST | `/api/animals/{id}/status-actions` | 动物状态动作 | 负责 RESCUER / ADMIN | 200 |

---

# 4. M05 接口汇总

| 编号 | Method | Path | 功能 | 权限 | 成功 HTTP |
|---|---|---|---|---|---:|
| M05-API-01 | POST | `/api/animals/{id}/adoption-applications` | 提交领养申请 | USER/RESCUER | 201 |
| M05-API-02 | GET | `/api/adoption-applications/me` | 查看本人申请 | USER/RESCUER | 200 |
| M05-API-03 | GET | `/api/adoption-applications/{id}` | 查看申请详情 | 申请人本人/ADMIN | 200 |
| M05-API-04 | POST | `/api/adoption-applications/{id}/withdraw` | 撤回本人 PENDING 申请 | USER/RESCUER本人 | 200 |
| M05-API-05 | GET | `/api/admin/adoption-applications` | ADMIN 查看审核队列 | ADMIN | 200 |
| M05-API-06 | POST | `/api/admin/adoption-applications/{id}/audit` | ADMIN 审核申请 | ADMIN | 200 |
| M05-API-07 | GET | `/api/adoption-records/me` | 查看本人领养记录 | USER/RESCUER | 200 |
| M05-API-08 | GET | `/api/adoption-records/{id}` | 查看领养记录详情 | 领养人本人/ADMIN | 200 |
| M05-API-09 | GET | `/api/animals/{id}/adoption-overview` | 查看负责动物领养情况 | 负责 RESCUER | 200 |

---

# 5. M04/M05 新增错误码建议

```text
40907 ADOPTION_APPLICATION_ALREADY_EXISTS
```

其余继续使用：

```text
40001 INVALID_PARAMETER
40301 FORBIDDEN
40401 RESOURCE_NOT_FOUND
40901 BUSINESS_STATE_CONFLICT
40903 OPTIMISTIC_LOCK_CONFLICT
40004 INVALID_FILE
41301 FILE_TOO_LARGE
50001 INTERNAL_ERROR
```

---

# 6. M04/M05 API V1.0 最终冻结结论

本轮没有修改 Frozen 数据库结构。

最终固定：

```text
1. Animal 状态动作统一使用：
   POST /api/animals/{id}/status-actions

2. 状态动作字段矩阵固定：
   version 所有动作必填且为非负整数；
   仅 SUSPEND_ADOPTION 允许并要求 suspendReason；
   其他动作出现 suspendReason 一律 40001。

3. HealthRecord 不自动同步 Animal.health_condition。
   当前健康摘要需要通过 PATCH Animal 单独维护。

4. 健康记录采用权限投影：
   公开/普通访问 → HealthRecordPublicResponse
   负责 RESCUER / ADMIN → HealthRecordResponse

5. Animal 图片单次新增 1～9 张；
   生命周期累计图片数量不限制为 9。

6. 提交领养申请时：
   SELECT Animal FOR UPDATE
   不存在 → 40401
   存在但 status != AVAILABLE → 40901
   AVAILABLE → 创建申请。

7. ADMIN 审核申请必须主动校验：
   target.user_id != 当前 ADMIN.id
   相等 → 40301，回滚，不写 OperationLog。

8. ADMIN APPROVE 锁顺序：
   Animal
   → target Application
   → other same-animal PENDING Applications by id ASC

9. ADMIN REJECT：
   仅锁 target Application，不锁 Animal。

10. 同一审核接口固定返回：
    ApiResponse<AdoptionAuditResponse>
    REJECT → adoptionRecord = null
    APPROVE → adoptionRecord = 新建记录。

11. AdoptionRecordResponse 固定包含 animal 展示摘要：
    id / name / species / sex / coverImageUrl
    仅用于已授权领养记录场景，不重新公开 ADOPTED Animal。

12. RESCUER 负责动物领养情况使用：
    GET /api/animals/{animalId}/adoption-overview
    仅返回只读摘要，不暴露其他申请人的敏感申请内容。

13. AnimalImage 媒体读取额外允许最终领养人：
    存在 AdoptionRecord.animal_id = Animal.id
    AND AdoptionRecord.user_id = 当前用户.id。

14. 历史申请人若不是最终领养人，且 Animal 已不可见：
    AdoptionApplicationDetailResponse.animal.coverImageUrl = null。
    不返回必然 404 的媒体地址。
```

> **✅ M04 动物档案 API V1.0 — Frozen**  
> **✅ M05 领养管理 API V1.0 — Frozen**
