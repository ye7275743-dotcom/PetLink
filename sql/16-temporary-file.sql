CREATE TABLE `temporary_file` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '临时文件ID',
    `token` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '小写UUID临时文件令牌',
    `owner_id` BIGINT UNSIGNED NOT NULL COMMENT '上传用户ID',
    `temp_path` VARCHAR(500) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '临时文件相对路径',
    `file_extension` VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '文件扩展名',
    `mime_type` VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '文件MIME类型',
    `file_size_bytes` BIGINT UNSIGNED NOT NULL COMMENT '文件大小，单位字节',
    `status` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/BOUND',
    `business_type` VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin
        DEFAULT NULL COMMENT '绑定业务类型',
    `business_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '绑定业务ID',
    `formal_path` VARCHAR(500) CHARACTER SET ascii COLLATE ascii_bin
        DEFAULT NULL COMMENT '绑定后对应的正式相对路径',
    `expires_at` DATETIME NOT NULL COMMENT '临时文件过期时间',
    `bound_at` DATETIME DEFAULT NULL COMMENT '业务绑定时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传记录创建时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_temporary_file_owner`
        FOREIGN KEY (`owner_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT `uk_temporary_file_token`
        UNIQUE (`token`),
    CONSTRAINT `uk_temporary_file_temp_path`
        UNIQUE (`temp_path`),
    CONSTRAINT `uk_temporary_file_formal_path`
        UNIQUE (`formal_path`),

    CONSTRAINT `chk_temporary_file_token`
        CHECK (CHAR_LENGTH(TRIM(`token`)) = 36 AND `token` = LOWER(`token`)),
    CONSTRAINT `chk_temporary_file_temp_path`
        CHECK (
            `temp_path` = TRIM(`temp_path`)
            AND CHAR_LENGTH(`temp_path`) > 0
        ),
    CONSTRAINT `chk_temporary_file_status`
        CHECK (`status` IN ('UPLOADED','BOUND')),
    CONSTRAINT `chk_temporary_file_extension`
        CHECK (`file_extension` IN ('jpg','jpeg','png')),
    CONSTRAINT `chk_temporary_file_mime`
        CHECK (`mime_type` IN ('image/jpeg','image/png')),
    CONSTRAINT `chk_temporary_file_type_match`
        CHECK (
            (`file_extension` IN ('jpg','jpeg') AND `mime_type` = 'image/jpeg')
            OR
            (`file_extension` = 'png' AND `mime_type` = 'image/png')
        ),
    CONSTRAINT `chk_temporary_file_size`
        CHECK (`file_size_bytes` > 0 AND `file_size_bytes` <= 5242880),
    CONSTRAINT `chk_temporary_file_business_type`
        CHECK (`business_type` IS NULL
            OR `business_type` IN ('RESCUE_CLUE','ANIMAL','FOLLOW_UP')),
    CONSTRAINT `chk_temporary_file_binding`
        CHECK (
            (`status` = 'UPLOADED'
                AND `business_type` IS NULL
                AND `business_id` IS NULL
                AND `formal_path` IS NULL
                AND `bound_at` IS NULL)
            OR
            (`status` = 'BOUND'
                AND `business_type` IS NOT NULL
                AND `business_id` IS NOT NULL
                AND `business_id` > 0
                AND `formal_path` IS NOT NULL
                AND `formal_path` = TRIM(`formal_path`)
                AND CHAR_LENGTH(`formal_path`) > 0
                AND `bound_at` IS NOT NULL
                AND `bound_at` >= `created_at`
                AND `bound_at` < `expires_at`)
        ),
    CONSTRAINT `chk_temporary_file_expires`
        CHECK (`expires_at` > `created_at`),

    KEY `idx_temporary_file_owner_status_expires`
        (`owner_id`, `status`, `expires_at`, `id`),
    KEY `idx_temporary_file_status_expires`
        (`status`, `expires_at`, `id`),
    KEY `idx_temporary_file_business`
        (`business_type`, `business_id`)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='临时上传文件技术支撑表';
