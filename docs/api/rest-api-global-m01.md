# PetLink REST API 设计 V1.0（全局规范 + M01 用户与认证）

> 状态：Frozen（最终同步版）  
> Base URL：`/api`

---

## 1. 全局 API 规范

### 1.1 通信与数据格式

- 协议：HTTPS
- 风格：REST
- JSON：`application/json; charset=UTF-8`
- 文件上传：`multipart/form-data`
- 认证头：

```http
Authorization: Bearer <JWT>
```

公开接口由 Spring Security 配置 `permitAll`；其余接口默认需要 JWT。

---

### 1.2 统一响应结构

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败：

```json
{
  "code": 40001,
  "message": "参数校验失败",
  "data": null
}
```

分页：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "page": 1,
    "size": 20,
    "total": 0,
    "pages": 0
  }
}
```

注意：

- 业务主键 ID 返回字符串；
- `page / size / total / pages` 保持数字。

---

### 1.3 HTTP 状态码与业务错误码

不能所有请求都返回 HTTP 200。

| 业务码 | HTTP 状态 |
|---|---:|
| `0` 查询/更新/动作成功 | 200 |
| `0` 创建资源成功 | 201 |
| `400xx` | 400 |
| `401xx` | 401 |
| `403xx` | 403 |
| `404xx` | 404 |
| `409xx` | 409 |
| `41301` | 413 |
| `500xx` | 500 |

例如：

```text
POST /api/auth/register
→ 201 Created

POST /api/files/temporary
→ 201 Created
```

---

### 1.4 HTTP 方法约定

| 方法 | 含义 |
|---|---|
| `GET` | 查询资源 |
| `POST` | 创建资源或执行显式业务动作 |
| `PUT` | 整体更新 |
| `PATCH` | 局部更新 |
| `DELETE` | 删除/取消关系类资源 |

状态迁移、审核、接取、发布、撤回等业务动作使用 `POST` 子资源形式。

禁止：

```http
PATCH /api/rescue-tasks/123

{
  "status": "SUCCESS"
}
```

必须：

```http
POST /api/rescue-tasks/123/result
```

---

### 1.5 BIGINT ID 传输规范

数据库：

```text
BIGINT UNSIGNED
```

Java：

```text
Long
```

JSON：

```json
{
  "userId": "1001",
  "animalId": "2001"
}
```

统一序列化为字符串，避免 JavaScript `Number` 超出安全整数范围。

请求路径中的 ID 也按十进制字符串处理，例如：

```http
GET /api/animals/2001
```

客户端不得自行生成数据库业务主键。

`publisherId / reviewerId / rescuerId / submitterId / operatorId` 等身份字段原则上从当前认证用户推导，禁止客户端越权指定。

---

### 1.6 时间格式与时区

所有 API 时间统一使用带时区的 ISO 8601：

```text
yyyy-MM-dd'T'HH:mm:ssXXX
```

示例：

```text
2026-08-29T10:30:00+08:00
```

关键业务时间由服务端或数据库产生，客户端不得伪造：

```text
created_at
updated_at
reviewed_at
started_at
finished_at
published_at
bound_at
adopted_at
```

---

### 1.7 分页规范

查询参数：

```text
page=1
size=20
```

规则：

- `page >= 1`
- `1 <= size <= 100`
- 默认 `page=1`
- 默认 `size=20`
- 排序字段使用后端白名单，禁止直接拼接客户端 SQL 字段。

---

### 1.8 权限模型

逻辑访问角色：

```text
VISITOR
USER
RESCUER
ADMIN
```

其中：

```text
VISITOR
```

不是数据库角色，只表示未登录访问者。

数据库：

```text
sys_user.role_code
= USER / RESCUER / ADMIN
```

权限由：

```text
Spring Security
+ JWT Filter
+ RBAC
+ Service 业务归属校验
```

共同保证。

---

### 1.9 JWT 有效用户与角色同步规则

JWT 不能作为用户当前状态和当前角色的永久真相。

每次受保护请求必须确认：

```text
当前 user 仍存在
AND status = ENABLED
```

并使用当前有效角色进行授权。

因此：

```text
ADMIN 禁用用户
→ 用户旧 JWT 立即失去受保护接口访问权限

USER → RESCUER
→ 后续请求按当前 RESCUER 权限判断
```

实现上可通过 JWT 中的 `userId` 加载当前用户状态和当前 `role_code`。

JWT 登录响应：

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "expiresAt": "2026-08-29T12:30:00+08:00"
}
```

其中 `expiresIn` 单位为秒。

---

### 1.10 登录账号与密码规范化规则

登录 `account`：

```text
先 trim
→ 按大小写不敏感方式查询
```

`password`：

```text
作为原始字符串处理
不得 trim
不得自动改变大小写
不得进行 Unicode 规范化
```

注册和登录均在 BCrypt 前校验：

