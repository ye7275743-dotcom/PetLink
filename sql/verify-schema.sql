-- PetLink Frozen 数据库初始化后的人工验收查询（MySQL 8.0.16+）
USE `petlink`;

-- 1) 应返回 16
SELECT COUNT(*) AS physical_table_count
FROM information_schema.tables
WHERE table_schema = 'petlink'
  AND table_type = 'BASE TABLE';

-- 2) 应完整返回下列 16 张物理表
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'petlink'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- 3) active_clue_id 应为 STORED GENERATED
SELECT table_name, column_name, data_type, is_nullable, extra, generation_expression
FROM information_schema.columns
WHERE table_schema = 'petlink'
  AND table_name = 'rescue_task'
  AND column_name = 'active_clue_id';

-- 4) 应存在 uk_rescue_task_active_clue
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = 'petlink'
  AND table_name = 'rescue_task'
  AND constraint_name = 'uk_rescue_task_active_clue';

-- 5) 汇总 FK / UNIQUE / CHECK 数量，便于和 Frozen SQL 做结构审计
SELECT constraint_type, COUNT(*) AS cnt
FROM information_schema.table_constraints
WHERE table_schema = 'petlink'
GROUP BY constraint_type
ORDER BY constraint_type;

-- 6) ADMIN 测试账号（执行 dev-seed-admin.sql 后应返回 1 行）
SELECT id, account, nickname, role_code, status
FROM sys_user
WHERE account = 'admin';
