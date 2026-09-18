# PetLink

PetLink 是一个面向流浪动物救助与领养的全栈课程设计项目，包含：

- `server/`：Spring Boot 2.7 + Java 17 后端
- `petlink-pc/`：Vue 3 + Vite + Element Plus 电脑管理端（ADMIN / RESCUER）
- `petlink-mobile/`：UniApp + Vue 3 移动端（VISITOR / USER / RESCUER）
- `sql/`：数据库结构、初始化与校验脚本
- `docs/`、`uml/`、`prototypes/`：设计说明、接口文档、测试证据和原型

## 本地运行

需要 JDK 17、MySQL 8.0.16+、Node.js 18+ 和 Python 3。

全新数据库直接执行 `sql/petlink.sql`。已有 PetLink 数据库升级到本版本时，先备份，再执行 `sql/migrate-existing-to-20260912.sql`；该脚本会按需补齐用户逻辑删除字段、索引以及动物性格和领养要求字段，可重复执行且不会覆盖现有数据。

```bash
cd server
./mvnw test
./mvnw spring-boot:run
```

```bash
cd petlink-pc
npm ci && npm run dev
```

```bash
cd petlink-mobile
npm ci && npm run dev:h5
```

电脑端默认端口为 `5173`，移动端 H5 默认端口为 `5174`。完整本地自动化检查见 `scripts/verify-all.sh`。

如需把去标识化的真实来源案例写入本地数据库，请先确保 `admin`、`rescuer`、`user` 三个演示账号使用同一演示密码并已启动后端，再在项目根目录设置 `PETLINK_REAL_CASE_PASSWORD` 后执行 `node scripts/seed-real-cases.mjs`。脚本通过正式 REST API 写入待审核、待接取、进行中、已完成、可领养、已领养和回访数据，图片来源与使用边界见 `docs/content/PHOTO-ASSET-MAP.md`。旧的 `seed-demo-data.mjs` 和 `seed-demo-task-states.mjs` 仅保留作历史回归参考，不应再向线上库追加数据。

如需继续增加第二批真实来源领养档案，可执行 `node scripts/seed-real-cases-batch2.mjs`；脚本会按动物名称跳过已存在档案，避免重复创建。

如果需要在全新环境中一次性还原当前课程案例，可按 [`docs/reproducibility/README.md`](docs/reproducibility/README.md) 的顺序初始化数据库、启动后端并执行 `node scripts/seed-course-fixture.mjs`。该流程会生成与当前版本一致的业务状态和案例图片，但不会把生产环境凭据、上传目录或真实个人信息带入仓库。

## 生产部署

生产 Compose 配置位于 `deploy/petlink-production/`。当前示例部署使用：

- PC：`http://103.233.254.186:8081/`
- Mobile H5：`http://103.233.254.186:8081/mobile/`

真实 `.env`、数据库密码、JWT 密钥、上传数据和构建产物不进入仓库；请根据部署 README 在目标环境中单独配置。

## 业务主链

普通用户发布线索 → 管理员审核 → 救助人员接取并完成救助 → 建立动物档案 → 开放领养 → 用户提交申请 → 管理员审核 → 用户回访。

> 本仓库为公开课程项目仓库。演示账号仅用于本地/演示环境，正式环境请立即更换密码和密钥。
