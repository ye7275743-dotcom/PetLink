CREATE TABLE `follow_up_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '领养回访记录ID',
    `adoption_record_id` BIGINT UNSIGNED NOT NULL COMMENT '对应领养记录ID',
    `submitter_id` BIGINT UNSIGNED NOT NULL COMMENT '回访提交人ID',
    `content` VARCHAR(2000) DEFAULT NULL COMMENT '生活情况及文字描述',
    `health_condition` VARCHAR(1000) DEFAULT NULL COMMENT '当前健康情况',
    `idempotency_key` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '小写UUID提交幂等键，创建后不可修改',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回访提交时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_follow_up_adoption_record`
        FOREIGN KEY (`adoption_record_id`) REFERENCES `adoption_record` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_follow_up_submitter`
        FOREIGN KEY (`submitter_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT `uk_follow_up_submitter_idempotency`
        UNIQUE (`submitter_id`, `idempotency_key`),
    CONSTRAINT `chk_follow_up_content`
        CHECK (`content` IS NULL OR CHAR_LENGTH(TRIM(`content`)) > 0),
    CONSTRAINT `chk_follow_up_health_condition`
        CHECK (`health_condition` IS NULL
            OR CHAR_LENGTH(TRIM(`health_condition`)) > 0),
    CONSTRAINT `chk_follow_up_idempotency_key`
        CHECK (
            `idempotency_key` = TRIM(`idempotency_key`)
            AND CHAR_LENGTH(`idempotency_key`) = 36
            AND `idempotency_key` = LOWER(`idempotency_key`)
        ),

    KEY `idx_follow_up_adoption_created`
        (`adoption_record_id`, `created_at` DESC, `id` DESC),
    KEY `idx_follow_up_submitter_created`
        (`submitter_id`, `created_at` DESC, `id` DESC),
    KEY `idx_follow_up_created`
        (`created_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='领养回访记录表';
