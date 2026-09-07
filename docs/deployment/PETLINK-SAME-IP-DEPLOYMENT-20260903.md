# PetLink 同 IP 部署记录（2026-09-03）

## 2026-09-05 UI/UX 产品化升级

- 发布范围：PC 公众站与工作台、Mobile H5、Spring Boot 后端、`sys_user/announcement` 逻辑删除迁移及索引。
- 发布前备份：`/srv/petlink-backups/20260904-174228-ui-ux-fixes`（服务器 UTC 时间），包含数据库 dump、运行文件、上传媒体、SHA-256 清单和发布前容器镜像标签。
- 迁移：`V20260904_02__ui_management_deletion.sql`；线上已确认两列默认值为 0，两个复合索引存在。
- 容器：仅重建 `petlink-backend` 与 `petlink-web`；`petlink-mysql` 数据卷保留，原项目 80 端口未停止或修改。
- 发布后状态：`/health` = `UP`，PC `/` = 200，Mobile `/mobile/` = 200，原项目 `/` = 200。MySQL 与 PetLink Backend 均未对公网直接暴露。
- 线上功能验收：三角色登录和越权 403 正常；用户/公告删除及搜索 15 项通过；各管理队列筛选通过；回访列表连续 5 次平均响应约 64 ms。
- 展示数据文案已去除“演示批次/演示任务/示例”工程标记，业务状态、图片和历史关联不变。
- 发布产物 SHA-256：Backend `0009d90adce4ff4dd0b6656526569296342ba17fb18cfc866bc5b122b3dcfdab`；PC index `ef09b5c386ca8345dcd8f56b7fcfe803998ece71cac1e5640f550b1555c730e5`；Mobile index `bc666ca2d31ff1fa4ef16f5fd2b184686da7bb2cdc34b7cba4d17749ef76f8d1`。

## 部署拓扑

服务器：`103.233.254.186`（Ubuntu 22.04 LTS，x86_64）

现有项目继续由原 Docker 栈占用 `80/TCP`。PetLink 使用独立 Compose 栈，不停止、不重建原有容器：

| 入口 | 用途 | 状态 |
|---|---|---|
| `http://103.233.254.186/` | 现有项目 | 保持运行 |
| `http://103.233.254.186:8081/` | PetLink PC 工作台 | 容器已运行 |
| `http://103.233.254.186:8081/mobile/` | PetLink Mobile H5 | 容器已运行 |
| `http://103.233.254.186:8081/api/` | PetLink API 反向代理 | 容器已运行 |

PetLink 后端仅在 Compose 网络内部监听 `8080`，MySQL 使用独立 Docker 持久化卷；上传文件位于 `/srv/petlink/data/uploads`。

## 服务器文件

- Compose 项目：`/srv/petlink`
- 生产环境变量：`/srv/petlink/.env`（权限 600，不进入版本库）
- 初始演示账号：`/root/petlink-initial-credentials.txt`（权限 600）
- 数据库卷：`petlink_petlink_mysql_data`

## 已完成验收

```text
MySQL 8.0.36 container        healthy
Spring Boot / Java 17         started, /health = UP
PC and Mobile static assets   HTTP 200 inside server
ADMIN login + stats API       200
RESCUER login + waiting API   200
USER public API               200
USER admin API                403
Original project on :80       still running
```

## 云侧网络状态

云厂商安全组已放行 PetLink 入站规则：

```text
Protocol: TCP
Port: 8081
Source: 以云控制台当前安全组记录为准
```

已从公网验证 PC、Mobile、三角色登录、API 和 `/health`。不放行 MySQL `3306` 或 Backend 内部 `8080`。

## 常用运维命令

```bash
cd /srv/petlink
docker compose ps
docker compose logs --tail=100 backend
docker compose restart backend web
docker compose pull
```

不要执行 `docker compose down -v`，否则会删除 PetLink MySQL 数据卷。
