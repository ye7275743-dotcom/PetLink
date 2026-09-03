CREATE TABLE `adoption_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '领养记录ID',
    `application_id` BIGINT UNSIGNED NOT NULL COMMENT '来源领养申请ID',
    `animal_id` BIGINT UNSIGNED NOT NULL COMMENT '被领养动物ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '领养人ID',
    `adopted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领养生效时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_adoption_record_application`
        FOREIGN KEY (`application_id`) REFERENCES `adoption_application` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_adoption_record_animal`
        FOREIGN KEY (`animal_id`) REFERENCES `animal` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_adoption_record_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT `uk_adoption_record_application`
        UNIQUE (`application_id`),
    CONSTRAINT `uk_adoption_record_animal`
        UNIQUE (`animal_id`),

    KEY `idx_adoption_record_user_adopted`
        (`user_id`, `adopted_at` DESC, `id` DESC),
    KEY `idx_adoption_record_adopted`
        (`adopted_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='成功领养记录表';
