CREATE TABLE `favorite` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '收藏用户ID',
    `animal_id` BIGINT UNSIGNED NOT NULL COMMENT '被收藏动物ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_favorite_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_favorite_animal`
        FOREIGN KEY (`animal_id`) REFERENCES `animal` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `uk_favorite_user_animal`
        UNIQUE (`user_id`, `animal_id`),

    KEY `idx_favorite_user_created`
        (`user_id`, `created_at` DESC, `id` DESC),
    KEY `idx_favorite_animal`
        (`animal_id`)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='动物收藏表';
