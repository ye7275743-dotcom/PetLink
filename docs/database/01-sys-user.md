# 01 `sys_user` 系统用户表

## 1. 表用途

`sys_user` 保存 PetLink 用户登录凭证、个人基础资料、当前单一角色及账号启用状态。Java 实体仍命名为 `User`，通过 MyBatis-Plus 映射：

```java
@TableName("sys_user")
public class User {
    // fields
}
```

V1.0 不建立角色关联表，不删除用户记录。

## 2. 数据字典

| 字段 | 类型 | NULL | 默认值 | 约束 | 说明 |
|---|---|:---:|---|---|---|
| `id` | `BIGINT UNSIGNED` | 否 | `AUTO_INCREMENT` | PK | 用户 ID |
| `account` | `VARCHAR(50)`, ASCII CI | 否 | — | UNIQUE, CHECK | 登录账号，大小写不敏感，创建后不可修改 |
| `password_hash` | `VARCHAR(100)`, ASCII BIN | 否 | — | — | BCrypt 密码哈希，不保存明文 |
| `nickname` | `VARCHAR(50)` | 否 | — | CHECK | 用户昵称；注册时未填写则由 Service 设置为账号原始展示值 |
| `phone` | `VARCHAR(20)`, ASCII BIN | 是 | `NULL` | CHECK | 联系手机号，不作为登录账号且不要求唯一 |
| `role_code` | `VARCHAR(20)`, ASCII BIN | 否 | `USER` | CHECK | `USER / RESCUER / ADMIN` |
| `status` | `VARCHAR(20)`, ASCII BIN | 否 | `ENABLED` | CHECK | `ENABLED / DISABLED` |
| `created_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | — | 创建时间 |
| `updated_at` | `DATETIME` | 否 | `CURRENT_TIMESTAMP` | 自动更新 | 最后更新时间 |

## 3. 业务规则

### 3.1 账号

```text
注册时去除首尾空白
格式：^[A-Za-z0-9_]{4,50}$
创建后不可修改
登录和唯一性判断大小写不敏感
不统一转小写保存，保留用户首次输入的展示形式
```

数据库唯一约束：

```sql
CONSTRAINT uk_sys_user_account UNIQUE (account)

CONSTRAINT chk_sys_user_account
CHECK (
    account = TRIM(account)
    AND account REGEXP '^[A-Za-z0-9_]{4,50}$'
)
```

### 3.2 密码

数据库只保存 BCrypt 哈希。原始密码只能在请求处理过程中短暂存在，不得写入数据库、日志或初始化脚本。JWT 不保存到 `sys_user`。

### 3.3 昵称与手机号

```text
nickname 未填写：由注册 Service 设置为 account
nickname 已填写：trim 后校验并保存

phone：trim → 空字符串转 NULL → 非空时校验格式和长度
phone 不设置 UNIQUE，不作为登录账号
```

手机号在非必要页面必须脱敏展示。

### 3.4 角色

```text
公开注册：后端强制 role_code = USER
客户端提交 ADMIN / RESCUER：忽略或拒绝
USER → RESCUER：只能由 ADMIN 执行
```

角色变化不改变历史业务数据所有权。角色与状态变更写入 `operation_log`。

### 3.5 账号状态

```text
ENABLED  → 允许认证和受保护业务操作
DISABLED → 拒绝登录和后续受保护业务操作
```

系统处理受保护请求时必须检查账号当前状态，不能只依赖尚未过期的旧 JWT。

## 4. 初始管理员

公开注册接口不得创建 `ADMIN`。初始管理员由部署初始化脚本直接创建：

```text
部署人员确定管理员账号和初始密码
→ 使用 BCrypt 预生成密码哈希
→ 初始化脚本只写入哈希
→ role_code = ADMIN
→ status = ENABLED
```

可执行 SQL 中不得保留明文密码，也不得直接执行未替换的占位哈希。初始化模板应保持注释状态，部署时替换为真实 BCrypt 哈希后再执行。

## 5. 索引与约束

```sql
PRIMARY KEY (id)

CONSTRAINT uk_sys_user_account
UNIQUE (account)

CONSTRAINT chk_sys_user_account
CHECK (
    account = TRIM(account)
    AND account REGEXP '^[A-Za-z0-9_]{4,50}$'
)

CONSTRAINT chk_sys_user_nickname
CHECK (CHAR_LENGTH(TRIM(nickname)) > 0)

CONSTRAINT chk_sys_user_phone
CHECK (
    phone IS NULL
    OR (
        phone = TRIM(phone)
        AND CHAR_LENGTH(phone) > 0
    )
)

CONSTRAINT chk_sys_user_role_code
CHECK (role_code IN ('USER', 'RESCUER', 'ADMIN'))

CONSTRAINT chk_sys_user_status
CHECK (status IN ('ENABLED', 'DISABLED'))

KEY idx_sys_user_role_status (role_code, status)
```

> **`sys_user` 表 V1.0 — Frozen**
