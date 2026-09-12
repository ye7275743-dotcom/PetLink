CREATE TABLE `animal` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '动物ID',
    `rescue_task_id` BIGINT UNSIGNED NOT NULL COMMENT '来源救助任务ID',
    `name` VARCHAR(100) NOT NULL COMMENT '动物名称',
    `species` VARCHAR(50) NOT NULL COMMENT '动物种类',
    `sex` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'UNKNOWN' COMMENT 'MALE/FEMALE/UNKNOWN',
    `estimated_age_months` SMALLINT UNSIGNED DEFAULT NULL COMMENT '估算年龄，单位月',
    `color` VARCHAR(100) DEFAULT NULL COMMENT '毛色',
    `health_condition` VARCHAR(1000) NOT NULL COMMENT '当前基础健康情况',
    `personality` VARCHAR(1000) DEFAULT NULL COMMENT '性格与行为观察',
    `adoption_requirements` VARCHAR(1000) DEFAULT NULL COMMENT '领养家庭要求',
    `status` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'TREATING'
        COMMENT 'TREATING/OBSERVING/AVAILABLE/SUSPENDED/ADOPTED',
    `suspend_reason` VARCHAR(500) DEFAULT NULL COMMENT '暂停领养原因',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_animal_rescue_task`
        FOREIGN KEY (`rescue_task_id`) REFERENCES `rescue_task` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `chk_animal_sex`
        CHECK (`sex` IN ('MALE', 'FEMALE', 'UNKNOWN')),
    CONSTRAINT `chk_animal_status`
        CHECK (`status` IN ('TREATING','OBSERVING','AVAILABLE','SUSPENDED','ADOPTED')),
    CONSTRAINT `chk_animal_required_text`
        CHECK (
            CHAR_LENGTH(TRIM(`name`)) > 0
            AND CHAR_LENGTH(TRIM(`species`)) > 0
            AND CHAR_LENGTH(TRIM(`health_condition`)) > 0
        ),
    CONSTRAINT `chk_animal_suspend_reason`
        CHECK (
            (`status` = 'SUSPENDED' AND `suspend_reason` IS NOT NULL
                AND CHAR_LENGTH(TRIM(`suspend_reason`)) > 0)
            OR
            (`status` <> 'SUSPENDED' AND `suspend_reason` IS NULL)
        ),

    KEY `idx_animal_task_created`
        (`rescue_task_id`, `created_at` DESC, `id` DESC),
    KEY `idx_animal_status_created`
        (`status`, `created_at` DESC, `id` DESC),
    KEY `idx_animal_status_species_created`
        (`status`, `species`, `created_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='动物档案表';
