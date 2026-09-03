# M07 Favorite and Announcement Management - Frozen

## Baseline protection

M07 was implemented on the accepted M01-M06 Frozen workspace. It uses the existing Frozen `favorite`, `announcement`, `animal`, `animal_image`, and `operation_log` structures; no schema migration or earlier API contract was introduced.

## Implemented API surface

### Favorite

1. `POST /api/animals/{animalId}/favorite`
2. `DELETE /api/animals/{animalId}/favorite`
3. `GET /api/favorites/me`

### Announcement

4. `GET /api/announcements`
5. `GET /api/announcements/{announcementId}`
6. `GET /api/admin/announcements`
7. `GET /api/admin/announcements/{announcementId}`
8. `POST /api/admin/announcements`
9. `PATCH /api/admin/announcements/{announcementId}`
10. `POST /api/admin/announcements/{announcementId}/publish`
11. `POST /api/admin/announcements/{announcementId}/withdraw`

## Frozen favorite rules

- Only authenticated USER/RESCUER actors can manage their own favorites.
- An existing favorite is returned idempotently before the current Animal status is checked.
- A new favorite locks the Animal with `FOR SHARE` and requires status `AVAILABLE`.
- Only the named unique constraint `uk_favorite_user_animal` is eligible for concurrent duplicate recovery.
- Two concurrent creates return HTTP 201 and HTTP 200 with the same `favoriteId` and one database row.
- Delete is idempotent and repeated deletion remains successful with `favorited=false`.
- A favorite relationship does not grant AnimalImage access. A historical favorite can remain visible while a now-hidden image URL is omitted.

## Frozen announcement rules

- The state machine is terminal and one-way: `DRAFT -> PUBLISHED -> WITHDRAWN`.
- Public list/detail expose only `PUBLISHED`; DRAFT and WITHDRAWN details are hidden as `40401`.
- Admin create, edit, publish, withdraw, list, and detail require ADMIN.
- Public/admin list and detail use four fixed response projections; public responses do not leak `version` or management metadata.
- PATCH distinguishes field presence from a null/missing value, supports one-field edits, and rejects a version-only body.
- DRAFT and PUBLISHED may be edited; WITHDRAWN is immutable and cannot be republished.
- PATCH, publish, and withdraw require optimistic-lock `version`; a stale version maps to `40903`.
- The first `publishedAt` is preserved when the announcement is withdrawn.
- CREATE, PUBLISH, and WITHDRAW append same-transaction ANNOUNCEMENT operation logs.

## Verification evidence

Completed on 2026-09-02 with Amazon Corretto 17.0.20.1, MySQL 8.0.46, and PowerShell 7.6.5:

```text
M07 endpoints           11
M07-focused tests       49
Full Maven suite        226 passed / 0 failed / 0 errors / 0 skipped
BUILD SUCCESS
verify_schema.py        PASSED (16 tables, 52 CHECK, 26 FK, 17 UNIQUE)
verify_m02-m07_static   PASSED
smoke-m07.ps1           PowerShell AST parse PASSED; ASCII-only
smoke-m07.ps1           12/12 PASSED against freshly initialized real MySQL
```

The 12-stage smoke covered favorite create/replay, named-unique concurrent recovery, historical visibility and media projection, idempotent deletion, all announcement states, public hiding, field-presence PATCH, optimistic conflict, filtering, and role enforcement.

The database audit chain for the smoke announcement was verified directly:

```text
CREATE    NULL       -> DRAFT
PUBLISH   DRAFT      -> PUBLISHED
WITHDRAW  PUBLISHED  -> WITHDRAWN
```

Existing frontend acceptance was rerun after M07 completion:

```text
PC tests              6/6 passed
Mobile tests          8/8 passed
verify_frontend.py    PASSED
PC production build   PASSED
Mobile H5 build       PASSED
```

The clean npm install reported dependency-audit warnings in the existing pinned frontend dependency trees (PC: 4; Mobile: 37), and the PC build reported a large Element Plus chunk warning. They did not affect compilation or the M07 functional contract, so they are recorded as dependency/performance backlog instead of being changed with a potentially breaking forced upgrade during the freeze.

## Frozen conclusion

M07 has passed source, automated, static, concurrency, authorization, optimistic-lock, audit-log, frontend-contract, production-build, and clean real-MySQL acceptance. This conclusion records the M07 freeze point; the current workspace status, including the later M08 freeze, is maintained in the root and server READMEs.
