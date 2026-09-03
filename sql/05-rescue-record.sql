CREATE TABLE `rescue_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '救助过程记录ID',
    `task_id` BIGINT UNSIGNED NOT NULL COMMENT '救助任务ID',
    `recorder_id` BIGINT UNSIGNED NOT NULL COMMENT '记录人ID',
    `content` VARCHAR(2000) NOT NULL COMMENT '救助过程记录内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录提交时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_rescue_record_task`
        FOREIGN KEY (`task_id`) REFERENCES `rescue_task` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_rescue_record_recorder`
        FOREIGN KEY (`recorder_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_rescue_record_content`
        CHECK (CHAR_LENGTH(TRIM(`content`)) > 0),

    KEY `idx_rescue_record_task_created` (`task_id`, `created_at`, `id`),
    KEY `idx_rescue_record_recorder` (`recorder_id`)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='救助过程记录表';
