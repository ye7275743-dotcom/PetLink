# PetLink 首页品牌化与性能优化验收记录

验收日期：2026-09-05  
范围：PC 公众首页、PC 按需加载、Spring Boot 列表装配、Nginx 静态交付、生产环境业务闭环。

## 1. 首页产品与视觉验收

- [x] 首页由普通左右分栏升级为沉浸式编辑排版：大幅救助主视觉、叠层渐变、品牌故事浮卡与明确主行动入口。
- [x] 首页增加“发现—审核—救助—建档—领养—回访”六阶段救助链路，首次访问即可理解平台业务闭环。
- [x] 动物与参与方式改为响应式 Bento 卡片，并保留真实动物数据边界，不用虚构救助数量填充页面。
- [x] 待领养动物数与公告数由公开 API 实时返回；接口失败时显示可恢复的中文错误状态。
- [x] 桌面和窄屏布局均完成浏览器视觉检查；移动端简化导航，标题、按钮与图片无溢出和遮挡。
- [x] 图片声明宽高并配置 `loading`、`decoding` 与首屏 `fetchpriority`；动效在 `prefers-reduced-motion` 下自动关闭。

## 2. Image 2 资产

本轮使用 Codex 内置 Image 2 模式生成主视觉，原始结果保留在：

`/Users/a1/.codex/generated_images/01a05daf-41f5-7d32-9438-82232659408c/exec-beceff48-1b57-4a5f-9a39-08045d780c15.png`

最终项目资产：

| 资产 | 用途 | 尺寸/大小 |
|---|---|---|
| `petlink-home-hero-v2.webp` | 首屏主视觉 | 1536×1024，约 63 KB |
| `pudding-cat-card-v2.webp` | 参与卡片 | 720×520，约 28 KB |
| `ahei-black-dog-card-v2.webp` | 参与卡片 | 720×520，约 22 KB |
| `coffee-kitten-card-v2.webp` | 参与卡片 | 720×520，约 36 KB |

生成提示词：

```text
Use case: photorealistic-natural
Asset type: PetLink animal rescue platform landing-page hero image
Primary request: Create an emotionally compelling, premium editorial photograph about rescued animals finding safety and a new home.
Scene/backdrop: a calm, modern animal rescue shelter at golden morning light, warm ivory plaster, subtle deep forest-green accents, soft linen bedding, natural wood.
Subject: one gentle rescued orange-and-white cat in the foreground and one calm black-and-white dog just behind it, both healthy but authentically real, looking slightly toward the camera; a caregiver's hand is softly visible at the edge offering reassurance, no human face.
Style/medium: high-end documentary lifestyle photography, photorealistic, natural fur texture, tasteful and humane, not staged advertising gloss.
Composition/framing: wide landscape, subjects concentrated in the right-center and lower-right, generous clean negative space on the left for website headline, layered depth, 35mm lens feeling.
Lighting/mood: warm directional sunrise, quiet hope, trust, safety, restrained cinematic contrast.
Color palette: deep forest green, warm ivory, muted apricot, natural brown.
Constraints: no text, no logo, no watermark, no cages, no medical distress, no extra animals, no deformed paws or eyes, no duplicated limbs, no exaggerated expressions.
```

生成图未被覆盖；项目仅使用独立的轻量 WebP 派生版本。

## 3. 前端性能结果

优化前公众首页会预加载完整 Element Plus：JS 约 921 KB（gzip 约 304 KB），CSS 约 353 KB（gzip 约 48 KB），旧首图约 324 KB。

优化后：

- Element Plus 改为组件与样式按需加载，登录页和工作台改为路由懒加载，公众首页不再依赖 Element Plus。
- 初始入口、Vue、Axios 与基础 CSS 合计 gzip 约 80.2 KB。
- 公众首页路由 JS/CSS 合计 gzip 约 10.2 KB，首屏主视觉约 63 KB。
- 公众首屏关键传输量约 154 KB，较优化前约 744 KB 减少约 79%。
- 生产首页不再预加载完整 Element Plus 资源；公共页面和管理工作台各自按路由加载所需代码。

