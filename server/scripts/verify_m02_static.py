from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "src/main/java/com/petlink"

def text(rel):
    return (java / rel).read_text(encoding="utf-8")

def require(haystack, needles, label):
    missing = [n for n in needles if n not in haystack]
    if missing:
        raise SystemExit(f"FAILED {label}: missing {missing}")

controller = text("modules/clue/controller/RescueClueController.java")
admin = text("modules/clue/controller/RescueClueAdminController.java")
media = text("modules/clue/controller/RescueClueMediaController.java")
query = text("modules/clue/service/RescueClueQueryService.java")
idempotency = text("modules/clue/service/ClueIdempotencyStore.java")
tx = text("modules/clue/service/RescueClueTransactionalService.java")
binding = text("modules/clue/service/ClueFileBindingService.java")
cors = text("config/SecurityConfig.java")
cleanup = text("infrastructure/file/service/BoundTemporaryFileCleanupService.java")
image_mapper = text("modules/clue/mapper/RescueClueImageMapper.java")

require(controller, [
    '@PostMapping("/idempotency-keys")', '@PostMapping', '@GetMapping("/me")',
    '@GetMapping("/{clueId}")', '@PatchMapping("/{clueId}")',
    '@PostMapping("/{clueId}/images")', '@DeleteMapping("/{clueId}/images/{imageId}")',
    '@PostMapping("/{clueId}/withdraw")', 'Idempotency-Key'
], "member endpoints")
require(admin, ['@RequestMapping("/api/admin/rescue-clues")', '@GetMapping', '@PostMapping("/{clueId}/audit")'], "admin endpoints")
require(media, ['@RequestMapping("/api/media/rescue-clue-images")', '@GetMapping("/{imageId}")', 'ResponseEntity<byte[]>'], "media endpoint")
require(query, ['PENDING_REVIEW', 'REJECTED', 'WITHDRAWN', 'WAITING_ACCEPT', 'CONVERTED', 'CLOSED', 'RESOURCE_NOT_FOUND', 'existsTaskForRescuer'], "visibility/status")
require(idempotency, ['ConcurrentHashMap', 'compute(', 'UNUSED', 'PROCESSING', 'SUCCEEDED', 'IDEMPOTENCY_REQUEST_IN_PROGRESS', 'IDEMPOTENCY_KEY_REUSED'], "idempotency")
require(tx, ['"CREATE"', '"WITHDRAW"', '"AUDIT_APPROVE"', '"AUDIT_REJECT"', 'prepareBindings(', 'copyPrepared(', 'boolean changed = false', 'if (changed)', 'selectMaxSortOrder'], "transaction/log/PATCH flow")
require(binding, ['selectForUpdateByTokens', 'prepareBindings', 'copyPrepared', 'createdFormalPaths', 'STATUS_COMMITTED'], "file binding")
require(cors, ['"Idempotency-Key"'], "CORS")
require(image_mapper, ['COALESCE(MAX(sort_order), 0)', 'countByClueId'], "image append ordering")
require(cleanup, ['associationVerifier.exists', 'existsRegularFileInsideRoot', 'Preserving BOUND temporary file for manual inspection'], "safe BOUND cleanup")

for script_name in ("smoke-m01.ps1", "smoke-m02.ps1"):
    data = (root / "scripts" / script_name).read_bytes()
    if any(byte > 0x7F for byte in data):
        raise SystemExit(f"FAILED {script_name}: smoke scripts must remain ASCII for Windows PowerShell 5.1")

smoke_m02 = (root / "scripts/smoke-m02.ps1").read_text(encoding="ascii")
require(smoke_m02, ['WITHDRAWN', 'REJECTED', 'WAITING_ACCEPT', 'REJECT requires rejectReason', 'same-value PATCH'], "M02 smoke state coverage")

print("M02 static implementation checks: PASSED")
print("member endpoints: 8")
print("admin endpoints: 2")
print("media endpoints: 1")
