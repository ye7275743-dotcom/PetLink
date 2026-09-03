CREATE TABLE `sys_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT '用户ID',

    `account` VARCHAR(50)
        CHARACTER SET ascii
        COLLATE ascii_general_ci
        NOT NULL
        COMMENT '登录账号，大小写不敏感，创建后不可修改',

    `password_hash` VARCHAR(100)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL
        COMMENT 'BCrypt密码哈希',

    `nickname` VARCHAR(50)
        NOT NULL
        COMMENT '用户昵称',

    `phone` VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        DEFAULT NULL
        COMMENT '手机号',

    `role_code` VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL
        DEFAULT 'USER'
        COMMENT '当前角色：USER/RESCUER/ADMIN',

    `status` VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL
        DEFAULT 'ENABLED'
        COMMENT '账号状态：ENABLED/DISABLED',

    `created_at` DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        COMMENT '创建时间',

    `updated_at` DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '更新时间',

    PRIMARY KEY (`id`),

    CONSTRAINT `uk_sys_user_account`
        UNIQUE (`account`),

    CONSTRAINT `chk_sys_user_account`
        CHECK (
            `account` = TRIM(`account`)
            AND `account` REGEXP '^[A-Za-z0-9_]{4,50}$'
        ),

    CONSTRAINT `chk_sys_user_nickname`
        CHECK (CHAR_LENGTH(TRIM(`nickname`)) > 0),

    CONSTRAINT `chk_sys_user_phone`
        CHECK (
            `phone` IS NULL
            OR (
                `phone` = TRIM(`phone`)
                AND CHAR_LENGTH(`phone`) > 0
            )
        ),

    CONSTRAINT `chk_sys_user_role_code`
        CHECK (`role_code` IN ('USER', 'RESCUER', 'ADMIN')),

    CONSTRAINT `chk_sys_user_status`
        CHECK (`status` IN ('ENABLED', 'DISABLED')),

    KEY `idx_sys_user_role_status`
        (`role_code`, `status`)

) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表';

-- 初始管理员模板（默认不执行）。
-- 部署前使用 BCrypt 生成真实哈希，替换占位内容并取消以下语句的注释。
-- 禁止把管理员明文密码写入 SQL 或版本库。
--
-- INSERT INTO `sys_user`
-- (`account`, `password_hash`, `nickname`, `role_code`, `status`)
-- VALUES
-- ('admin', '<PRE_GENERATED_BCRYPT_HASH>', '系统管理员', 'ADMIN', 'ENABLED');
