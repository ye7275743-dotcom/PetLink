-- PetLink 2026-09-18 demo cleanup
--
-- Purpose: remove the historical smoke/acceptance batches that were created
-- during development.  It deliberately keeps the three original demo
-- accounts (admin/rescuer/user) and any accounts that do not match the
-- explicit test-account patterns below.
--
-- Run only against the backed-up 2026-09-18 server snapshot.  The script is
-- transactional and prints the rows selected for deletion before COMMIT.

SET NAMES utf8mb4;
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS cleanup_users;
DROP TEMPORARY TABLE IF EXISTS cleanup_users_applicant;
DROP TEMPORARY TABLE IF EXISTS cleanup_users_reviewer;
DROP TEMPORARY TABLE IF EXISTS cleanup_users_operator;
DROP TEMPORARY TABLE IF EXISTS cleanup_users_business;
DROP TEMPORARY TABLE IF EXISTS cleanup_clues;
DROP TEMPORARY TABLE IF EXISTS cleanup_tasks;
DROP TEMPORARY TABLE IF EXISTS cleanup_animals;
DROP TEMPORARY TABLE IF EXISTS cleanup_apps;
DROP TEMPORARY TABLE IF EXISTS cleanup_adoption_records;
DROP TEMPORARY TABLE IF EXISTS cleanup_followups;
DROP TEMPORARY TABLE IF EXISTS cleanup_announcements;
DROP TEMPORARY TABLE IF EXISTS cleanup_logs;

CREATE TEMPORARY TABLE cleanup_users (
    id BIGINT UNSIGNED PRIMARY KEY
);

INSERT INTO cleanup_users (id)
SELECT id
FROM sys_user
WHERE account LIKE 'releasecheck%'
   OR account LIKE 'deleteqa%'
   OR account LIKE 'roleqa%'
   OR account LIKE 'e2emtn%'
   OR account LIKE 'petlink_%'
   OR account LIKE 'm02_%'
   OR account LIKE 'm03_%'
   OR account LIKE 'dbg_%'
   OR account LIKE 'm05%';

-- MySQL cannot reopen the same temporary table twice in one join.  Keep
-- dedicated copies for the independent foreign-key roles used below.
CREATE TEMPORARY TABLE cleanup_users_applicant (id BIGINT UNSIGNED PRIMARY KEY);
INSERT INTO cleanup_users_applicant SELECT id FROM cleanup_users;
CREATE TEMPORARY TABLE cleanup_users_reviewer (id BIGINT UNSIGNED PRIMARY KEY);
INSERT INTO cleanup_users_reviewer SELECT id FROM cleanup_users;
CREATE TEMPORARY TABLE cleanup_users_operator (id BIGINT UNSIGNED PRIMARY KEY);
INSERT INTO cleanup_users_operator SELECT id FROM cleanup_users;
CREATE TEMPORARY TABLE cleanup_users_business (id BIGINT UNSIGNED PRIMARY KEY);
INSERT INTO cleanup_users_business SELECT id FROM cleanup_users;

-- IDs 1-6 are the first synthetic demo batch; the remaining matches are
-- named M01/M02/M03/M04/M05, Debug, concurrency or cross-platform smoke data.
CREATE TEMPORARY TABLE cleanup_clues (
    id BIGINT UNSIGNED PRIMARY KEY
);

INSERT INTO cleanup_clues (id)
SELECT id
FROM rescue_clue
WHERE id BETWEEN 1 AND 26
  AND (
      id <= 6
      OR location LIKE '%验收%'
      OR location LIKE '%并发%'
      OR location LIKE '%Smoke%'
      OR location LIKE '%Debug%'
      OR location = '1'
      OR location LIKE 'Nanjing %'
  );

CREATE TEMPORARY TABLE cleanup_tasks (
    id BIGINT UNSIGNED PRIMARY KEY
);
INSERT INTO cleanup_tasks (id)
SELECT id FROM rescue_task WHERE clue_id IN (SELECT id FROM cleanup_clues);

CREATE TEMPORARY TABLE cleanup_animals (
    id BIGINT UNSIGNED PRIMARY KEY
);
INSERT INTO cleanup_animals (id)
SELECT id FROM animal WHERE rescue_task_id IN (SELECT id FROM cleanup_tasks);

CREATE TEMPORARY TABLE cleanup_apps (
    id BIGINT UNSIGNED PRIMARY KEY
);
INSERT INTO cleanup_apps (id)
SELECT DISTINCT a.id
FROM adoption_application a
LEFT JOIN cleanup_animals ca ON ca.id = a.animal_id
LEFT JOIN cleanup_users_applicant cu ON cu.id = a.user_id
LEFT JOIN cleanup_users_reviewer cr ON cr.id = a.reviewer_id
WHERE ca.id IS NOT NULL OR cu.id IS NOT NULL OR cr.id IS NOT NULL;

CREATE TEMPORARY TABLE cleanup_adoption_records (
    id BIGINT UNSIGNED PRIMARY KEY
);
INSERT INTO cleanup_adoption_records (id)
SELECT DISTINCT r.id
FROM adoption_record r
LEFT JOIN cleanup_apps ca ON ca.id = r.application_id
LEFT JOIN cleanup_animals an ON an.id = r.animal_id
LEFT JOIN cleanup_users cu ON cu.id = r.user_id
WHERE ca.id IS NOT NULL OR an.id IS NOT NULL OR cu.id IS NOT NULL;

