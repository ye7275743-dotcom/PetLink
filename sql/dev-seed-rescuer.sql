-- PetLink local development RESCUER seed
-- Development/testing only.
-- account: rescuer
-- 密码由现场演示负责人单独保管，不在交付源码中记录明文。

SET NAMES utf8mb4;
USE `petlink`;

INSERT INTO `sys_user`
(`account`, `password_hash`, `nickname`, `phone`, `role_code`, `status`)
SELECT
    'rescuer',
    '$2y$10$Ez.VO4C6/r97/l8fGQlwvuYqkIIfqws0JTztPO1iFdwWFwRs4MPNu',
    '救助人员演示',
    NULL,
    'RESCUER',
    'ENABLED'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `account` = 'rescuer'
);
