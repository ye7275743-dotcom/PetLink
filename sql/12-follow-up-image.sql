CREATE TABLE `follow_up_image` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '回访图片ID',
    `follow_up_id` BIGINT UNSIGNED NOT NULL COMMENT '回访记录ID',
    `image_path` VARCHAR(500) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '正式图片相对路径',
    `sort_order` TINYINT UNSIGNED NOT NULL COMMENT '图片展示顺序，1～9',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_follow_up_image_follow_up`
        FOREIGN KEY (`follow_up_id`) REFERENCES `follow_up_record` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `uk_follow_up_image_follow_up_sort`
        UNIQUE (`follow_up_id`, `sort_order`),
    CONSTRAINT `uk_follow_up_image_path`
        UNIQUE (`image_path`),
    CONSTRAINT `chk_follow_up_image_sort_order`
        CHECK (`sort_order` BETWEEN 1 AND 9)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='领养回访图片表';
