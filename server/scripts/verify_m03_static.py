from pathlib import Path
import re, sys

root = Path(__file__).resolve().parents[1]
java = root / "src" / "main" / "java" / "com" / "petlink"
sql = root.parent / "sql" / "petlink.sql"

required_files = [
    java / "modules/rescue/controller/RescueTaskIntakeController.java",
    java / "modules/rescue/controller/RescueTaskController.java",
    java / "modules/rescue/controller/RescueTaskAdminController.java",
    java / "modules/rescue/service/RescueTaskTransactionalService.java",
    java / "modules/rescue/service/RescueTaskQueryService.java",
    java / "modules/rescue/service/RescueRequestNormalizer.java",
    java / "modules/rescue/mapper/RescueTaskMapper.java",
    java / "modules/rescue/mapper/RescueRecordMapper.java",
    java / "modules/animal/service/AnimalFileBindingService.java",
]

errors=[]
for p in required_files:
    if not p.exists(): errors.append(f"missing {p.relative_to(root)}")

all_text="\n".join(p.read_text(encoding="utf-8") for p in required_files if p.exists())
needles=[
    'waiting-acceptance', '/{clueId}/accept', '/{taskId}/start', '/{taskId}/records', '/{taskId}/result',
    '/{taskId}/cancel', '/{taskId}/failure-resolution',
    'START_RESCUE', 'COMPLETE_RESCUE', 'RESCUE_FAILED', 'CANCEL_RESCUE',
    'CLOSE_AFTER_RESCUE', 'CLOSE_AFTER_FAILURE', 'ACCEPT_RESCUE', 'REOPEN',
    'selectForUpdate(taskId)', 'selectForUpdate(clueId)',
    'selectLatestForUpdateByClue', 'selectActiveForUpdateByClue',
    '"ANIMAL"',
]
for n in needles:
    if n not in all_text: errors.append(f"missing marker: {n}")

mapper=(java/"modules/rescue/mapper/RescueTaskMapper.java").read_text(encoding="utf-8")
if "status IN ('WAITING_START','IN_PROGRESS')" not in mapper: errors.append("active task current-read query missing")
if "ORDER BY created_at DESC, id DESC LIMIT 1 FOR UPDATE" not in mapper: errors.append("latest task current-read order missing")

sql_text=sql.read_text(encoding="utf-8")
for n in ["active_clue_id", "uk_rescue_task_active_clue", "CREATE TABLE `rescue_record`", "CREATE TABLE `animal`", "CREATE TABLE `health_record`"]:
    if n not in sql_text: errors.append(f"SQL missing: {n}")

# Delivery regression guard: the M02 test must keep the import needed by its concurrency assertion.
m02_test = root / "src/test/java/com/petlink/modules/clue/RescueClueTransactionalServiceTest.java"
if "import com.petlink.modules.clue.vo.StateActionResponse;" not in m02_test.read_text(encoding="utf-8"):
    errors.append("M02 regression: StateActionResponse import missing")

# Windows PowerShell 5.1: scripts must stay ASCII and all potentially scalar collections must be wrapped in @(...).Count.
for name, required_safe in {
    "smoke-m02.ps1": [
        '@($mine.data.records | Where-Object',
        '@($queue.data.records | Where-Object',
        '@($detail.data.images).Count',
    ],
    "smoke-m03.ps1": [
        '@($waiting.data.records | Where-Object',
        '@($records.data).Count',
        '@($success.data.animalIds).Count',
        '@($taskDetail.data.animals).Count',
        '@($mine.data.records | Where-Object',
    ],
}.items():
    p=root/"scripts"/name
    if not p.exists():
        errors.append(f"{name} missing")
        continue
    raw=p.read_bytes()
    if any(b > 127 for b in raw): errors.append(f"{name} contains non-ASCII bytes")
    text=raw.decode("ascii")
    for marker in required_safe:
        if marker not in text: errors.append(f"{name} missing safe collection marker: {marker}")

smoke=(root/"scripts/smoke-m03.ps1")
if smoke.exists():
    text=smoke.read_text(encoding="ascii")
    for n in ['FAILED', 'REOPEN', 'CANCELED', 'CLOSE', 'SUCCESS', 'initialHealthRecord']:
        if n not in text: errors.append(f"smoke missing flow marker: {n}")

# Source-count tests precisely; do not present this as runtime Maven evidence.
total_test_count=0
m03_test_count=0
test_root=root/"src/test/java"
for p in test_root.rglob("*.java"):
    c=len(re.findall(r"@Test\b", p.read_text(encoding="utf-8")))
    total_test_count += c
    if "/modules/rescue/" in p.as_posix():
        m03_test_count += c

security_test=root/"src/test/java/com/petlink/modules/rescue/RescueTaskControllerSecurityWebTest.java"
if not security_test.exists(): errors.append("M03 controller security Web test missing")
else:
    st=security_test.read_text(encoding="utf-8")
    for marker in [
        'roles = "USER"', 'roles = "RESCUER"', 'roles = "ADMIN"',
        'isUnauthorized()', 'isForbidden()',
        'new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)',
        'isNotFound()', 'jsonPath("$.code").value(40401)',
    ]:
        if marker not in st: errors.append(f"controller security test missing marker: {marker}")

print(f"M03-focused @Test methods: {m03_test_count}")
print(f"Total @Test methods: {total_test_count}")
print("M03 APIs: 10")
print("M03 critical flows: accept/start/record/FAILED/SUCCESS/cancel/failure-resolution")
if errors:
    print("FAILED")
    for e in errors: print(" -",e)
    sys.exit(1)
print("PASSED")
