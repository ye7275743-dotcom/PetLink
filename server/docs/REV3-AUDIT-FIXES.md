# Sprint 0 + M01 Rev3 审计修复记录

本文件记录 Rev2 → Rev3 的编码阶段加固，不修改 Frozen 需求、数据库业务语义或 API 业务口径。

## 硬修复

- Controller MVC slice：Mock `JwtAuthenticationFilter`。
- Python 3.10：Schema 统计正则先计算后格式化。
- CORS：允许 `Idempotency-Key`。

## 加固

- 清理任务：单条 `RuntimeException` 隔离，继续处理后续记录。
- Maven bootstrap：固定 Maven 3.9.9 SHA-256 并 fail-closed。
- Smoke：multipart 改用 `curl.exe -F`，Windows PowerShell 5.1 可运行。
- 测试：新增 CORS 预检与 Cleanup 批次容错测试。

## Frozen 门槛

Rev3 仍不自动 Frozen。需要本机：`mvnw test` + Python schema 静态验证 + MySQL 真实初始化/约束查询 + Spring Boot 启动 + M01 smoke 全通过。