## 4. 后端性能结果

批量装配替代分页后的逐行查询：

| 列表 | 优化前查询量 | 优化后查询量 |
|---|---:|---:|
| 动物分页（N 条） | N + 2 | 3 |
| 线索分页（N 条） | N + 2 | 3 |
| 领养申请（N 条） | N + 2 | 3 |
| 领养记录（N 条） | 2N + 2 | 4 |

按每页 20 条计算，领养记录由约 42 次查询降至 4 次。批量封面、动物装配均有 Mockito 查询次数测试约束，防止后续回归为 N+1。

本轮公网连续 10 次测量（包含客户端到服务器约 100 ms 的网络往返）：

- 公开动物分页平均约 100.9 ms。
- 公开公告分页平均约 98.5 ms。

此外，线索时间校验增加 5 分钟分布式时钟容差，正常客户端/服务器轻微时间偏差不再误报“发现时间不能晚于当前时间”；超过容差的未来时间仍被拒绝。

## 5. 静态交付与缓存

- [x] 哈希 JS/CSS 使用 `Cache-Control: public, max-age=31536000, immutable`。
- [x] JS 请求实测返回 `Content-Encoding: gzip` 与 `Vary: Accept-Encoding`。
- [x] PC 与 Mobile 的 `index.html` 使用 `no-cache, no-store, must-revalidate`，确保发布后及时获得新资源清单。
- [x] WebP 主视觉线上返回 200，传输大小 64,362 字节。
- [x] Nginx 启用 `sendfile`、`tcp_nopush` 和文件句柄短缓存。

## 6. 自动化与生产验收

- [x] Spring Boot/JUnit：297 项全部通过，55 个测试套件失败/错误为 0。
- [x] PC Node：36/36 通过，Vite 生产构建通过。
- [x] Mobile Node：18/18 通过，`/mobile/` 生产构建通过。
- [x] 生产跨平台主业务：40/40 通过，覆盖注册、线索、审核、接单、救助、建档、领养、回访、公告与权限隔离。
- [x] 生产账号权限生命周期：20/20 通过，覆盖升降级、旧令牌即时权限、禁用恢复与接单并发。
- [x] 生产用户/公告/检索管理：15/15 通过。
- [x] 生产闭环生成的随机编号验收公告已在确认测试结果后逻辑删除，公开首页仅保留正式流程指南。
- [x] PC 本地产物与服务器 `index.html` SHA-256 一致：`19d9527387fb3e1dd0dd676dd851e7483e567d08f285554795da8654952952de`。
- [x] 线上后端 JAR SHA-256：`5f400434a068e0f00a02fb6703fbbe50631a6bc6938f1744e74c219661d7bae0`。
- [x] `/health` 返回 `UP`；PC、Mobile 与原 80 端口项目均返回 200。
- [x] MySQL 和 PetLink 后端没有新增公网监听；公网仅保留原 80 与 PetLink 8081。

后端重启瞬间曾产生 1 条 Nginx 上游连接拒绝日志，对应健康轮询碰到容器切换窗口；新容器就绪后的验收请求全部成功，后端无 ERROR。

## 7. 发布、备份与回滚

- 发布目录：`/srv/petlink`
- 发布候选归档：`/srv/petlink-releases/20260905-homepage-performance-v2`
- 发布前备份：`/srv/petlink-backups/20260905-014614-homepage-performance`
- PC：`http://103.233.254.186:8081/`
- Mobile：`http://103.233.254.186:8081/mobile/`
- Health：`http://103.233.254.186:8081/health`

如需回滚，先停止新增业务写入，再从备份恢复上一版运行文件并仅重建 `backend`、`web`；数据库恢复属于有损操作，不能作为默认第一步。本次没有修改原项目 80 端口服务。