```text
8～64 个字符
且 UTF-8 编码后 ≤ 72 bytes
```

这样可以保证注册与登录对同一密码字符串采用完全一致的处理方式。

---

### 1.15 状态动作统一并发规则

普通单表状态转换采用条件更新：

```sql
UPDATE ...
SET status = ?
WHERE id = ?
  AND status = ?;
```

判断：

```text
affectedRows = 1
→ 成功

affectedRows = 0
→ 重新查询
   ├─ 数据不存在或对当前用户不可见 → 40401 RESOURCE_NOT_FOUND
   └─ 数据存在但状态已变化         → 40901 BUSINESS_STATE_CONFLICT
```

复杂跨表事务：

```text
严格按照已冻结锁顺序
SELECT ... FOR UPDATE
```

例如：

```text
领养批准：
Animal
→ target AdoptionApplication
→ other PENDING applications by id ASC
```

禁止自行改变已冻结的锁顺序。

---

### 1.15 乐观锁规则

带 `version` 的资源：

```text
Animal
Announcement
```

更新请求必须携带当前版本：

```json
{
  "version": 3
}
```

更新：

```text
WHERE id = ?
  AND version = ?

SET version = version + 1
```

`affectedRows = 0` 时必须重新查询：

```text
affectedRows = 0
→ 重新查询
   ├─ 资源不存在或对当前用户不可见 → 40401 RESOURCE_NOT_FOUND
   └─ 资源存在但 version 不一致    → 40903 OPTIMISTIC_LOCK_CONFLICT
```

---

### 1.15 通用错误码

```text
40001 INVALID_PARAMETER
40002 INVALID_ACCOUNT_FORMAT
40003 INVALID_PASSWORD_FORMAT
40004 INVALID_FILE

40101 UNAUTHORIZED
40102 TOKEN_EXPIRED
40103 INVALID_CREDENTIALS

40301 FORBIDDEN
40302 ACCOUNT_DISABLED

40401 RESOURCE_NOT_FOUND

40901 BUSINESS_STATE_CONFLICT
40902 DUPLICATE_OPERATION
40903 OPTIMISTIC_LOCK_CONFLICT
40904 ACCOUNT_ALREADY_EXISTS

41301 FILE_TOO_LARGE

50001 INTERNAL_ERROR
```

不向客户端返回数据库异常、SQL、堆栈或内部文件路径。

---

### 1.15 回访持久化幂等规则

`FollowUpRecord` 使用持久化幂等键：

```text
idempotencyKey
```

规则：

- 标准 UUID；
- 服务端校验并转换为小写；
- 永久保存在 `follow_up_record`；
- `(submitter_id, idempotency_key)` 唯一；
- 同用户 + 同 key + 同 `adoptionRecordId`：返回第一次提交结果；
- 同用户 + 同 key + 不同 `adoptionRecordId`：409 冲突。

---

### 1.15 UC-01 发布线索短期重复提交控制

数据库不新增字段。

采用：

```text
前端提交按钮禁用
+
服务端短期幂等令牌
```

短期幂等令牌的生成、有效期、消费方式在 M02 API 设计中正式定义。

它只解决短时间内重复点击/重复请求，不替代正常业务唯一性规则。

---

## 2. 通用临时文件 API

文件上传属于公共技术服务，不作为 M01～M08 一级业务模块。

### 2.1 上传临时图片

```http
POST /api/files/temporary
```

权限：

```text
USER / RESCUER / ADMIN
```

请求：

```text
Content-Type: multipart/form-data
file=<image>
```

规则：

- 每次请求只允许上传一个 `file`；
- `owner_id` 必须来自当前 JWT，客户端不得提交；
- JPG / JPEG / PNG；
- 单张 `<= 5MB`；
- 服务端校验扩展名、MIME、文件头和实际大小；
- 生成标准小写 UUID token；
- 默认 TTL：`PT1H`。

成功：

```http
HTTP/1.1 201 Created
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "expiresAt": "2026-08-29T10:30:00+08:00"
  }
}
```

客户端永远不接触：

```text
tempPath
formalPath
```

---

## 3. M01 用户与认证 API

M01 只负责：

```text
注册
登录
查看本人资料
修改本人资料
```

以下属于 M08：

```text
用户启用 / 禁用
USER → RESCUER
用户管理
```

---

### 3.1 用户注册

```http
POST /api/auth/register
```

权限：

```text
permitAll
```

请求：

```json
{
  "account": "petlink_user01",
  "password": "Example123!",
  "nickname": "小明",
  "phone": "13800138000"
}
```

规则：

#### account

```text
trim
^[A-Za-z0-9_]{4,50}$
大小写不敏感唯一
创建后不可修改
```

#### password

V1.0 API 规则：

```text
8～64 个字符
UTF-8 编码后不得超过 72 bytes
```

原因：后端使用 BCrypt。

密码：

```text
只接收明文请求值
→ 后端 BCrypt
→ 只保存 password_hash
```

