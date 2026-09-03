# PetLink 编码阶段启动基线 · Sprint 0 + M01 Rev2

## Sprint 0 验收条件

1. `petlink.sql` 可在 MySQL 8.0.16+ 成功执行。
2. 15 张核心业务表 + 1 张 `temporary_file`，共 16 张物理表存在。
3. CHECK / FK / UNIQUE / `active_clue_id` STORED GENERATED 均存在。
4. 开发环境 ADMIN 测试账号初始化完成。
5. Spring Boot 2.7.18 可启动并连接数据库。
6. `/actuator/health` 返回 UP。
7. M01 冒烟链：register → login → JWT → me → patch me → temporary upload 通过。
8. 模块测试结果回填 RTM；未执行项不得标记通过。

## Rev2 必修修正

- MySQL 驱动使用 `com.mysql:mysql-connector-j`。
- `MissingServletRequestPartException` 使用 `org.springframework.web.multipart.support`。
- 临时文件上传注册事务 `afterCompletion` 回调：事务未 COMMIT 时删除当前事务写入的物理临时文件。
- 保留方法内部即时异常清理，覆盖写文件/INSERT 阶段异常。

## 进入 M02 前的加固

- `mvnw` / `mvnw.cmd`：首次运行自动引导 Apache Maven 3.9.9。
- dev CORS 白名单：`localhost:5173` 与 `127.0.0.1:5173`；prod 必须显式提供允许来源。
- JWT secret/有效期、临时文件 TTL/根目录/清理间隔均在启动绑定阶段校验。
- 未知异常与文件保存失败仅写服务端日志，不向客户端泄露内部异常。
- 过期 `UPLOADED` 临时文件定时清理；`BOUND` 残留由带正式图片关联核验和路径安全检查的重试任务处理，不确定记录保留人工检查。
- `smoke-m01.ps1` 使用 GUID 账号，并包含临时 PNG 上传。
- 新增 Auth Web/Service、JWT Filter、PATCH presence、文件 MIME/文件头/大小/事务回滚清理测试。

## 接下来

本地完成 MySQL 初始化 + `./mvnw test` + Spring Boot 启动 + M01 冒烟后，再进入 M02。
后续每个模块按：接口实现 → Service/集成测试 → 数据库约束测试 → API 测试 → RTM 回填。
