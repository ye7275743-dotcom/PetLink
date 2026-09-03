# PetLink

PetLink 是一个面向流浪动物救助与领养的全栈课程设计项目，包含：

- `server/`：Spring Boot 2.7 + Java 17 后端
- `petlink-pc/`：Vue 3 + Vite + Element Plus 电脑管理端（ADMIN / RESCUER）
- `petlink-mobile/`：UniApp + Vue 3 移动端（VISITOR / USER / RESCUER）
- `sql/`：数据库结构、初始化与校验脚本
- `docs/`、`uml/`、`prototypes/`：设计说明、接口文档、测试证据和原型

## 本地运行

需要 JDK 17、MySQL 8.0.16+、Node.js 18+ 和 Python 3。

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

## 生产部署

生产 Compose 配置位于 `deploy/petlink-production/`。当前示例部署使用：

- PC：`http://103.233.254.186:8081/`
- Mobile H5：`http://103.233.254.186:8081/mobile/`

真实 `.env`、数据库密码、JWT 密钥、上传数据和构建产物不进入仓库；请根据部署 README 在目标环境中单独配置。

## 业务主链

普通用户发布线索 → 管理员审核 → 救助人员接取并完成救助 → 建立动物档案 → 开放领养 → 用户提交申请 → 管理员审核 → 用户回访。

> 本仓库为公开课程项目仓库。演示账号仅用于本地/演示环境，正式环境请立即更换密码和密钥。