CREATE TEMPORARY TABLE cleanup_followups (
    id BIGINT UNSIGNED PRIMARY KEY
);
INSERT INTO cleanup_followups (id)
SELECT DISTINCT f.id
FROM follow_up_record f
LEFT JOIN cleanup_adoption_records ar ON ar.id = f.adoption_record_id
LEFT JOIN cleanup_users cu ON cu.id = f.submitter_id
WHERE ar.id IS NOT NULL OR cu.id IS NOT NULL;

CREATE TEMPORARY TABLE cleanup_announcements (
    id BIGINT UNSIGNED PRIMARY KEY
);
INSERT INTO cleanup_announcements (id)
SELECT id
FROM announcement
WHERE id IN (3, 4, 5, 6)
   OR title LIKE '%验收%'
   OR title LIKE '%跨平台%'
   OR title LIKE '%deleteqa%';

CREATE TEMPORARY TABLE cleanup_logs (
    id BIGINT UNSIGNED PRIMARY KEY
);
INSERT INTO cleanup_logs (id)
SELECT DISTINCT l.id
FROM operation_log l
LEFT JOIN cleanup_users_operator u ON u.id = l.operator_id
LEFT JOIN cleanup_users_business su ON l.business_type = 'SYS_USER' AND su.id = l.business_id
LEFT JOIN cleanup_clues c ON l.business_type = 'RESCUE_CLUE' AND c.id = l.business_id
LEFT JOIN cleanup_tasks t ON l.business_type = 'RESCUE_TASK' AND t.id = l.business_id
LEFT JOIN cleanup_animals a ON l.business_type = 'ANIMAL' AND a.id = l.business_id
LEFT JOIN cleanup_apps ap ON l.business_type = 'ADOPTION_APPLICATION' AND ap.id = l.business_id
LEFT JOIN cleanup_announcements an ON l.business_type = 'ANNOUNCEMENT' AND an.id = l.business_id
WHERE u.id IS NOT NULL OR su.id IS NOT NULL OR c.id IS NOT NULL OR t.id IS NOT NULL
   OR a.id IS NOT NULL OR ap.id IS NOT NULL OR an.id IS NOT NULL;

SELECT 'selected_users' AS item, COUNT(*) AS rows_selected FROM cleanup_users
UNION ALL SELECT 'selected_clues', COUNT(*) FROM cleanup_clues
UNION ALL SELECT 'selected_tasks', COUNT(*) FROM cleanup_tasks
UNION ALL SELECT 'selected_animals', COUNT(*) FROM cleanup_animals
UNION ALL SELECT 'selected_adoption_applications', COUNT(*) FROM cleanup_apps
UNION ALL SELECT 'selected_adoption_records', COUNT(*) FROM cleanup_adoption_records
UNION ALL SELECT 'selected_followups', COUNT(*) FROM cleanup_followups
UNION ALL SELECT 'selected_announcements', COUNT(*) FROM cleanup_announcements;

DELETE FROM follow_up_image
WHERE follow_up_id IN (SELECT id FROM cleanup_followups);
DELETE FROM follow_up_record
WHERE id IN (SELECT id FROM cleanup_followups);
DELETE FROM adoption_record
WHERE id IN (SELECT id FROM cleanup_adoption_records);
DELETE FROM adoption_application
WHERE id IN (SELECT id FROM cleanup_apps);
DELETE f
FROM favorite f
LEFT JOIN cleanup_animals a ON a.id = f.animal_id
LEFT JOIN cleanup_users u ON u.id = f.user_id
WHERE a.id IS NOT NULL OR u.id IS NOT NULL;
DELETE h
FROM health_record h
LEFT JOIN cleanup_animals a ON a.id = h.animal_id
LEFT JOIN cleanup_users u ON u.id = h.recorder_id
WHERE a.id IS NOT NULL OR u.id IS NOT NULL;
DELETE FROM animal_image
WHERE animal_id IN (SELECT id FROM cleanup_animals);
DELETE r
FROM rescue_record r
LEFT JOIN cleanup_tasks t ON t.id = r.task_id
LEFT JOIN cleanup_users u ON u.id = r.recorder_id
WHERE t.id IS NOT NULL OR u.id IS NOT NULL;
DELETE FROM animal
WHERE id IN (SELECT id FROM cleanup_animals);
DELETE FROM rescue_task
WHERE id IN (SELECT id FROM cleanup_tasks);
DELETE FROM rescue_clue_image
WHERE clue_id IN (SELECT id FROM cleanup_clues);
DELETE FROM rescue_clue
WHERE id IN (SELECT id FROM cleanup_clues);
DELETE FROM operation_log
WHERE id IN (SELECT id FROM cleanup_logs);
DELETE FROM announcement
WHERE id IN (SELECT id FROM cleanup_announcements);
DELETE FROM temporary_file
WHERE owner_id IN (SELECT id FROM cleanup_users);
DELETE FROM sys_user
WHERE id IN (SELECT id FROM cleanup_users);

-- Keep this script safe to rehearse: change the final statement to COMMIT
-- only after reviewing the selected-row output above.
ROLLBACK;
