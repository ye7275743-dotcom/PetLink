# PetLink V1.0 交付清单

更新时间：2026-09-02

## A. 已就绪交付物

- [x] 后端源码：`server/src/`
- [x] PC 源码与锁文件：`petlink-pc/`
- [x] Mobile 源码与锁文件：`petlink-mobile/`
- [x] 数据库总脚本：`sql/petlink.sql`
- [x] 分表 SQL、结构验证和本地种子：`sql/`
- [x] 详细设计 Frozen：工作区上级目录中的 `PetLink-详细设计说明书-v1.0-Frozen(1)(1).docx`
- [x] REST API 文档：`docs/api/`
- [x] 数据库设计文档：`docs/database/`
- [x] 用例说明：`docs/use-cases/`
- [x] UML 源文件与渲染结果：`uml/`
- [x] 原 RTM V1.1 Frozen（保留）：`docs/PetLink-需求追踪矩阵-v1.1-Frozen.xlsx`
- [x] 实施 RTM V1.2：`docs/PetLink-需求追踪矩阵-v1.2-Implemented-Frozen.xlsx`
- [x] M06～M08 Frozen 证据：`server/docs/`
- [x] 功能/异常安全/性能/兼容性报告：`docs/testing/`
- [x] 50 用户性能原始 JSON：`docs/testing/evidence/performance-2026-09-02.json`
- [x] 可复现性能脚本：`server/scripts/performance_m01_m08.py`
- [x] PC 生产构建：`petlink-pc/dist/`
- [x] Mobile H5 生产构建：`petlink-mobile/dist/`
- [x] 运行与验证说明：`README-CODING.md`、`README-FRONTEND.md`、`server/README.md`
- [x] 前端中文化变更记录与浏览器证据：`docs/frontend/FRONTEND-CHINESE-LOCALIZATION-CHANGE.md`
- [x] 演示运行手册：`docs/DEMO-RUNBOOK-V1.0.md`
- [x] 前端依赖安全审计与移动端升级边界：`docs/testing/DEPENDENCY-AUDIT-2026-09-02.md`
- [x] macOS/Linux 一键自动回归脚本：`scripts/verify-all.sh`
- [x] 最终源码与文档压缩包：`../PetLink-v1.0-Frozen-Chinese-Delivery-20260902.zip`（SHA-256 见压缩包旁的 `.sha256` 文件）

## B. 最终打包前检查

- [x] 在全新目录按 README 执行后端测试、静态验证和前端构建。
- [x] 确认压缩包不包含 `.env` 私密值、数据库密码、JWT secret、token、IDE 缓存或临时上传文件。
- [x] 明确演示数据库初始化顺序：`petlink.sql` → `dev-seed-admin.sql` → `dev-seed-rescuer.sql` → `dev-seed-user.sql` → 启动后端。
- [x] 准备 ADMIN、RESCUER、USER 三类演示账号种子：`sql/dev-seed-admin.sql`、`sql/dev-seed-rescuer.sql`、`sql/dev-seed-user.sql`；密码只在现场说明，不写入公开文档。
- [x] 确认 PC 与 Mobile 的 API Base URL 指向本机演示后端。
- [x] 对提交压缩包计算 SHA-256 并记录提交日期（同目录 `.sha256` 文件）。

## C. 人工兼容与演示

- [x] 本机内置浏览器：PC 登录态 9 页、Mobile 访客 5 页中文化与控制台检查。
- [ ] Chrome 最新稳定版核心流程。
- [ ] Edge 最新稳定版核心流程。
- [ ] HBuilderX H5/模拟器流程。
- [ ] Android/iOS 真机上传、权限、返回栈和安全区。
- [ ] 生产/演示域名 HTTPS、CORS 和错误提示。
- [ ] 10～15 分钟演示彩排，覆盖 USER → RESCUER → ADMIN 主链。
- [ ] 准备断网、重复提交、越权、乐观锁冲突的备用演示说明。

## D. 课程提交物

- [ ] 答辩 PPT。
- [ ] 项目演示脚本与分工表。
- [ ] 课程要求的项目报告/封面/签字页。
- [x] 最终源码与文档压缩包（已生成并记录 SHA-256；课程提交前仍应按现场要求复核内容）。
- [ ] 若老师要求，附数据库备份或一键初始化说明。

## E. 当前判定

代码、自动化证据与冻结交付包：**已就绪**。  
最终提交签字：仍需完成适用的浏览器/真机/部署人工项；答辩 PPT、演示脚本、项目报告和完整彩排按当前要求暂缓。
