# PetLink V1.0 本机演示运行手册

## 1. 环境

- JDK 17
- MySQL 8.0.16 或更高版本
- Node.js 18 或更高版本
- Python 3

## 2. 初始化数据库

按顺序执行：

1. `sql/petlink.sql`
2. `sql/dev-seed-admin.sql`
3. `sql/dev-seed-rescuer.sql`
4. `sql/dev-seed-user.sql`

如果不是新建数据库，而是在已有 PetLink 数据库上运行本版本，不要重新导入 `petlink.sql`；先备份，再执行 `sql/migrate-existing-to-20260912.sql`。脚本可重复执行，会兼容补齐 `sys_user.deleted`、对应索引以及动物档案新增字段。

三类种子分别提供 ADMIN、RESCUER、USER 演示账号；脚本采用幂等插入，重复执行不会创建重复账号。种子仅用于本机课程演示。演示密码由现场负责人保管，不写入答辩 PPT、截图或公开提交说明。

## 3. 启动后端

先按 `server/.env.example` 配置环境变量，至少提供数据库密码；生产或联网环境必须覆盖开发用 JWT 密钥。

```bash
cd server
./mvnw spring-boot:run
```

健康检查：`http://localhost:8080/actuator/health`

## 4. 启动前端

电脑工作台：

```bash
cd petlink-pc
npm ci
npm run dev
```

移动端 H5：

```bash
cd petlink-mobile
npm ci
npm run dev:h5
```

默认访问地址：

- 电脑工作台：`http://localhost:5173`
- 移动端 H5：`http://localhost:5174`

## 5. 可选：写入完整演示案例

后端启动后，在项目根目录设置三个演示账号共用的密码并运行：

```bash
PETLINK_DEMO_PASSWORD="你的本地演示密码" node scripts/seed-demo-data.mjs
```

Windows PowerShell：

```powershell
$env:PETLINK_DEMO_PASSWORD="你的本地演示密码"
node scripts/seed-demo-data.mjs
```

脚本会上传同一动物的多张照片，并创建救助、建档、领养、回访和不同状态分支。它只新增带演示批次标记的数据，不覆盖已有记录。

## 6. 建议演示顺序

1. 普通用户注册并发布救助线索。
2. 管理员审核线索。
3. 救助人员接取、开始并记录救助过程。
4. 救助成功后建立动物档案并开放领养。
5. 普通用户收藏动物并提交领养申请。
6. 管理员批准申请并生成领养记录。
7. 普通用户提交回访，救助人员或管理员查看回访。
8. 管理员演示公告、用户管理、监督查询和统计。

## 7. 演示前快速检查

- 后端健康检查为 `UP`。
- 电脑端和移动端均能正常访问 `/api`。
- 浏览器控制台无错误。
- 上传目录可写，图片为 JPG/JPEG/PNG 且单张不超过 5MB。
- 准备重复提交、越权访问、乐观锁冲突和断网场景的口头说明。
