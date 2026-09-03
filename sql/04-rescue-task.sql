CREATE TABLE `rescue_task` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '救助任务ID',
    `clue_id` BIGINT UNSIGNED NOT NULL COMMENT '来源救助线索ID',
    `rescuer_id` BIGINT UNSIGNED NOT NULL COMMENT '负责救助人员ID',
    `status` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'WAITING_START'
        COMMENT 'WAITING_START/IN_PROGRESS/SUCCESS/FAILED/CANCELED',
    `active_clue_id` BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE
                WHEN `status` IN ('WAITING_START', 'IN_PROGRESS') THEN `clue_id`
                ELSE NULL
            END
        ) STORED COMMENT '活动任务线索ID，仅用于唯一约束',
    `started_at` DATETIME DEFAULT NULL COMMENT '实际开始救助时间',
    `finished_at` DATETIME DEFAULT NULL COMMENT '任务进入终态时间',
    `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '救助失败原因',
    `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT '管理员取消原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_rescue_task_clue`
        FOREIGN KEY (`clue_id`) REFERENCES `rescue_clue` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_rescue_task_rescuer`
        FOREIGN KEY (`rescuer_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `uk_rescue_task_active_clue` UNIQUE (`active_clue_id`),
    CONSTRAINT `chk_rescue_task_status`
        CHECK (`status` IN ('WAITING_START','IN_PROGRESS','SUCCESS','FAILED','CANCELED')),
    CONSTRAINT `chk_rescue_task_reason`
        CHECK (
            (`status` = 'FAILED' AND `failure_reason` IS NOT NULL
                AND CHAR_LENGTH(TRIM(`failure_reason`)) > 0 AND `cancel_reason` IS NULL)
            OR
            (`status` = 'CANCELED' AND `cancel_reason` IS NOT NULL
                AND CHAR_LENGTH(TRIM(`cancel_reason`)) > 0 AND `failure_reason` IS NULL)
            OR
            (`status` IN ('WAITING_START','IN_PROGRESS','SUCCESS')
                AND `failure_reason` IS NULL AND `cancel_reason` IS NULL)
        ),
    CONSTRAINT `chk_rescue_task_time`
        CHECK (
            (`status` = 'WAITING_START' AND `started_at` IS NULL AND `finished_at` IS NULL)
            OR
            (`status` = 'IN_PROGRESS' AND `started_at` IS NOT NULL AND `finished_at` IS NULL)
            OR
            (`status` IN ('SUCCESS','FAILED') AND `started_at` IS NOT NULL AND `finished_at` IS NOT NULL)
            OR
            (`status` = 'CANCELED' AND `finished_at` IS NOT NULL)
        ),
    CONSTRAINT `chk_rescue_task_time_order`
        CHECK (`started_at` IS NULL OR `finished_at` IS NULL OR `finished_at` >= `started_at`),
    CONSTRAINT `chk_rescue_task_time_created_order`
        CHECK (
            (`started_at` IS NULL OR `started_at` >= `created_at`)
            AND (`finished_at` IS NULL OR `finished_at` >= `created_at`)
        ),

    KEY `idx_rescue_task_clue_created`
        (`clue_id`, `created_at` DESC, `id` DESC),
    KEY `idx_rescue_task_rescuer_status_created`
        (`rescuer_id`, `status`, `created_at` DESC, `id` DESC),
    KEY `idx_rescue_task_status_created`
        (`status`, `created_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='救助任务表';
