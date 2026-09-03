CREATE TABLE `health_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '健康记录ID',
    `animal_id` BIGINT UNSIGNED NOT NULL COMMENT '动物ID',
    `recorder_id` BIGINT UNSIGNED NOT NULL COMMENT '记录人ID',
    `content` VARCHAR(2000) NOT NULL COMMENT '健康记录内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_health_record_animal`
        FOREIGN KEY (`animal_id`) REFERENCES `animal` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_health_record_recorder`
        FOREIGN KEY (`recorder_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_health_record_content`
        CHECK (CHAR_LENGTH(TRIM(`content`)) > 0),

    KEY `idx_health_record_animal_created`
        (`animal_id`, `created_at`, `id`),
    KEY `idx_health_record_recorder`
        (`recorder_id`)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='动物健康记录表';
