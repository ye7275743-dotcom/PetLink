# PetLink 可复现交付说明

本仓库包含当前版本的前端、Spring Boot 后端、数据库结构、固定图片资源、接口脚本和去标识化业务案例。按本文步骤可以在一套全新的本地环境中还原与当前版本一致的页面、角色权限和业务状态。

## 安全边界

仓库不会提交生产环境的 `.env`、数据库密码、JWT 密钥、访问令牌、服务器备份、上传目录或真实个人联系方式。生产库中的主键、日志时间和上传文件路径也不会作为公共快照发布。这样做不会影响课程演示：案例脚本会通过正式 REST API 重新创建相同的待审核、待接取、进行中、可领养、已领养、拒绝和回访状态。

## 全新环境恢复顺序

1. 安装 JDK 17、Node.js 18+、MySQL 8.0.16+。
2. 初始化数据库结构：

   ```bash
   mysql -uroot -p < sql/petlink.sql
   ```

3. 写入三个本地演示角色（管理员、救助人员、普通用户）。三个账号的演示密码由使用者自行设置并保持一致：

   ```bash
   mysql -uroot -p petlink < sql/dev-seed-admin.sql
   mysql -uroot -p petlink < sql/dev-seed-rescuer.sql
   mysql -uroot -p petlink < sql/dev-seed-user.sql
   ```

4. 复制 `server/.env.example` 为本机 `server/.env`，填写本机 MySQL 密码和至少 32 字节的 JWT 密钥；启动后端：

   ```bash
   cd server
   ./mvnw spring-boot:run
   ```

5. 分别在两个终端启动 PC 端和 Mobile H5 端：

   ```bash
   # 终端 A
   cd petlink-pc && npm ci && npm run dev

   # 终端 B（从仓库根目录执行）
   cd petlink-mobile && npm ci && npm run dev:h5
   ```

6. 在第三个终端生成与当前版本一致的业务案例。该命令只使用仓库内图片和去标识化字段，不读取生产库：

   ```bash
   # 保持在仓库根目录
   export PETLINK_REAL_CASE_ORIGIN='http://127.0.0.1:8080/api'
   export PETLINK_REAL_CASE_PASSWORD='你为三个本地演示账号设置的密码'
   node scripts/seed-course-fixture.mjs
   ```

   生成内容包括：待审核线索、审核通过后待接取任务、进行中任务、已完成救助、动物健康记录、开放领养、待审核领养申请、已通过领养与回访、已拒绝申请、收藏关系，以及对应的来源链接和连续图片。

## 资源一致性

- PC 和 Mobile 各自包含 36 张同名 JPEG 内容资源。
- `docs/content/PHOTO-ASSET-MAP.md` 记录每组图片的来源链接与文件映射。
- `docs/REAL-CASE-DATA-20260918.md` 记录案例名称、业务状态和复现脚本，不包含服务器密码或生产数据库导出。
- 页面中的来源信息保留在档案字段和来源链接中，不再显示“演示内容说明”等横幅提示。

## 生产部署

生产环境请使用 `deploy/petlink-production/.env.example` 生成服务器专用 `.env`，再按 `deploy/petlink-production/README.md` 构建并启动 Compose。不要把服务器 `.env`、数据库导出或 `data/uploads/` 目录复制回公共仓库。

## 验证

```bash
cd petlink-pc && npm test && npm run build
cd ../petlink-mobile && npm test && PETLINK_H5_BASE=/mobile/ npm run build:h5
```

后端执行 `./mvnw test`。部署后检查 `/health`、PC `/`、PC `/stories` 和 Mobile `/mobile/`，并按 `docs/testing/FINAL-ACCEPTANCE-SUMMARY.md` 完成主链验收。
