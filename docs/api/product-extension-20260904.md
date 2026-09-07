# 2026-09-04 产品化扩展（原 Frozen 基线不改写）

## 1. 角色调整 API

新增 `POST /api/admin/users/{userId}/role`，要求 ADMIN。请求：

```json
{"roleCode":"RESCUER","reason":"已完成救助资格核验"}
```

- `roleCode` 只接受 USER、RESCUER；`reason` 去除首尾空格后 1～500 字。
- 成功响应沿用 AdminUserActionResponse（id、roleCode、status、updatedAt），HTTP 200。
- 非管理员、试图更改管理员账号/自身角色：403；无效输入：400；账号不存在：404；目标角色相同或仍有活动任务：409。
- 降级不改历史归属，管理员可继续维护动物档案；WAITING_START、IN_PROGRESS 任务必须先完成或依法定业务处置后才能降级。
- 不提供任命管理员的入口。原单向 `/promote-rescuer` 保留兼容，本次界面使用新接口并填写原因。

## 2. 事务与数据

无 DDL 迁移。新增操作日志类型 `SYS_USER / DEMOTE_RESCUER`；新角色接口的升降级都写入前后角色、操作者及原因，与角色更新同一事务提交。

锁顺序扩展：接取任务先锁 `sys_user` 中 ENABLED RESCUER 行，再进入原线索/任务写入；角色调整先锁同一账户行，再检查活动任务、更新角色并写日志。避免“降级检查通过”与“新接取”同时成功。真实 MySQL 并发验收覆盖两个不同获胜方向。

注册仍使用原 `/auth/register`，只能建立普通用户，拒绝未知的角色/状态字段。注册时间由应用显式按 Asia/Shanghai 写入，不再依赖数据库宿主机默认时区；不批量改写历史时间。

## 3. 会话与前端路由

- PC 缓存：`petlink_pc_token`、`petlink_pc_user`；Mobile：`petlink_mobile_token`、`petlink_mobile_user`。
- 旧通用缓存存在格式歧义，不自动迁移，升级后需重新登录一次。
- PC 普通用户登录成功后跳到 `/mobile/` 公众服务；仅在同源环境进行显式会话交接，不把 token 放在 URL。
- 两端业务身份以服务端实时查询为准。页面进入/应用恢复时刷新身份，401 或禁用账号响应清理对应端会话。
- PC 新增公开 `/`、`/adopt`、`/adopt/:id`、`/news`、`/news/:id`、`/guide`、`/register`；原后台路由保持兼容。
- 公开首页只展示 AVAILABLE 动物与已发布公告；详情入口同样检查可领养状态，不为填充卡片放宽公开范围。
- Mobile 未登录访问受保护页面时记录目标页面，登录后继续原操作。

## 4. 统计

`GET /api/admin/stats/trends?from=YYYY-MM-DD&to=YYYY-MM-DD&granularity=DAY` 仍用原契约，增加最多 366 个自然日限制。页面展示全部线索/任务/动物/申请状态分布、可选日期范围、图例和真实数值，零值不再画成非零柱。救助人员首页使用接口 total，不把第一页记录数当总数。

## 5. 需求追踪增量

| 增量项 | 原计划关系 | 实现 | 验证 |
|---|---|---|---|
| EXT-ACCOUNT-01 PC 注册及用户管理关联 | 补齐 PC 入口 | RegisterView、原注册接口、UsersView | PC 实际注册、管理员搜索；线上专用账号验收 |
| EXT-ACCOUNT-02 USER/RESCUER 双向调整 | 降级为本次新需求 | 新 role API、审计、任务阻断 | 8 项新增单测、真实数据库权限及并发测试、实际弹窗操作 |
| EXT-SESSION-01 同源身份隔离 | 修复部署缺陷 | 独立缓存、身份刷新、显式普通用户交接 | 同源正式构建 PC 管理员与 Mobile 用户并行，PC 退出独立 |
| EXT-HOME-01 公众首页及角色入口 | 完善产品入口 | PublicView、Mobile 首页、角色工作台 | 无登录浏览、注册引导、手机登录后回原页面 |
| FR-STAT-01～04 / RTM-M08-05 呈现 | 补齐已有统计接口的页面展示 | DashboardView、真实计数、时间范围 | 单测及管理员真实数据界面核对 |
| NFR-COMPAT 兼容 | 原计划未完整验收项 | 本轮构建与内置 Chromium 验证 | 独立 Chrome、Edge、HBuilderX、真机仍未签字 |

原 Frozen API 72 个，本次增加 1 个，合计 73 个；历史冻结包及其哈希保留，不能把历史包哈希通过解释为新版归档已更新。

## 6. UI/UX 收口扩展

- 新增 `DELETE /api/admin/users/{userId}`：仅 ADMIN，逻辑删除非管理员账号；目标存在活动救助任务时返回 409，历史业务与审计日志保留。
- 新增 `DELETE /api/admin/announcements/{announcementId}?version={version}`：仅 ADMIN，使用乐观锁版本并逻辑删除；删除后公开和后台查询均不可见。
- 线索审核列表增加 `keyword`（地点、动物描述、现场情况、联系方式）服务端检索。
- 领养申请管理列表增加 `animalId` 与 `userId` 服务端筛选。救助任务、动物档案、领养记录与回访列表同步暴露已有的服务端筛选参数。
- PC 受保护路由守卫对身份请求进行同 token 合并与 15 秒短缓存；后端业务 API 仍每次校验账号实时状态。回访列表图片由逐条查询改为分页内批量查询。

本次扩展后 API 总数为 75 个（历史 Frozen 72 + 角色调整 1 + 用户删除 1 + 公告删除 1）。
