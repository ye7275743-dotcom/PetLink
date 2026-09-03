# PetLink UC-01～UC-07 序列图 V1.0

状态：Frozen（已完成最终渲染核验）

共 7 个核心用例、8 个 PlantUML 文件：

1. `uc01-publish-rescue-clue.puml`
2. `uc02-audit-rescue-clue.puml`
3. `uc03-accept-rescue-task.puml`
4. `uc04a-start-and-record-rescue.puml`
5. `uc04b-submit-rescue-result.puml`
6. `uc05-submit-adoption-application.puml`
7. `uc06-audit-adoption-application.puml`
8. `uc07-submit-follow-up.puml`

说明：
- UC-04 因原图过长，拆为（a）开始救助与过程记录、（b）提交救助结果，两图仍属于同一个 UC-04。
- 全图统一使用 `hide footbox` 与 `skinparam shadowing false`。
- JWT、账号状态检查、RBAC 通过公共安全链统一说明，不单独展开参与者。
- OperationLog 使用正式 `business_type + operation_type` 名称。
- 写事务均明确 ROLLBACK 兜底；文件事务保留 copy → COMMIT → cleanup 规则。
- 所有锁顺序与已 Frozen API/数据库设计保持一致。
- UC-01 失败兜底覆盖临时文件校验；UC-07 空内容判断按 trim 后值执行。

