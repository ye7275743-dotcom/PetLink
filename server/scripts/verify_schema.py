#!/usr/bin/env python3
"""Static verification for PetLink Frozen SQL.
This does not replace executing the SQL on MySQL 8.0.16+.
"""
from pathlib import Path
import re
import sys

sql_path = Path(__file__).resolve().parents[2] / "sql" / "petlink.sql"
sql = sql_path.read_text(encoding="utf-8")

expected_tables = [
    "sys_user", "rescue_clue", "rescue_clue_image", "rescue_task",
    "rescue_record", "animal", "animal_image", "health_record",
    "adoption_application", "adoption_record", "follow_up_record",
    "follow_up_image", "favorite", "announcement", "operation_log",
    "temporary_file",
]

tables = re.findall(r"CREATE\s+TABLE\s+`([^`]+)`", sql, re.I)
errors = []
if tables != expected_tables:
    errors.append(f"table list mismatch: {tables}")

required_snippets = [
    "GENERATED ALWAYS AS",
    "`active_clue_id` BIGINT UNSIGNED",
    "CONSTRAINT `uk_rescue_task_active_clue` UNIQUE (`active_clue_id`)",
    "CONSTRAINT `chk_rescue_task_status`",
    "CONSTRAINT `chk_temporary_file_binding`",
    "CONSTRAINT `fk_temporary_file_owner`",
    "CONSTRAINT `uk_temporary_file_token`",
]
for snippet in required_snippets:
    if snippet not in sql:
        errors.append(f"missing: {snippet}")

print(f"SQL: {sql_path}")
print(f"physical tables: {len(tables)}")
print(f"core business tables: {len(tables) - 1}")
print("technical support table: temporary_file")
check_count = len(re.findall(r"CONSTRAINT\s+`chk_", sql))
fk_count = len(re.findall(r"CONSTRAINT\s+`fk_", sql))
unique_count = len(re.findall(r"CONSTRAINT\s+`uk_", sql))

print(f"CHECK constraints: {check_count}")
print(f"FK constraints: {fk_count}")
print(f"UNIQUE constraints: {unique_count}")

if errors:
    print("FAILED")
    for err in errors:
        print(" -", err)
    sys.exit(1)
print("PASSED: Frozen SQL static structure checks")