#### nickname

```text
未提供 / trim 后为空
→ 使用 account

有效值：
trim 后 1～50 个字符
```

#### phone

PetLink V1.0 采用中国大陆手机号码格式：

```text
^1[3-9]\d{9}$
```

规则：

```text
未提供 / null / trim 后为空
→ NULL

非空：
→ trim
→ 长度不得超过 20
→ 必须匹配中国大陆手机号格式
```

phone：

```text
不作为登录账号
不要求唯一
```

公开注册只能创建：

```text
USER
```

禁止客户端传：

```text
roleCode
status
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
    "userId": "1001",
    "account": "petlink_user01",
    "nickname": "小明",
    "roleCode": "USER"
  }
}
```

主要异常：

```text
40002 INVALID_ACCOUNT_FORMAT
40003 INVALID_PASSWORD_FORMAT
40904 ACCOUNT_ALREADY_EXISTS
```

---

### 3.2 用户登录

```http
POST /api/auth/login
```

权限：

```text
permitAll
```

请求：

```json
{
  "account": "petlink_user01",
  "password": "Example123!"
}
```

流程：

```text
1. account 先 trim，按大小写不敏感方式查询用户
2. password 按原始字符串处理，不 trim、不改大小写、不做 Unicode 规范化
3. 在 BCrypt 前校验密码字符数与 UTF-8 byte 数
4. BCrypt 校验密码
5. 检查 status = ENABLED
6. 读取当前 role_code
7. 生成 JWT
```

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "expiresAt": "2026-08-29T12:30:00+08:00",
    "user": {
      "id": "1001",
      "account": "petlink_user01",
      "nickname": "小明",
      "phone": "13800138000",
      "roleCode": "USER"
    }
  }
}
```

异常：

```text
40103 INVALID_CREDENTIALS
40302 ACCOUNT_DISABLED
```

安全规则：

- 外部不区分“账号不存在”和“密码错误”；
- 不返回 `password_hash`；
- 受保护请求继续检查数据库中的当前账号状态和当前角色。

---

### 3.3 获取当前用户信息

```http
GET /api/users/me
```

权限：

```text
USER / RESCUER / ADMIN
```

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "1001",
    "account": "petlink_user01",
    "nickname": "小明",
    "phone": "13800138000",
    "roleCode": "USER",
    "status": "ENABLED",
    "createdAt": "2026-08-29T09:30:00+08:00",
    "updatedAt": "2026-08-29T09:30:00+08:00"
  }
}
```

---

### 3.4 修改个人资料

```http
PATCH /api/users/me
```

权限：

```text
USER / RESCUER / ADMIN
```

允许字段：

```text
nickname
phone
```

字段语义：

```text
字段未出现
→ 保持原值

phone = null
phone = ""
phone = "   "
→ 清空，保存 NULL

nickname = null
nickname = ""
nickname = "   "
→ 40001 INVALID_PARAMETER

空请求体
→ 40001 INVALID_PARAMETER

出现禁止字段
→ 40001 INVALID_PARAMETER
```

禁止字段包括：

```text
id
account
roleCode
status
password
passwordHash
createdAt
updatedAt
```

禁止字段不是静默忽略，而是直接拒绝请求。

请求示例：

```json
{
  "nickname": "新的昵称",
  "phone": "13900139000"
}
```

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "1001",
    "account": "petlink_user01",
    "nickname": "新的昵称",
    "phone": "13900139000",
    "roleCode": "USER",
    "status": "ENABLED",
    "createdAt": "2026-08-29T09:30:00+08:00",
    "updatedAt": "2026-08-29T09:45:00+08:00"
  }
}
```

---

## 4. M01 接口汇总

| 编号 | Method | Path | 功能 | 权限 | 成功 HTTP |
|---|---|---|---|---|---:|
| M01-API-01 | POST | `/api/auth/register` | 注册 USER | permitAll | 201 |
| M01-API-02 | POST | `/api/auth/login` | 登录并获取 JWT | permitAll | 200 |
| M01-API-03 | GET | `/api/users/me` | 查看本人资料 | USER/RESCUER/ADMIN | 200 |
| M01-API-04 | PATCH | `/api/users/me` | 修改本人资料 | USER/RESCUER/ADMIN | 200 |
| COMMON-API-01 | POST | `/api/files/temporary` | 上传单个临时图片 | USER/RESCUER/ADMIN | 201 |

---

## 5. M08 后续接口占位

后续统一在 M08 设计：

```text
GET  /api/admin/users
GET  /api/admin/users/{id}
POST /api/admin/users/{id}/enable
POST /api/admin/users/{id}/disable
POST /api/admin/users/{id}/promote-rescuer
```

对应正式 OperationLog：

```text
SYS_USER + ENABLE
SYS_USER + DISABLE
SYS_USER + PROMOTE_RESCUER
```

关键日志与对应业务变更同事务提交。
