# M06 Follow-Up Management - Frozen

## Baseline protection

M06 was selectively merged into the accepted M01-M05 Frozen workspace according to `M06-SELECTIVE-MERGE-MANIFEST.md`. The merge did not replace the earlier Frozen tests, static verifiers, API documents, or database structure.

## Implemented API surface

1. `POST /api/adoption-records/{adoptionRecordId}/follow-ups`
2. `GET /api/adoption-records/{adoptionRecordId}/follow-ups`
3. `GET /api/follow-ups/{followUpId}`
4. `GET /api/admin/follow-ups`
5. `GET /api/rescuer/follow-ups`
6. `GET /api/media/follow-up-images/{imageId}` (`COMMON-MEDIA-03`)

## Frozen rules

- Follow-up records are append-only and have no PATCH or DELETE route.
- Only the final adopter in role USER/RESCUER can submit; ownership failures are hidden as `40401`.
- `idempotencyKey` is a canonical UUID permanently scoped by `(submitter_id, idempotency_key)`.
- An existing key is resolved before stale `imageTokens` are validated.
- Same key and same adoption record returns the first result with HTTP 200; reuse for another adoption record returns `40908`.
- New submission order is AdoptionRecord ownership check, FollowUpRecord insert, TemporaryFile locks by ascending id, FollowUpImage inserts in client order, BOUND transition, formal copy, and commit.
- Only `uk_follow_up_submitter_idempotency` participates in concurrent duplicate recovery.
- Visibility is limited to the final adopter, the responsible rescuer, and ADMIN.
- History is ordered by `created_at ASC, id ASC`; management pages use descending order.
- Follow-up media reuses canonicalized, symlink-safe file reading and the same three-party visibility rule.

## Verification evidence

Completed on 2026-09-02 with Amazon Corretto 17.0.20.1, MySQL 8.0.46, and PowerShell 7.6.5:

```text
M06-focused tests      40
Full Maven suite       226 passed / 0 failed / 0 errors / 0 skipped
BUILD SUCCESS
verify_schema.py       PASSED
verify_m02_static.py   PASSED
verify_m03_static.py   PASSED
verify_m04_static.py   PASSED
verify_m05_static.py   PASSED
verify_m06_static.py   PASSED
smoke-m06.ps1          11/11 PASSED against freshly initialized real MySQL
```

The clean-database smoke included a simultaneous two-request barrier for the same permanent idempotency key. It produced HTTP 201 and HTTP 200, returned one shared `followUpId`, left exactly one row, and rejected cross-record key reuse.

## Frozen conclusion

M06 has passed source, automated, static, concurrency, file-transaction, authorization, and clean real-MySQL acceptance. The former `M06-IMPLEMENTATION-CANDIDATE.md` is retained only as candidate history; this document is the current M06 status authority.

The management-list image query N+1 behavior remains a documented, non-blocking V1.0 performance item.
