-- One-time, non-destructive data union for the reviewed 2026-09-12 database dump.
-- Source schema: petlink_incoming_20260912
-- Destination schema: petlink
--
-- Safety defaults:
--   1. Existing production rows always win.
--   2. Users are matched by their case-insensitive unique account.
--   3. Every incoming business row receives a new primary key and all foreign
--      keys are remapped, so overlapping numeric IDs cannot cross-link data.
--   4. Image metadata is deliberately excluded because the database archive
--      did not include the referenced binary upload files.
--   5. This checked-in script ends with ROLLBACK. Deployment creates an APPLY
--      copy whose final statement is changed to COMMIT after a dry run passes.

USE `petlink`;
SET NAMES utf8mb4;
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `merge_assert_zero`;
CREATE TEMPORARY TABLE `merge_assert_zero` (
    `value` BIGINT NOT NULL,
    CONSTRAINT `chk_merge_assert_zero` CHECK (`value` = 0)
);

DROP TEMPORARY TABLE IF EXISTS `merge_user_map`;
CREATE TEMPORARY TABLE `merge_user_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @user_base := (SELECT COALESCE(MAX(`id`), 0) FROM `sys_user`);
INSERT INTO `merge_user_map` (`old_id`, `new_id`)
SELECT n.`id`, COALESCE(p.`id`, @user_base + n.`id`)
FROM `petlink_incoming_20260912`.`sys_user` n
LEFT JOIN `sys_user` p ON p.`account` = n.`account`;
INSERT INTO `merge_assert_zero`
SELECT (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`sys_user`) - (SELECT COUNT(*) FROM `merge_user_map`);

-- MySQL cannot reopen the same temporary table twice in one statement. Keep a
-- second identical copy for reviewer/updater/business-user joins.
DROP TEMPORARY TABLE IF EXISTS `merge_user_map_secondary`;
CREATE TEMPORARY TABLE `merge_user_map_secondary` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
INSERT INTO `merge_user_map_secondary` SELECT `old_id`,`new_id` FROM `merge_user_map`;

INSERT INTO `sys_user`
(`id`,`account`,`password_hash`,`nickname`,`phone`,`role_code`,`status`,`deleted`,`created_at`,`updated_at`)
SELECT m.`new_id`,n.`account`,n.`password_hash`,n.`nickname`,n.`phone`,n.`role_code`,n.`status`,n.`deleted`,n.`created_at`,n.`updated_at`
FROM `petlink_incoming_20260912`.`sys_user` n
JOIN `merge_user_map` m ON m.`old_id` = n.`id`
LEFT JOIN `sys_user` p ON p.`account` = n.`account`
WHERE p.`id` IS NULL;
SET @inserted_users := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_clue_map`;
CREATE TEMPORARY TABLE `merge_clue_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @clue_base := (SELECT COALESCE(MAX(`id`), 0) FROM `rescue_clue`);
INSERT INTO `merge_clue_map` SELECT `id`, @clue_base + `id` FROM `petlink_incoming_20260912`.`rescue_clue`;
INSERT INTO `rescue_clue`
(`id`,`publisher_id`,`location`,`found_time`,`animal_description`,`scene_description`,`contact`,`status`,`reviewer_id`,`reviewed_at`,`reject_reason`,`created_at`,`updated_at`)
SELECT cm.`new_id`,up.`new_id`,n.`location`,n.`found_time`,n.`animal_description`,n.`scene_description`,n.`contact`,n.`status`,ur.`new_id`,n.`reviewed_at`,n.`reject_reason`,n.`created_at`,n.`updated_at`
FROM `petlink_incoming_20260912`.`rescue_clue` n
JOIN `merge_clue_map` cm ON cm.`old_id` = n.`id`
JOIN `merge_user_map` up ON up.`old_id` = n.`publisher_id`
LEFT JOIN `merge_user_map_secondary` ur ON ur.`old_id` = n.`reviewer_id`;
SET @inserted_clues := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_task_map`;
CREATE TEMPORARY TABLE `merge_task_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @task_base := (SELECT COALESCE(MAX(`id`), 0) FROM `rescue_task`);
INSERT INTO `merge_task_map` SELECT `id`, @task_base + `id` FROM `petlink_incoming_20260912`.`rescue_task`;
INSERT INTO `rescue_task`
(`id`,`clue_id`,`rescuer_id`,`status`,`started_at`,`finished_at`,`failure_reason`,`cancel_reason`,`created_at`,`updated_at`)
SELECT tm.`new_id`,cm.`new_id`,um.`new_id`,n.`status`,n.`started_at`,n.`finished_at`,n.`failure_reason`,n.`cancel_reason`,n.`created_at`,n.`updated_at`
FROM `petlink_incoming_20260912`.`rescue_task` n
JOIN `merge_task_map` tm ON tm.`old_id` = n.`id`
JOIN `merge_clue_map` cm ON cm.`old_id` = n.`clue_id`
JOIN `merge_user_map` um ON um.`old_id` = n.`rescuer_id`;
SET @inserted_tasks := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_rescue_record_map`;
CREATE TEMPORARY TABLE `merge_rescue_record_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @rescue_record_base := (SELECT COALESCE(MAX(`id`), 0) FROM `rescue_record`);
INSERT INTO `merge_rescue_record_map` SELECT `id`, @rescue_record_base + `id` FROM `petlink_incoming_20260912`.`rescue_record`;
INSERT INTO `rescue_record` (`id`,`task_id`,`recorder_id`,`content`,`created_at`)
SELECT rm.`new_id`,tm.`new_id`,um.`new_id`,n.`content`,n.`created_at`
FROM `petlink_incoming_20260912`.`rescue_record` n
JOIN `merge_rescue_record_map` rm ON rm.`old_id` = n.`id`
JOIN `merge_task_map` tm ON tm.`old_id` = n.`task_id`
JOIN `merge_user_map` um ON um.`old_id` = n.`recorder_id`;
SET @inserted_rescue_records := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_animal_map`;
CREATE TEMPORARY TABLE `merge_animal_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @animal_base := (SELECT COALESCE(MAX(`id`), 0) FROM `animal`);
INSERT INTO `merge_animal_map` SELECT `id`, @animal_base + `id` FROM `petlink_incoming_20260912`.`animal`;
INSERT INTO `animal`
(`id`,`rescue_task_id`,`name`,`species`,`sex`,`estimated_age_months`,`color`,`health_condition`,`personality`,`adoption_requirements`,`status`,`suspend_reason`,`version`,`created_at`,`updated_at`)
SELECT am.`new_id`,tm.`new_id`,n.`name`,n.`species`,n.`sex`,n.`estimated_age_months`,n.`color`,n.`health_condition`,n.`personality`,n.`adoption_requirements`,n.`status`,n.`suspend_reason`,n.`version`,n.`created_at`,n.`updated_at`
FROM `petlink_incoming_20260912`.`animal` n
JOIN `merge_animal_map` am ON am.`old_id` = n.`id`
JOIN `merge_task_map` tm ON tm.`old_id` = n.`rescue_task_id`;
SET @inserted_animals := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_health_map`;
CREATE TEMPORARY TABLE `merge_health_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @health_base := (SELECT COALESCE(MAX(`id`), 0) FROM `health_record`);
INSERT INTO `merge_health_map` SELECT `id`, @health_base + `id` FROM `petlink_incoming_20260912`.`health_record`;
INSERT INTO `health_record` (`id`,`animal_id`,`recorder_id`,`content`,`created_at`)
SELECT hm.`new_id`,am.`new_id`,um.`new_id`,n.`content`,n.`created_at`
FROM `petlink_incoming_20260912`.`health_record` n
JOIN `merge_health_map` hm ON hm.`old_id` = n.`id`
JOIN `merge_animal_map` am ON am.`old_id` = n.`animal_id`
JOIN `merge_user_map` um ON um.`old_id` = n.`recorder_id`;
SET @inserted_health_records := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_application_map`;
CREATE TEMPORARY TABLE `merge_application_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @application_base := (SELECT COALESCE(MAX(`id`), 0) FROM `adoption_application`);
INSERT INTO `merge_application_map` SELECT `id`, @application_base + `id` FROM `petlink_incoming_20260912`.`adoption_application`;
INSERT INTO `adoption_application`
(`id`,`user_id`,`animal_id`,`adoption_reason`,`housing_condition`,`family_members`,`pet_experience`,`contact`,`status`,`reviewer_id`,`reviewed_at`,`reject_reason`,`created_at`,`updated_at`)
SELECT apm.`new_id`,uu.`new_id`,am.`new_id`,n.`adoption_reason`,n.`housing_condition`,n.`family_members`,n.`pet_experience`,n.`contact`,n.`status`,ur.`new_id`,n.`reviewed_at`,n.`reject_reason`,n.`created_at`,n.`updated_at`
FROM `petlink_incoming_20260912`.`adoption_application` n
JOIN `merge_application_map` apm ON apm.`old_id` = n.`id`
JOIN `merge_user_map` uu ON uu.`old_id` = n.`user_id`
JOIN `merge_animal_map` am ON am.`old_id` = n.`animal_id`
LEFT JOIN `merge_user_map_secondary` ur ON ur.`old_id` = n.`reviewer_id`;
SET @inserted_applications := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_adoption_record_map`;
CREATE TEMPORARY TABLE `merge_adoption_record_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @adoption_record_base := (SELECT COALESCE(MAX(`id`), 0) FROM `adoption_record`);
INSERT INTO `merge_adoption_record_map` SELECT `id`, @adoption_record_base + `id` FROM `petlink_incoming_20260912`.`adoption_record`;
INSERT INTO `adoption_record` (`id`,`application_id`,`animal_id`,`user_id`,`adopted_at`,`created_at`)
SELECT arm.`new_id`,apm.`new_id`,am.`new_id`,um.`new_id`,n.`adopted_at`,n.`created_at`
FROM `petlink_incoming_20260912`.`adoption_record` n
JOIN `merge_adoption_record_map` arm ON arm.`old_id` = n.`id`
JOIN `merge_application_map` apm ON apm.`old_id` = n.`application_id`
JOIN `merge_animal_map` am ON am.`old_id` = n.`animal_id`
JOIN `merge_user_map` um ON um.`old_id` = n.`user_id`;
SET @inserted_adoption_records := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_followup_map`;
CREATE TEMPORARY TABLE `merge_followup_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @followup_base := (SELECT COALESCE(MAX(`id`), 0) FROM `follow_up_record`);
INSERT INTO `merge_followup_map` SELECT `id`, @followup_base + `id` FROM `petlink_incoming_20260912`.`follow_up_record`;
INSERT INTO `follow_up_record`
(`id`,`adoption_record_id`,`submitter_id`,`content`,`health_condition`,`idempotency_key`,`created_at`)
SELECT fm.`new_id`,arm.`new_id`,um.`new_id`,n.`content`,n.`health_condition`,n.`idempotency_key`,n.`created_at`
FROM `petlink_incoming_20260912`.`follow_up_record` n
JOIN `merge_followup_map` fm ON fm.`old_id` = n.`id`
JOIN `merge_adoption_record_map` arm ON arm.`old_id` = n.`adoption_record_id`
JOIN `merge_user_map` um ON um.`old_id` = n.`submitter_id`;
SET @inserted_followups := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_favorite_map`;
CREATE TEMPORARY TABLE `merge_favorite_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @favorite_base := (SELECT COALESCE(MAX(`id`), 0) FROM `favorite`);
INSERT INTO `merge_favorite_map` SELECT `id`, @favorite_base + `id` FROM `petlink_incoming_20260912`.`favorite`;
INSERT INTO `favorite` (`id`,`user_id`,`animal_id`,`created_at`)
SELECT fm.`new_id`,um.`new_id`,am.`new_id`,n.`created_at`
FROM `petlink_incoming_20260912`.`favorite` n
JOIN `merge_favorite_map` fm ON fm.`old_id` = n.`id`
JOIN `merge_user_map` um ON um.`old_id` = n.`user_id`
JOIN `merge_animal_map` am ON am.`old_id` = n.`animal_id`;
SET @inserted_favorites := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_announcement_map`;
CREATE TEMPORARY TABLE `merge_announcement_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @announcement_base := (SELECT COALESCE(MAX(`id`), 0) FROM `announcement`);
INSERT INTO `merge_announcement_map` SELECT `id`, @announcement_base + `id` FROM `petlink_incoming_20260912`.`announcement`;
INSERT INTO `announcement`
(`id`,`title`,`content`,`status`,`created_by`,`updated_by`,`published_at`,`version`,`created_at`,`updated_at`,`deleted`)
SELECT anm.`new_id`,n.`title`,n.`content`,n.`status`,uc.`new_id`,uu.`new_id`,n.`published_at`,n.`version`,n.`created_at`,n.`updated_at`,0
FROM `petlink_incoming_20260912`.`announcement` n
JOIN `merge_announcement_map` anm ON anm.`old_id` = n.`id`
JOIN `merge_user_map` uc ON uc.`old_id` = n.`created_by`
JOIN `merge_user_map_secondary` uu ON uu.`old_id` = n.`updated_by`;
SET @inserted_announcements := ROW_COUNT();

DROP TEMPORARY TABLE IF EXISTS `merge_operation_map`;
CREATE TEMPORARY TABLE `merge_operation_map` (`old_id` BIGINT UNSIGNED PRIMARY KEY, `new_id` BIGINT UNSIGNED NOT NULL UNIQUE);
SET @operation_base := (SELECT COALESCE(MAX(`id`), 0) FROM `operation_log`);
INSERT INTO `merge_operation_map` SELECT `id`, @operation_base + `id` FROM `petlink_incoming_20260912`.`operation_log`;
INSERT INTO `operation_log`
(`id`,`business_type`,`business_id`,`operation_type`,`before_status`,`after_status`,`operator_id`,`reason`,`created_at`)
SELECT om.`new_id`,n.`business_type`,
       CASE n.`business_type`
           WHEN 'SYS_USER' THEN um_business.`new_id`
           WHEN 'RESCUE_CLUE' THEN cm.`new_id`
           WHEN 'RESCUE_TASK' THEN tm.`new_id`
           WHEN 'ANIMAL' THEN am.`new_id`
           WHEN 'ADOPTION_APPLICATION' THEN apm.`new_id`
           WHEN 'ANNOUNCEMENT' THEN anm.`new_id`
       END,
       n.`operation_type`,n.`before_status`,n.`after_status`,um_operator.`new_id`,n.`reason`,n.`created_at`
FROM `petlink_incoming_20260912`.`operation_log` n
JOIN `merge_operation_map` om ON om.`old_id` = n.`id`
JOIN `merge_user_map` um_operator ON um_operator.`old_id` = n.`operator_id`
LEFT JOIN `merge_user_map_secondary` um_business ON n.`business_type` = 'SYS_USER' AND um_business.`old_id` = n.`business_id`
LEFT JOIN `merge_clue_map` cm ON n.`business_type` = 'RESCUE_CLUE' AND cm.`old_id` = n.`business_id`
LEFT JOIN `merge_task_map` tm ON n.`business_type` = 'RESCUE_TASK' AND tm.`old_id` = n.`business_id`
LEFT JOIN `merge_animal_map` am ON n.`business_type` = 'ANIMAL' AND am.`old_id` = n.`business_id`
LEFT JOIN `merge_application_map` apm ON n.`business_type` = 'ADOPTION_APPLICATION' AND apm.`old_id` = n.`business_id`
LEFT JOIN `merge_announcement_map` anm ON n.`business_type` = 'ANNOUNCEMENT' AND anm.`old_id` = n.`business_id`;
SET @inserted_operations := ROW_COUNT();

-- Exact row-count assertions. Any mismatch aborts the script before finalize.
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_clues - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`rescue_clue`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_tasks - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`rescue_task`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_rescue_records - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`rescue_record`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_animals - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`animal`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_health_records - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`health_record`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_applications - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`adoption_application`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_adoption_records - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`adoption_record`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_followups - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`follow_up_record`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_favorites - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`favorite`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_announcements - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`announcement`));
INSERT INTO `merge_assert_zero`
SELECT ABS(@inserted_operations - (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`operation_log`));

SELECT
    @inserted_users AS `new_users`,
    @inserted_clues AS `rescue_clues`,
    @inserted_tasks AS `rescue_tasks`,
    @inserted_rescue_records AS `rescue_records`,
    @inserted_animals AS `animals`,
    @inserted_health_records AS `health_records`,
    @inserted_applications AS `adoption_applications`,
    @inserted_adoption_records AS `adoption_records`,
    @inserted_followups AS `followups`,
    @inserted_favorites AS `favorites`,
    @inserted_announcements AS `announcements`,
    @inserted_operations AS `operation_logs`,
    (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`rescue_clue_image`)
      + (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`animal_image`)
      + (SELECT COUNT(*) FROM `petlink_incoming_20260912`.`follow_up_image`) AS `image_rows_excluded_missing_binary_files`;

ROLLBACK;
