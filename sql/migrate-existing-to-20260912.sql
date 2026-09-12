-- Upgrade an existing PetLink development database to the 2026-09-12 schema.
-- Safe to run repeatedly: every column and index is checked before it is added.
-- Existing rows and passwords are preserved.

USE `petlink`;

DELIMITER $$

DROP PROCEDURE IF EXISTS `petlink_add_column_if_missing`$$
CREATE PROCEDURE `petlink_add_column_if_missing`(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition VARCHAR(2000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @petlink_ddl = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD COLUMN `', REPLACE(p_column_name, '`', '``'),
            '` ', p_definition
        );
        PREPARE petlink_stmt FROM @petlink_ddl;
        EXECUTE petlink_stmt;
        DEALLOCATE PREPARE petlink_stmt;
    END IF;
END$$

DROP PROCEDURE IF EXISTS `petlink_add_index_if_missing`$$
CREATE PROCEDURE `petlink_add_index_if_missing`(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_definition VARCHAR(2000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @petlink_ddl = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD INDEX `', REPLACE(p_index_name, '`', '``'),
            '` ', p_definition
        );
        PREPARE petlink_stmt FROM @petlink_ddl;
        EXECUTE petlink_stmt;
        DEALLOCATE PREPARE petlink_stmt;
    END IF;
END$$

CALL `petlink_add_column_if_missing`(
    'sys_user',
    'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0=正常，1=已删除'' AFTER `status`'
)$$

CALL `petlink_add_index_if_missing`(
    'sys_user',
    'idx_sys_user_deleted_role_status',
    '(`deleted`, `role_code`, `status`)'
)$$

CALL `petlink_add_column_if_missing`(
    'announcement',
    'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除：0=正常，1=已删除'' AFTER `version`'
)$$

CALL `petlink_add_index_if_missing`(
    'announcement',
    'idx_announcement_deleted_status_created',
    '(`deleted`, `status`, `created_at` DESC, `id` DESC)'
)$$

CALL `petlink_add_column_if_missing`(
    'animal',
    'personality',
    'VARCHAR(1000) DEFAULT NULL COMMENT ''性格与行为观察'' AFTER `health_condition`'
)$$

CALL `petlink_add_column_if_missing`(
    'animal',
    'adoption_requirements',
    'VARCHAR(1000) DEFAULT NULL COMMENT ''领养家庭要求'' AFTER `personality`'
)$$

DROP PROCEDURE `petlink_add_index_if_missing`$$
DROP PROCEDURE `petlink_add_column_if_missing`$$

DELIMITER ;

-- These rows are the post-migration verification result.
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
      (TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'deleted')
      OR
      (TABLE_NAME = 'announcement' AND COLUMN_NAME = 'deleted')
      OR
      (TABLE_NAME = 'animal' AND COLUMN_NAME IN ('personality', 'adoption_requirements'))
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;
