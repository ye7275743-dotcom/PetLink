CREATE TABLE `adoption_application` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '领养申请ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
    `animal_id` BIGINT UNSIGNED NOT NULL COMMENT '申请动物ID',
    `adoption_reason` VARCHAR(1000) NOT NULL COMMENT '领养原因快照',
    `housing_condition` VARCHAR(1000) NOT NULL COMMENT '居住情况快照',
    `family_members` VARCHAR(1000) NOT NULL COMMENT '家庭成员情况快照',
    `pet_experience` VARCHAR(1000) NOT NULL COMMENT '养宠经验快照',
    `contact` VARCHAR(100) NOT NULL COMMENT '本次申请联系方式快照',
    `status` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/APPROVED/REJECTED/WITHDRAWN/INVALIDATED',
    `reviewer_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
    `approved_animal_id` BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE WHEN `status` = 'APPROVED' THEN `animal_id` ELSE NULL END
        ) STORED COMMENT '已批准动物ID，仅用于唯一约束',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请提交时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_adoption_application_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_adoption_application_animal`
        FOREIGN KEY (`animal_id`) REFERENCES `animal` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_adoption_application_reviewer`
        FOREIGN KEY (`reviewer_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT `uk_adoption_application_user_animal`
        UNIQUE (`user_id`, `animal_id`),
    CONSTRAINT `uk_adoption_application_approved_animal`
        UNIQUE (`approved_animal_id`),

    CONSTRAINT `chk_adoption_application_status`
        CHECK (`status` IN ('PENDING','APPROVED','REJECTED','WITHDRAWN','INVALIDATED')),
    CONSTRAINT `chk_adoption_application_required_text`
        CHECK (
            CHAR_LENGTH(TRIM(`adoption_reason`)) > 0
            AND CHAR_LENGTH(TRIM(`housing_condition`)) > 0
            AND CHAR_LENGTH(TRIM(`family_members`)) > 0
            AND CHAR_LENGTH(TRIM(`pet_experience`)) > 0
            AND CHAR_LENGTH(TRIM(`contact`)) > 0
        ),
    CONSTRAINT `chk_adoption_application_review_info`
        CHECK (
            (`status` IN ('PENDING','WITHDRAWN','INVALIDATED')
                AND `reviewer_id` IS NULL AND `reviewed_at` IS NULL)
            OR
            (`status` IN ('APPROVED','REJECTED')
                AND `reviewer_id` IS NOT NULL AND `reviewed_at` IS NOT NULL)
        ),
    CONSTRAINT `chk_adoption_application_not_self_review`
        CHECK (`reviewer_id` IS NULL OR `reviewer_id` <> `user_id`),
    CONSTRAINT `chk_adoption_application_reject_reason`
        CHECK (
            (`status` = 'REJECTED' AND `reject_reason` IS NOT NULL
                AND CHAR_LENGTH(TRIM(`reject_reason`)) > 0)
            OR
            (`status` <> 'REJECTED' AND `reject_reason` IS NULL)
        ),
    CONSTRAINT `chk_adoption_application_reviewed_at`
        CHECK (`reviewed_at` IS NULL OR `reviewed_at` >= `created_at`),

    KEY `idx_adoption_application_user_created`
        (`user_id`, `created_at` DESC, `id` DESC),
    KEY `idx_adoption_application_status_created`
        (`status`, `created_at` ASC, `id` ASC),
    KEY `idx_adoption_application_animal_status_id`
        (`animal_id`, `status`, `id`),
    KEY `idx_adoption_application_created`
        (`created_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='领养申请表';
