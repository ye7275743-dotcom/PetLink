# PetLink 主要类图 V1.0 最终版

状态：Frozen

最终共 4 张正式类图：

1. `core-domain-class-diagram.puml`
   - 核心领域类图
   - 已通过语义与版面验收

2. `application-rescue-class-diagram.puml`
   - 应用层类图（a）
   - M01 用户认证
   - M02 救助线索
   - M03 救助任务
   - M04 动物档案
   - 公共 TemporaryFile 上传、FileService、IdempotencyService
   - 已通过语义验收，横向整页使用

3. `application-adoption-followup-class-diagram.puml`
   - 应用层类图（b-1）
   - M05 领养管理
   - M06 回访管理
   - FileService / TemporaryFileMapper / OperationLogMapper
   - 保留 M05/M06 的全部 Frozen 依赖

4. `application-favorite-admin-class-diagram.puml`
   - 应用层类图（b-2）
   - M07 收藏与公告
   - M08 后台管理与统计
   - 公共统计 Mapper / OperationLogMapper
   - 保留 StatsService 的全部 Frozen 统计依赖

本轮仅拆分原应用层图（b）的版面，不修改任何业务类、方法、依赖或数据库语义。

