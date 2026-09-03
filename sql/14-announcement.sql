CREATE TABLE `announcement` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告正文',
    `status` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/WITHDRAWN',
    `created_by` BIGINT UNSIGNED NOT NULL COMMENT '创建管理员ID',
    `updated_by` BIGINT UNSIGNED NOT NULL COMMENT '最后更新管理员ID',
    `published_at` DATETIME DEFAULT NULL COMMENT '首次正式发布时间',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_announcement_creator`
        FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_announcement_updater`
        FOREIGN KEY (`updated_by`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT `chk_announcement_status`
        CHECK (`status` IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT `chk_announcement_title`
        CHECK (CHAR_LENGTH(TRIM(`title`)) > 0),
    CONSTRAINT `chk_announcement_content`
        CHECK (CHAR_LENGTH(TRIM(`content`)) > 0),
    CONSTRAINT `chk_announcement_published_at`
        CHECK (
            (`status` = 'DRAFT' AND `published_at` IS NULL)
            OR
            (`status` IN ('PUBLISHED','WITHDRAWN')
                AND `published_at` IS NOT NULL
                AND `published_at` >= `created_at`)
        ),

    KEY `idx_announcement_status_published`
        (`status`, `published_at` DESC, `id` DESC),
    KEY `idx_announcement_created`
        (`created_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统公告表';
