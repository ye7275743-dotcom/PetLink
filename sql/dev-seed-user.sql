-- PetLink 本地开发环境 USER 测试账号初始化
-- 仅用于课程设计开发/联调，不应在真实生产环境沿用默认密码。
-- account: user
-- 密码由现场演示负责人单独保管，不在交付源码中记录明文。

SET NAMES utf8mb4;
USE `petlink`;

INSERT INTO `sys_user`
(`account`, `password_hash`, `nickname`, `phone`, `role_code`, `status`)
SELECT
    'user',
    '$2y$10$Ez.VO4C6/r97/l8fGQlwvuYqkIIfqws0JTztPO1iFdwWFwRs4MPNu',
    '普通用户演示',
    NULL,
    'USER',
    'ENABLED'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `account` = 'user'
);
