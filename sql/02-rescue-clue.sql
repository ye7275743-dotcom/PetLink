CREATE TABLE `rescue_clue` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT '救助线索ID',

    `publisher_id` BIGINT UNSIGNED NOT NULL
        COMMENT '发布人ID',

    `location` VARCHAR(255) NOT NULL
        COMMENT '发现地点',

    `found_time` DATETIME NOT NULL
        COMMENT '发现时间',

    `animal_description` VARCHAR(1000) NOT NULL
        COMMENT '动物描述',

    `scene_description` VARCHAR(1000) DEFAULT NULL
        COMMENT '现场补充描述',

    `contact` VARCHAR(100) NOT NULL
        COMMENT '本条线索联系方式快照',

    `status` VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL DEFAULT 'PENDING_REVIEW'
        COMMENT 'PENDING_REVIEW/REJECTED/WITHDRAWN/WAITING_ACCEPT/CONVERTED/CLOSED',

    `reviewer_id` BIGINT UNSIGNED DEFAULT NULL
        COMMENT '审核人ID',

    `reviewed_at` DATETIME DEFAULT NULL
        COMMENT '审核时间',

    `reject_reason` VARCHAR(500) DEFAULT NULL
        COMMENT '驳回原因',

    `created_at` DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        COMMENT '创建时间',

    `updated_at` DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '更新时间',

    PRIMARY KEY (`id`),

    CONSTRAINT `fk_rescue_clue_publisher`
        FOREIGN KEY (`publisher_id`)
        REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT `fk_rescue_clue_reviewer`
        FOREIGN KEY (`reviewer_id`)
        REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT `chk_rescue_clue_status`
        CHECK (`status` IN (
            'PENDING_REVIEW',
            'REJECTED',
            'WITHDRAWN',
            'WAITING_ACCEPT',
            'CONVERTED',
            'CLOSED'
        )),

    CONSTRAINT `chk_rescue_clue_required_text`
        CHECK (
            CHAR_LENGTH(TRIM(`location`)) > 0
            AND CHAR_LENGTH(TRIM(`animal_description`)) > 0
            AND CHAR_LENGTH(TRIM(`contact`)) > 0
        ),

    CONSTRAINT `chk_rescue_clue_review_info`
        CHECK (
            (
                `status` IN ('PENDING_REVIEW', 'WITHDRAWN')
                AND `reviewer_id` IS NULL
                AND `reviewed_at` IS NULL
            )
            OR
            (
                `status` IN (
                    'REJECTED',
                    'WAITING_ACCEPT',
                    'CONVERTED',
                    'CLOSED'
                )
                AND `reviewer_id` IS NOT NULL
                AND `reviewed_at` IS NOT NULL
            )
        ),

    CONSTRAINT `chk_rescue_clue_reviewed_at`
        CHECK (
            `reviewed_at` IS NULL
            OR `reviewed_at` >= `created_at`
        ),

    CONSTRAINT `chk_rescue_clue_reject_reason`
        CHECK (
            (
                `status` = 'REJECTED'
                AND `reject_reason` IS NOT NULL
                AND CHAR_LENGTH(TRIM(`reject_reason`)) > 0
            )
            OR
            (
                `status` <> 'REJECTED'
                AND `reject_reason` IS NULL
            )
        ),

    KEY `idx_rescue_clue_publisher_status_created`
        (`publisher_id`, `status`, `created_at` DESC, `id` DESC),

    KEY `idx_rescue_clue_status_created`
        (`status`, `created_at` DESC, `id` DESC),

    KEY `idx_rescue_clue_reviewer`
        (`reviewer_id`)

) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='救助线索表';
