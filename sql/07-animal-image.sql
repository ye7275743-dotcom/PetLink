CREATE TABLE `animal_image` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '动物图片ID',
    `animal_id` BIGINT UNSIGNED NOT NULL COMMENT '动物ID',
    `image_path` VARCHAR(500) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '正式图片相对路径',
    `sort_order` SMALLINT UNSIGNED NOT NULL COMMENT '图片展示顺序，从1开始',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_animal_image_animal`
        FOREIGN KEY (`animal_id`) REFERENCES `animal` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `uk_animal_image_animal_sort`
        UNIQUE (`animal_id`, `sort_order`),
    CONSTRAINT `uk_animal_image_path`
        UNIQUE (`image_path`),
    CONSTRAINT `chk_animal_image_sort_order`
        CHECK (`sort_order` >= 1)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='动物图片表';
