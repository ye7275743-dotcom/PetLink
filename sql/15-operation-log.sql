CREATE TABLE `operation_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
    `business_type` VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '业务对象类型',
    `business_id` BIGINT UNSIGNED NOT NULL COMMENT '业务对象ID',
    `operation_type` VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '操作类型',
    `before_status` VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin
        DEFAULT NULL COMMENT '操作前状态',
    `after_status` VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin
        DEFAULT NULL COMMENT '操作后状态',
    `operator_id` BIGINT UNSIGNED NOT NULL COMMENT '操作者ID',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '操作原因快照',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_operation_log_operator`
        FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_operation_log_business_type`
        CHECK (`business_type` IN (
            'SYS_USER','RESCUE_CLUE','RESCUE_TASK','ANIMAL',
            'ADOPTION_APPLICATION','ANNOUNCEMENT'
        )),
    CONSTRAINT `chk_operation_log_business_id`
        CHECK (`business_id` > 0),
    CONSTRAINT `chk_operation_log_operation_type`
        CHECK (CHAR_LENGTH(TRIM(`operation_type`)) > 0),
    CONSTRAINT `chk_operation_log_status_text`
        CHECK (
            (`before_status` IS NULL OR CHAR_LENGTH(TRIM(`before_status`)) > 0)
            AND
            (`after_status` IS NULL OR CHAR_LENGTH(TRIM(`after_status`)) > 0)
        ),
    CONSTRAINT `chk_operation_log_reason`
        CHECK (`reason` IS NULL OR CHAR_LENGTH(TRIM(`reason`)) > 0),

    KEY `idx_operation_log_business_created`
        (`business_type`, `business_id`, `created_at` ASC, `id` ASC),
    KEY `idx_operation_log_operator_created`
        (`operator_id`, `created_at` DESC, `id` DESC),
    KEY `idx_operation_log_created`
        (`created_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='核心业务操作审计日志表';
