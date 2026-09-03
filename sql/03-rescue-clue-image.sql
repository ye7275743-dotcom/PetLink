CREATE TABLE `rescue_clue_image` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT '救助线索图片ID',

    `clue_id` BIGINT UNSIGNED NOT NULL
        COMMENT '救助线索ID',

    `image_path` VARCHAR(500)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL
        COMMENT '正式图片相对路径',

    `sort_order` TINYINT UNSIGNED NOT NULL
        COMMENT '图片展示顺序，1～9',

    `created_at` DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        COMMENT '创建时间',

    PRIMARY KEY (`id`),

    CONSTRAINT `fk_rescue_clue_image_clue`
        FOREIGN KEY (`clue_id`)
        REFERENCES `rescue_clue` (`id`)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT `uk_rescue_clue_image_clue_sort`
        UNIQUE (`clue_id`, `sort_order`),

    CONSTRAINT `uk_rescue_clue_image_path`
        UNIQUE (`image_path`),

    CONSTRAINT `chk_rescue_clue_image_sort_order`
        CHECK (`sort_order` BETWEEN 1 AND 9)

) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='救助线索图片表';
