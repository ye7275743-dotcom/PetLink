# PetLink V1.0 跨平台三角色闭环验收记录

执行日期：2026-09-03  
验收范围：PC 管理端（5173）、Mobile H5 端（5174）、Spring Boot API（8080）、MySQL 8.0.46  
验收目标：验证普通用户、管理员、救助人员在两个前端之间共享同一业务数据，并覆盖主流程、权限边界、幂等和即时状态一致性。

## 1. 环境与执行方式

| 项目 | 实际值 |
|---|---|
| 后端 | `http://127.0.0.1:8080`，健康状态 `UP` |
| PC 端 | `http://127.0.0.1:5173` |
| Mobile H5 | `http://127.0.0.1:5174` |
| 数据库 | MySQL `8.0.46`，Docker 容器 `petlink-mysql-m06m07` |
| JDK | Corretto `17.0.20.1` |
| 执行脚本 | `scripts/cross-platform-e2e.mjs` |

脚本通过两个前端开发代理发起请求，分别保存 USER、ADMIN、RESCUER 的独立令牌；每次使用唯一前缀创建数据，不删除既有项目数据。执行命令：

```bash
PETLINK_E2E_OPERATOR_PASSWORD='（本机演示密码）' node scripts/cross-platform-e2e.mjs
```

密码只通过环境变量注入，未写入本记录或源码。

## 2. 40 项跨平台检查结果

脚本输出 `CROSS_PLATFORM_E2E_PASSED checks=40`，40/40 全部通过：

| 领域 | 已验证的跨端动作 |
|---|---|
| 认证与身份 | Mobile 注册 USER；USER 令牌跨 PC 查询资料；Mobile Origin/CORS 登录 RESCUER |
| 线索 | Mobile 发布线索；PC 重放幂等请求；PC 审核通过；两端 RESCUER 同时看到待接取 |
| 救助任务 | Mobile 接取；PC 待接取列表同步移除；PC 开始；Mobile 追加过程记录；PC 提交成功 |
| 动物档案 | Mobile 读取完成任务和动物；PC 查看负责动物；PC 推进观察期；Mobile 开放领养 |
| 领养 | Mobile USER 查看动物、收藏、提交申请；PC 审核批准；Mobile 查看领养记录 |
| 回访 | Mobile 提交回访；PC 重放幂等请求；PC RESCUER 查看回访 |
| 公告 | PC 创建并发布；Mobile USER 读取已发布公告 |
| 管理与安全 | PC 查询用户；禁用后 Mobile 令牌立即 403；启用后恢复 200；USER 越权访问 ADMIN/RESCUER 接口均 403；PC 统计覆盖业务状态 |

本次脚本生成的关联业务数据：

```text
user=e2emtkftozjn65ye
clue=20
task=17
animal=21
application=14
adoption record=10
follow-up=12
announcement=6
```

UI 优化完成后的回归复测同样输出 `CROSS_PLATFORM_E2E_PASSED checks=40`；最近一次新建链路为 `clue=20`、`task=17`、`animal=21`、`application=14`、`adoption record=10`、`follow-up=12`、`announcement=6`，40 项检查全部通过。

## 3. 数据库最终一致性核验

对最近一次关联 ID 执行只读查询，结果如下：

| 实体 | ID | 最终状态/关系 | 结果 |
|---|---:|---|---|
| USER | 80 | `e2emtkftozjn65ye / USER / ENABLED` | 通过 |
| 救助线索 | 20 | `CLOSED`，发布者 80，审核者 1 | 通过 |
| 救助任务 | 17 | `SUCCESS`，线索 20，救助人员 2 | 通过 |
| 动物档案 | 21 | `ADOPTED`，救助任务 17，版本 3 | 通过 |
| 领养申请 | 14 | `APPROVED`，动物 21，申请人 80 | 通过 |
| 领养记录 | 10 | 申请 14、动物 21、用户 80 | 通过 |
| 回访记录 | 12 | 领养记录 10，提交人 80，幂等键已落库 | 通过 |
| 公告 | 6 | `PUBLISHED`，版本 1，发布时间已写入 | 通过 |

同时执行关联链查询：`rescue_clue(20) → rescue_task(17) → animal(21) → adoption_application(14) → adoption_record(10) → follow_up_record(12)`，返回单条完整链路，无断链或重复记录。

## 4. 前端现场复核

- Mobile 救助任务页重载后显示“等待接取”和“我的任务”，列表包含中文状态、详情与接取操作；当前已完成任务显示任务 17/线索 20。
- Mobile 控制台无 error；仅保留 UniApp 依赖的既有 Vue Router 弃用 warning，不影响运行。
- PC 救助任务页重载后显示任务 17/线索 20，状态为“救助成功”，控制台无 error。
- 手机端任务页的 `onShow` 已改为兼容同步/异步返回值的安全刷新写法，避免 `undefined.catch` 运行时异常；对应契约测试已纳入 Mobile 18/18。

## 5. 自动化门禁结果

| 门禁 | 结果 |
|---|---|
| 后端 Maven | 269/269，通过；0 失败、错误、跳过 |
| SQL 与 M02～M08 静态检查 | 全部通过（16 张表、52 CHECK、26 FK、17 UNIQUE） |
| PC 测试 | 20/20，通过 |
| PC 生产构建 | `vite build` 通过 |
| Mobile 测试 | 18/18，通过 |
| Mobile H5 生产构建 | `uni build` 通过 |
| 前端静态契约与交付包哈希 | 通过 |
| 真实跨平台 API E2E | 40/40，通过 |

## 6. 遗留事项与判定

本轮未发现阻断 M01～M08 业务闭环的代码、接口、权限或数据一致性问题。以下属于发布前环境动作，不构成当前功能验收失败：

- 正式域名、HTTPS、生产 JWT Secret、数据库凭据、文件目录权限和定时清理任务仍需在部署环境复核。
- Chrome/Edge 独立版本、HBuilderX、Android/iOS 真机的相机/相册权限和安全区仍需按现场清单确认。
- PC 构建的 Element Plus 大 chunk 和 UniApp 工具链升级提示属于非阻断优化项。
- 课程答辩 PPT、演示脚本、项目报告和完整彩排按用户要求暂缓。

**验收结论：PetLink V1.0 M01～M08 已达到自动化交付就绪；两个平台、三种角色的核心业务链路闭环通过。**
