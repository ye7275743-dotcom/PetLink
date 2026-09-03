# M06 Follow-Up Management - Freeze-Ready Candidate Rev2

## Status

M06 is implemented on top of the accepted M01-M05 Frozen business baseline. No Frozen API contract or database structure was changed.

The previous M06 Candidate was independently audited on 2026-09-01 with the following reported evidence:

- Maven tests: 172/172 PASS;
- M06-focused tests: 35;
- M02-M06 static verification: PASS;
- database structure: 16 tables, 52 CHECK, 26 FK, 17 UNIQUE;
- `smoke-m06.ps1`: PowerShell syntax accepted and ASCII-only;
- M01-M05 business code matched the Frozen baseline;
- all six M06 endpoints, permanent idempotency, visibility, file transaction rules and media authorization matched the Frozen API.

That evidence applies to the pre-Rev2 candidate. Rev2 adds five direct file-binding tests and strengthens the real-MySQL smoke with a concurrent idempotency pair, so the revised package must be rerun before M06 is marked Frozen.

## Implemented API surface

1. `POST /api/adoption-records/{adoptionRecordId}/follow-ups`
2. `GET /api/adoption-records/{adoptionRecordId}/follow-ups`
3. `GET /api/follow-ups/{followUpId}`
4. `GET /api/admin/follow-ups`
5. `GET /api/rescuer/follow-ups`
6. `GET /api/media/follow-up-images/{imageId}` (`COMMON-MEDIA-03`)

## Frozen M06 rules implemented

- `FollowUpRecord` is append-only: no status/version/updatedAt/deleted fields and no PATCH/DELETE endpoint.
- Only the final adopter in role USER/RESCUER can submit; `submitter_id` comes from the authenticated principal.
- The URL `adoptionRecordId` must belong to the current user; ownership failures are hidden as `40401`.
- `idempotencyKey` is a canonical UUID and is permanently scoped by `(submitter_id, idempotency_key)`.
- Existing permanent keys are looked up before old `imageTokens` are validated.
- Same key + same adoption record returns the first record with HTTP 200; same key + different adoption record returns `40908`.
- For a new request the transaction order is: AdoptionRecord ownership -> INSERT FollowUpRecord -> TemporaryFile rows by id ASC FOR UPDATE -> FollowUpImage in client token order -> BOUND -> formal copy -> COMMIT.
- Only the named unique constraint `uk_follow_up_submitter_idempotency` is eligible for concurrent duplicate recovery.
- A formal destination is registered for rollback cleanup before physical copy begins. If copy fails, rollback cleanup can therefore delete any partially created formal file; the temporary original is not cleaned until after commit.
- Committed BOUND temporary files are cleaned after commit through the shared verified cleanup service.
- Read visibility is exactly: final adopter, responsible RESCUER derived through `AdoptionRecord -> Animal -> RescueTask`, or ADMIN.
- A RESCUER `animalId` filter must itself be inside that rescuer's responsibility scope; otherwise `40401`.
- Follow-up history is ordered `created_at ASC, id ASC`; admin/rescuer pages are `created_at DESC, id DESC`.
- Follow-up media uses the shared canonicalized/symlink-safe file reader and the same three-party visibility rule.

## Rev2 direct file-transaction coverage

`FollowUpFileBindingServiceTest` now directly verifies:

1. the mapper lock contract contains `ORDER BY id ASC FOR UPDATE`;
2. returned lock order may be id order while persisted `sort_order=1..N` follows the original client token order;
3. `BOUND affectedRows=0` throws before any physical copy;
4. a copy failure leaves the temporary original untouched and registers the formal destination for rollback deletion;
5. temporary-file cleanup runs only after commit.

M06-focused source tests are now 40 and the total source-tree `@Test` count is 177.

## Rev2 real-database concurrency smoke

`smoke-m06.ps1` now has 11 stages. The new stage starts two PowerShell jobs behind the same UTC-time barrier and POSTs the same:

```text
submitter_id + idempotency_key + adoptionRecordId
```

Expected result:

```text
one request -> HTTP 201
one request -> HTTP 200
both -> same followUpId
history -> exactly one additional FollowUpRecord
same key + different adoptionRecordId -> HTTP 409 / 40908
```

This is the remaining real-MySQL concurrency acceptance evidence requested by audit.

## Static verification in the Rev2 generation environment

```text
verify_schema.py      PASSED
verify_m02_static.py  PASSED
verify_m03_static.py  PASSED
verify_m04_static.py  PASSED
verify_m05_static.py  PASSED
verify_m06_static.py  PASSED
M06-focused @Test     40 authored
Total @Test           177 authored
smoke-m06.ps1         ASCII-only
```

The current generation environment has no local Maven distribution/dependency cache and no PowerShell runtime, so it does not claim a Maven execution or a PowerShell AST parse for the revised files.

## Final M06 Frozen gate

Merge only the files listed in `M06-SELECTIVE-MERGE-MANIFEST.md` into the actual M01-M05 Frozen workspace. Do not overwrite the workspace root README, server README, or M05 evidence documents with files from a candidate package.

Then restart the M06 backend and run:

```powershell
cd server
.\mvnw.cmd test
python .\scripts\verify_schema.py
python .\scripts\verify_m02_static.py
python .\scripts\verify_m03_static.py
python .\scripts\verify_m04_static.py
python .\scripts\verify_m05_static.py
python .\scripts\verify_m06_static.py
.\scripts\smoke-m06.ps1
```

M06 may be promoted to Frozen only after the revised suite is green and the 11-stage real-MySQL smoke ends with `M06 smoke flow PASSED`.

The ADMIN/RESCUER page response assembler still performs one image query per follow-up row. This N+1 behavior is explicitly non-blocking for V1.0 and is deferred to the performance optimization phase.
