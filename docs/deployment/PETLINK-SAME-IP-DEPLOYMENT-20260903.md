# PetLink 同 IP 部署记录（2026-09-03）

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

## 待完成的云侧动作

云厂商安全组需要放行一条入站规则：

```text
Protocol: TCP
Port: 8081
Source: your office/public IP (or 0.0.0.0/0 for temporary demo)
```

服务器 UFW 当前未启用，外部 `8081` 超时已确认来自云侧安全组/上游防火墙。放行后再从公网验证 PC、Mobile、登录和 `/health`。

## 常用运维命令

```bash
cd /srv/petlink
docker compose ps
docker compose logs --tail=100 backend
docker compose restart backend web
docker compose pull
```

不要执行 `docker compose down -v`，否则会删除 PetLink MySQL 数据卷。
