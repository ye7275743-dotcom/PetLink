-- PetLink V1.0 完整数据库初始化脚本
-- 适用版本：MySQL 8.0.16 及以上
-- 汇总日期：2026-08-29
-- 说明：
--   1. 本脚本按外键依赖顺序创建 15 张核心业务表 + 1 张 temporary_file 技术支撑表，共 16 张物理表。
--   2. 本脚本不包含 DROP DATABASE / DROP TABLE 等破坏性语句。
--   3. 建议在空数据库环境中执行；重复执行遇到已有表时会停止，
--      以便及时发现数据库结构与设计基线不一致的问题。
--   4. 初始 ADMIN 仅提供注释模板，部署前须生成真实 BCrypt 哈希。
--   5. SET time_zone 仅影响当前连接；Spring Boot 运行时还须保证
--      应用、JDBC 连接与 MySQL session 使用统一的 +08:00 时区。

SET NAMES utf8mb4;
SET time_zone = '+08:00';

CREATE DATABASE IF NOT EXISTS `petlink`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `petlink`;

-- =====================================================================
-- 01. sys_user
-- 来源：01-sys-user.sql
-- =====================================================================

CREATE TABLE `sys_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT '用户ID',

    `account` VARCHAR(50)
        CHARACTER SET ascii
        COLLATE ascii_general_ci
        NOT NULL
        COMMENT '登录账号，大小写不敏感，创建后不可修改',

    `password_hash` VARCHAR(100)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL
        COMMENT 'BCrypt密码哈希',

    `nickname` VARCHAR(50)
        NOT NULL
        COMMENT '用户昵称',

    `phone` VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        DEFAULT NULL
        COMMENT '手机号',

    `role_code` VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL
        DEFAULT 'USER'
        COMMENT '当前角色：USER/RESCUER/ADMIN',

    `status` VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL
        DEFAULT 'ENABLED'
        COMMENT '账号状态：ENABLED/DISABLED',

    `deleted` TINYINT NOT NULL DEFAULT 0
        COMMENT '逻辑删除：0=正常，1=已删除',

    `created_at` DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        COMMENT '创建时间',

    `updated_at` DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '更新时间',

    PRIMARY KEY (`id`),

    CONSTRAINT `uk_sys_user_account`
        UNIQUE (`account`),

    CONSTRAINT `chk_sys_user_account`
        CHECK (
            `account` = TRIM(`account`)
            AND `account` REGEXP '^[A-Za-z0-9_]{4,50}$'
        ),

    CONSTRAINT `chk_sys_user_nickname`
        CHECK (CHAR_LENGTH(TRIM(`nickname`)) > 0),

    CONSTRAINT `chk_sys_user_phone`
        CHECK (
            `phone` IS NULL
            OR (
                `phone` = TRIM(`phone`)
                AND CHAR_LENGTH(`phone`) > 0
            )
        ),

    CONSTRAINT `chk_sys_user_role_code`
        CHECK (`role_code` IN ('USER', 'RESCUER', 'ADMIN')),

    CONSTRAINT `chk_sys_user_status`
        CHECK (`status` IN ('ENABLED', 'DISABLED')),

    KEY `idx_sys_user_role_status`
        (`role_code`, `status`),

    KEY `idx_sys_user_deleted_role_status`
        (`deleted`, `role_code`, `status`)

) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表';

-- 初始管理员模板（默认不执行）。
-- 部署前使用 BCrypt 生成真实哈希，替换占位内容并取消以下语句的注释。
-- 禁止把管理员明文密码写入 SQL 或版本库。
--
-- INSERT INTO `sys_user`
-- (`account`, `password_hash`, `nickname`, `role_code`, `status`)
-- VALUES
-- ('admin', '<PRE_GENERATED_BCRYPT_HASH>', '系统管理员', 'ADMIN', 'ENABLED');

-- =====================================================================
-- 02. rescue_clue
-- 来源：02-rescue-clue.sql
-- =====================================================================

CREATE TABLE `rescue_clue` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT '救助线索ID',

    `publisher_id` BIGINT UNSIGNED NOT NULL
        COMMENT '发布人ID',

    `location` VARCHAR(255) NOT NULL
        COMMENT '发现地点',

    `found_time` DATETIME NOT NULL
        COMMENT '发现时间',

    `animal_description` VARCHAR(1000) NOT NULL
        COMMENT '动物描述',

    `scene_description` VARCHAR(1000) DEFAULT NULL
        COMMENT '现场补充描述',

    `contact` VARCHAR(100) NOT NULL
        COMMENT '本条线索联系方式快照',

    `status` VARCHAR(20)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL DEFAULT 'PENDING_REVIEW'
        COMMENT 'PENDING_REVIEW/REJECTED/WITHDRAWN/WAITING_ACCEPT/CONVERTED/CLOSED',

    `reviewer_id` BIGINT UNSIGNED DEFAULT NULL
        COMMENT '审核人ID',

    `reviewed_at` DATETIME DEFAULT NULL
        COMMENT '审核时间',

    `reject_reason` VARCHAR(500) DEFAULT NULL
        COMMENT '驳回原因',

    `created_at` DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        COMMENT '创建时间',

    `updated_at` DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
        COMMENT '更新时间',

    PRIMARY KEY (`id`),

    CONSTRAINT `fk_rescue_clue_publisher`
        FOREIGN KEY (`publisher_id`)
        REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT `fk_rescue_clue_reviewer`
        FOREIGN KEY (`reviewer_id`)
        REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT `chk_rescue_clue_status`
        CHECK (`status` IN (
            'PENDING_REVIEW',
            'REJECTED',
            'WITHDRAWN',
            'WAITING_ACCEPT',
            'CONVERTED',
            'CLOSED'
        )),

    CONSTRAINT `chk_rescue_clue_required_text`
        CHECK (
            CHAR_LENGTH(TRIM(`location`)) > 0
            AND CHAR_LENGTH(TRIM(`animal_description`)) > 0
            AND CHAR_LENGTH(TRIM(`contact`)) > 0
        ),

    CONSTRAINT `chk_rescue_clue_review_info`
        CHECK (
            (
                `status` IN ('PENDING_REVIEW', 'WITHDRAWN')
                AND `reviewer_id` IS NULL
                AND `reviewed_at` IS NULL
            )
            OR
            (
                `status` IN (
                    'REJECTED',
                    'WAITING_ACCEPT',
                    'CONVERTED',
                    'CLOSED'
                )
                AND `reviewer_id` IS NOT NULL
                AND `reviewed_at` IS NOT NULL
            )
        ),

    CONSTRAINT `chk_rescue_clue_reviewed_at`
        CHECK (
            `reviewed_at` IS NULL
            OR `reviewed_at` >= `created_at`
        ),

    CONSTRAINT `chk_rescue_clue_reject_reason`
        CHECK (
            (
                `status` = 'REJECTED'
                AND `reject_reason` IS NOT NULL
                AND CHAR_LENGTH(TRIM(`reject_reason`)) > 0
            )
            OR
            (
                `status` <> 'REJECTED'
                AND `reject_reason` IS NULL
            )
        ),

    KEY `idx_rescue_clue_publisher_status_created`
        (`publisher_id`, `status`, `created_at` DESC, `id` DESC),

    KEY `idx_rescue_clue_status_created`
        (`status`, `created_at` DESC, `id` DESC),

    KEY `idx_rescue_clue_reviewer`
        (`reviewer_id`)

) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='救助线索表';

-- =====================================================================
-- 03. rescue_clue_image
-- 来源：03-rescue-clue-image.sql
-- =====================================================================

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

-- =====================================================================
-- 04. rescue_task
-- 来源：04-rescue-task.sql
-- =====================================================================

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

-- =====================================================================
-- 05. rescue_record
-- 来源：05-rescue-record.sql
-- =====================================================================

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

-- =====================================================================
-- 06. animal
-- 来源：06-animal.sql
-- =====================================================================

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
    `status` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'TREATING'
        COMMENT 'TREATING/OBSERVING/AVAILABLE/SUSPENDED/ADOPTED',
    `suspend_reason` VARCHAR(500) DEFAULT NULL COMMENT '暂停领养原因',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
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

-- =====================================================================
-- 07. animal_image
-- 来源：07-animal-image.sql
-- =====================================================================

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

-- =====================================================================
-- 08. health_record
-- 来源：08-health-record.sql
-- =====================================================================

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

-- =====================================================================
-- 09. adoption_application
-- 来源：09-adoption-application.sql
-- =====================================================================

CREATE TABLE `adoption_application` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '领养申请ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
    `animal_id` BIGINT UNSIGNED NOT NULL COMMENT '申请动物ID',
    `adoption_reason` VARCHAR(1000) NOT NULL COMMENT '领养原因快照',
    `housing_condition` VARCHAR(1000) NOT NULL COMMENT '居住情况快照',
    `family_members` VARCHAR(1000) NOT NULL COMMENT '家庭成员情况快照',
    `pet_experience` VARCHAR(1000) NOT NULL COMMENT '养宠经验快照',
    `contact` VARCHAR(100) NOT NULL COMMENT '本次申请联系方式快照',
    `status` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING/APPROVED/REJECTED/WITHDRAWN/INVALIDATED',
    `reviewer_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
    `approved_animal_id` BIGINT UNSIGNED
        GENERATED ALWAYS AS (
            CASE WHEN `status` = 'APPROVED' THEN `animal_id` ELSE NULL END
        ) STORED COMMENT '已批准动物ID，仅用于唯一约束',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请提交时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_adoption_application_user`
        FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_adoption_application_animal`
        FOREIGN KEY (`animal_id`) REFERENCES `animal` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_adoption_application_reviewer`
        FOREIGN KEY (`reviewer_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT `uk_adoption_application_user_animal`
        UNIQUE (`user_id`, `animal_id`),
    CONSTRAINT `uk_adoption_application_approved_animal`
        UNIQUE (`approved_animal_id`),

    CONSTRAINT `chk_adoption_application_status`
        CHECK (`status` IN ('PENDING','APPROVED','REJECTED','WITHDRAWN','INVALIDATED')),
    CONSTRAINT `chk_adoption_application_required_text`
        CHECK (
            CHAR_LENGTH(TRIM(`adoption_reason`)) > 0
            AND CHAR_LENGTH(TRIM(`housing_condition`)) > 0
            AND CHAR_LENGTH(TRIM(`family_members`)) > 0
            AND CHAR_LENGTH(TRIM(`pet_experience`)) > 0
            AND CHAR_LENGTH(TRIM(`contact`)) > 0
        ),
    CONSTRAINT `chk_adoption_application_review_info`
        CHECK (
            (`status` IN ('PENDING','WITHDRAWN','INVALIDATED')
                AND `reviewer_id` IS NULL AND `reviewed_at` IS NULL)
            OR
            (`status` IN ('APPROVED','REJECTED')
                AND `reviewer_id` IS NOT NULL AND `reviewed_at` IS NOT NULL)
        ),
    CONSTRAINT `chk_adoption_application_not_self_review`
        CHECK (`reviewer_id` IS NULL OR `reviewer_id` <> `user_id`),
    CONSTRAINT `chk_adoption_application_reject_reason`
        CHECK (
            (`status` = 'REJECTED' AND `reject_reason` IS NOT NULL
                AND CHAR_LENGTH(TRIM(`reject_reason`)) > 0)
            OR
            (`status` <> 'REJECTED' AND `reject_reason` IS NULL)
        ),
    CONSTRAINT `chk_adoption_application_reviewed_at`
        CHECK (`reviewed_at` IS NULL OR `reviewed_at` >= `created_at`),

    KEY `idx_adoption_application_user_created`
        (`user_id`, `created_at` DESC, `id` DESC),
    KEY `idx_adoption_application_status_created`
        (`status`, `created_at` ASC, `id` ASC),
    KEY `idx_adoption_application_animal_status_id`
        (`animal_id`, `status`, `id`),
    KEY `idx_adoption_application_created`
        (`created_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='领养申请表';

-- =====================================================================
-- 10. adoption_record
-- 来源：10-adoption-record.sql
-- =====================================================================

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

-- =====================================================================
-- 11. follow_up_record
-- 来源：11-follow-up-record.sql
-- =====================================================================

CREATE TABLE `follow_up_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '领养回访记录ID',
    `adoption_record_id` BIGINT UNSIGNED NOT NULL COMMENT '对应领养记录ID',
    `submitter_id` BIGINT UNSIGNED NOT NULL COMMENT '回访提交人ID',
    `content` VARCHAR(2000) DEFAULT NULL COMMENT '生活情况及文字描述',
    `health_condition` VARCHAR(1000) DEFAULT NULL COMMENT '当前健康情况',
    `idempotency_key` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '小写UUID提交幂等键，创建后不可修改',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回访提交时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_follow_up_adoption_record`
        FOREIGN KEY (`adoption_record_id`) REFERENCES `adoption_record` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_follow_up_submitter`
        FOREIGN KEY (`submitter_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT `uk_follow_up_submitter_idempotency`
        UNIQUE (`submitter_id`, `idempotency_key`),
    CONSTRAINT `chk_follow_up_content`
        CHECK (`content` IS NULL OR CHAR_LENGTH(TRIM(`content`)) > 0),
    CONSTRAINT `chk_follow_up_health_condition`
        CHECK (`health_condition` IS NULL
            OR CHAR_LENGTH(TRIM(`health_condition`)) > 0),
    CONSTRAINT `chk_follow_up_idempotency_key`
        CHECK (
            `idempotency_key` = TRIM(`idempotency_key`)
            AND CHAR_LENGTH(`idempotency_key`) = 36
            AND `idempotency_key` = LOWER(`idempotency_key`)
        ),

    KEY `idx_follow_up_adoption_created`
        (`adoption_record_id`, `created_at` DESC, `id` DESC),
    KEY `idx_follow_up_submitter_created`
        (`submitter_id`, `created_at` DESC, `id` DESC),
    KEY `idx_follow_up_created`
        (`created_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='领养回访记录表';

-- =====================================================================
-- 12. follow_up_image
-- 来源：12-follow-up-image.sql
-- =====================================================================

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

-- =====================================================================
-- 13. favorite
-- 来源：13-favorite.sql
-- =====================================================================

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

-- =====================================================================
-- 14. announcement
-- 来源：14-announcement.sql
-- =====================================================================

CREATE TABLE `announcement` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告正文',
    `status` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/WITHDRAWN',
    `created_by` BIGINT UNSIGNED NOT NULL COMMENT '创建管理员ID',
    `updated_by` BIGINT UNSIGNED NOT NULL COMMENT '最后更新管理员ID',
    `published_at` DATETIME DEFAULT NULL COMMENT '首次正式发布时间',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_announcement_creator`
        FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_announcement_updater`
        FOREIGN KEY (`updated_by`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT `chk_announcement_status`
        CHECK (`status` IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT `chk_announcement_title`
        CHECK (CHAR_LENGTH(TRIM(`title`)) > 0),
    CONSTRAINT `chk_announcement_content`
        CHECK (CHAR_LENGTH(TRIM(`content`)) > 0),
    CONSTRAINT `chk_announcement_published_at`
        CHECK (
            (`status` = 'DRAFT' AND `published_at` IS NULL)
            OR
            (`status` IN ('PUBLISHED','WITHDRAWN')
                AND `published_at` IS NOT NULL
                AND `published_at` >= `created_at`)
        ),

    KEY `idx_announcement_status_published`
        (`status`, `published_at` DESC, `id` DESC),
    KEY `idx_announcement_created`
        (`created_at` DESC, `id` DESC),
    KEY `idx_announcement_deleted_status_created`
        (`deleted`, `status`, `created_at` DESC, `id` DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统公告表';

-- =====================================================================
-- 15. operation_log
-- 来源：15-operation-log.sql
-- =====================================================================

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

-- =====================================================================
-- 16. temporary_file
-- 来源：16-temporary-file.sql
-- =====================================================================

CREATE TABLE `temporary_file` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '临时文件ID',
    `token` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '小写UUID临时文件令牌',
    `owner_id` BIGINT UNSIGNED NOT NULL COMMENT '上传用户ID',
    `temp_path` VARCHAR(500) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '临时文件相对路径',
    `file_extension` VARCHAR(10) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '文件扩展名',
    `mime_type` VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL COMMENT '文件MIME类型',
    `file_size_bytes` BIGINT UNSIGNED NOT NULL COMMENT '文件大小，单位字节',
    `status` VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin
        NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/BOUND',
    `business_type` VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin
        DEFAULT NULL COMMENT '绑定业务类型',
    `business_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '绑定业务ID',
    `formal_path` VARCHAR(500) CHARACTER SET ascii COLLATE ascii_bin
        DEFAULT NULL COMMENT '绑定后对应的正式相对路径',
    `expires_at` DATETIME NOT NULL COMMENT '临时文件过期时间',
    `bound_at` DATETIME DEFAULT NULL COMMENT '业务绑定时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传记录创建时间',

    PRIMARY KEY (`id`),
    CONSTRAINT `fk_temporary_file_owner`
        FOREIGN KEY (`owner_id`) REFERENCES `sys_user` (`id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT `uk_temporary_file_token`
        UNIQUE (`token`),
    CONSTRAINT `uk_temporary_file_temp_path`
        UNIQUE (`temp_path`),
    CONSTRAINT `uk_temporary_file_formal_path`
        UNIQUE (`formal_path`),

    CONSTRAINT `chk_temporary_file_token`
        CHECK (CHAR_LENGTH(TRIM(`token`)) = 36 AND `token` = LOWER(`token`)),
    CONSTRAINT `chk_temporary_file_temp_path`
        CHECK (
            `temp_path` = TRIM(`temp_path`)
            AND CHAR_LENGTH(`temp_path`) > 0
        ),
    CONSTRAINT `chk_temporary_file_status`
        CHECK (`status` IN ('UPLOADED','BOUND')),
    CONSTRAINT `chk_temporary_file_extension`
        CHECK (`file_extension` IN ('jpg','jpeg','png')),
    CONSTRAINT `chk_temporary_file_mime`
        CHECK (`mime_type` IN ('image/jpeg','image/png')),
    CONSTRAINT `chk_temporary_file_type_match`
        CHECK (
            (`file_extension` IN ('jpg','jpeg') AND `mime_type` = 'image/jpeg')
            OR
            (`file_extension` = 'png' AND `mime_type` = 'image/png')
        ),
    CONSTRAINT `chk_temporary_file_size`
        CHECK (`file_size_bytes` > 0 AND `file_size_bytes` <= 5242880),
    CONSTRAINT `chk_temporary_file_business_type`
        CHECK (`business_type` IS NULL
            OR `business_type` IN ('RESCUE_CLUE','ANIMAL','FOLLOW_UP')),
    CONSTRAINT `chk_temporary_file_binding`
        CHECK (
            (`status` = 'UPLOADED'
                AND `business_type` IS NULL
                AND `business_id` IS NULL
                AND `formal_path` IS NULL
                AND `bound_at` IS NULL)
            OR
            (`status` = 'BOUND'
                AND `business_type` IS NOT NULL
                AND `business_id` IS NOT NULL
                AND `business_id` > 0
                AND `formal_path` IS NOT NULL
                AND `formal_path` = TRIM(`formal_path`)
                AND CHAR_LENGTH(`formal_path`) > 0
                AND `bound_at` IS NOT NULL
                AND `bound_at` >= `created_at`
                AND `bound_at` < `expires_at`)
        ),
    CONSTRAINT `chk_temporary_file_expires`
        CHECK (`expires_at` > `created_at`),

    KEY `idx_temporary_file_owner_status_expires`
        (`owner_id`, `status`, `expires_at`, `id`),
    KEY `idx_temporary_file_status_expires`
        (`status`, `expires_at`, `id`),
    KEY `idx_temporary_file_business`
        (`business_type`, `business_id`)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='临时上传文件技术支撑表';

-- =====================================================================
-- Service 层必须保证的跨表、权限与实现规则
-- =====================================================================
-- 1. operation_log 仅允许 INSERT / SELECT，不提供 UPDATE / DELETE。
-- 2. business_type + operation_type 必须由 Java Enum 校验合法组合。
-- 3. FollowUpRecord.submitter_id 必须等于 AdoptionRecord.user_id。
-- 4. AdoptionRecord.user_id / animal_id 必须从已锁定的
--    AdoptionApplication 派生，客户端不得指定或覆盖。
-- 5. Animal 必须来源于 SUCCESS RescueTask；任务成功、动物建档与
--    RescueClue 关闭必须在同一事务中完成。
-- 6. 新增 Favorite 时，Animal 必须处于 AVAILABLE。
-- 7. 公开 Announcement 列表和详情均只能访问 PUBLISHED 数据。
-- 8. TemporaryFile.token 与 FollowUpRecord.idempotency_key
--    必须由 Service 校验为完整的标准 UUID。
--
-- =====================================================================
-- PetLink V1.0 数据库结构初始化结束
-- =====================================================================
