# PetLink coding workspace - M01-M08 and Chinese Frontend Frozen

This workspace contains the Frozen implementations of M01-M08. Earlier Frozen modules are protected by automated, static, and real-database regression checks.

## Status

- Sprint 0 + M01: **Frozen by user acceptance**
- M02 rescue clue: **Frozen by user acceptance**
- M03 rescue task: **Frozen by user acceptance**
- M04 animal: **Frozen after automated tests and real-MySQL smoke acceptance**
- M05 adoption: **Frozen after automated, static, and real-MySQL smoke acceptance**
- M06 follow-up: **Frozen after permanent-idempotency concurrency acceptance and 11/11 clean real-MySQL smoke**
- M07 favorite + announcement: **Frozen after 226 automated tests, all static gates, 12/12 clean real-MySQL smoke, audit-chain verification, and frontend regression/build acceptance**
- M08 administration: **Frozen after 267 automated tests, all static gates, 12/12 clean real-MySQL smoke, SYS_USER audit verification, and frontend regression/build acceptance**
- Frontend V1.0 Chinese delivery + M08 contract alignment: **Frozen after clean npm installation, 38/38 frontend tests (PC 20 + Mobile 18), semantic SVG icon upgrade, static verification, cross-platform E2E, browser acceptance, and PC/Mobile production builds**
- Full-stack optimization baseline (2026-09-03): **accepted after request correlation, security headers, race-safe pagination, offline UX, responsive tables, lazy images, 269 backend tests, 38/38 frontend tests, and 40/40 cross-platform E2E**

M06-M08 final evidence is recorded under `server/docs/M06-IMPLEMENTATION-FROZEN.md` through `server/docs/M08-IMPLEMENTATION-FROZEN.md`. The planned M01-M08 V1.0 backend scope is complete.

The frontend source lives in `petlink-pc/` and `petlink-mobile/`. Frontend verification is documented in `README-FRONTEND.md` and `docs/frontend/`.

## V1.0 final acceptance evidence

- Implemented RTM: `docs/PetLink-需求追踪矩阵-v1.2-Implemented-Frozen.xlsx` (52/52 traced requirements, 72/72 implemented/tested APIs).
- Final summary: `docs/testing/FINAL-ACCEPTANCE-SUMMARY.md`.
- UI optimization acceptance: `docs/testing/UI-OPTIMIZATION-ACCEPTANCE-20260903.md`.
- Full-stack optimization acceptance: `docs/testing/FULL-STACK-OPTIMIZATION-ACCEPTANCE-20260903.md`.
- Icon system acceptance: `docs/testing/ICON-SYSTEM-ACCEPTANCE-20260903.md`.
- Functional, exception/security, performance, and compatibility reports: `docs/testing/`.
- 50-user performance acceptance: query P95 52.696 ms and update P95 83.827 ms, both with 150/150 successful requests.
- Delivery checklist: `docs/DELIVERY-CHECKLIST-V1.0.md`.
- Current project state: `project_state(PetLink v1.0需求定义).md`.

## M04 implemented API surface

1. `GET /api/animals`
2. `GET /api/animals/{animalId}`
3. `GET /api/animals/responsible/me`
4. `PATCH /api/animals/{animalId}`
5. `POST /api/animals/{animalId}/images`
6. `DELETE /api/animals/{animalId}/images/{imageId}`
7. `GET /api/media/animal-images/{imageId}`
8. `POST /api/animals/{animalId}/health-records`
9. `GET /api/animals/{animalId}/health-records`
10. `POST /api/animals/{animalId}/status-actions`

## Frozen M04 rules implemented

- VISITOR/USER public list and public detail expose only `AVAILABLE` animals.
- RESCUER can additionally see animals whose source RescueTask belongs to that rescuer; ADMIN can see all animals.
- hidden animal resources return `40401` rather than leaking existence through 403.
- public health-record projection hides `recorderId`; responsible RESCUER/ADMIN receives the full projection.
- Animal `rescueTaskId` is returned only to responsible RESCUER/ADMIN.
- basic PATCH requires `version` and uses optimistic locking; only changed fields are written.
- image append is **max 9 per append request**, with **no lifetime 9-image cap**; order starts at `MAX(sort_order)+1`.
- M04 image/health write lock order is `Animal FOR UPDATE -> plain RescueTask responsibility read -> TemporaryFile(id ASC)` where temp files are involved.
- image deletion reindexes following rows in ascending order and deletes the physical formal file after DB commit.
- final adopter media authorization is supported through `adoption_record`, without making an ADOPTED Animal public again.
- HealthRecord is append-only and does not auto-update `Animal.health_condition`.
- legal state actions only: `TO_OBSERVING`, `OPEN_ADOPTION`, `SUSPEND_ADOPTION`, `RESUME_ADOPTION`.
- state changes use status + version conditional updates and write the Frozen ANIMAL OperationLog type in the same transaction.
- `AVAILABLE -> ADOPTED` is deliberately not exposed in M04; it remains reserved for M05 adoption approval.

## Verification evidence in this package

Static checks executed in the generation environment:

```text
verify_schema.py      PASSED
verify_m02_static.py  PASSED
verify_m03_static.py  PASSED
verify_m04_static.py  PASSED

physical tables       16
CHECK                  52
FK                     26
UNIQUE                 17
M03-focused @Test      26
M04-focused @Test      29
Total @Test methods    103
Maven test result      103 passed / 0 failed / 0 errors / 0 skipped
Build result           BUILD SUCCESS
```

The complete Maven test suite was executed during the workspace audit on 2026-09-01. The real-MySQL M04 smoke flow subsequently passed all 15 stages and ended with `M04 smoke flow PASSED. animal=3 AVAILABLE`.

After selectively merging M05 on top of this Frozen baseline, the complete suite was executed again:

```text
verify_schema.py      PASSED
verify_m02_static.py  PASSED
verify_m03_static.py  PASSED
verify_m04_static.py  PASSED
verify_m05_static.py  PASSED
Total @Test methods    137
Maven test result      137 passed / 0 failed / 0 errors / 0 skipped
Build result           BUILD SUCCESS
M05 real-MySQL smoke   PASSED
```

After selectively merging M06 and implementing M07, the complete workspace was accepted again on 2026-09-02:

```text
verify_schema.py       PASSED (16 tables, 52 CHECK, 26 FK, 17 UNIQUE)
verify_m02_static.py   PASSED
verify_m03_static.py   PASSED
verify_m04_static.py   PASSED
verify_m05_static.py   PASSED
verify_m06_static.py   PASSED
verify_m07_static.py   PASSED
Total @Test methods    226
Maven test result      226 passed / 0 failed / 0 errors / 0 skipped
Build result           BUILD SUCCESS
M06 clean MySQL smoke  11/11 PASSED, including concurrent idempotency
M07 clean MySQL smoke  12/12 PASSED, including concurrent favorite recovery
Announcement audit     CREATE -> PUBLISH -> WITHDRAW verified in operation_log
Frontend tests         PC 6/6 + Mobile 8/8 PASSED
Frontend static/build  PASSED for PC and Mobile H5
```

After M08 implementation, the entire workspace was accepted once more on a freshly initialized database:

```text
verify_schema.py       PASSED (16 tables, 52 CHECK, 26 FK, 17 UNIQUE)
verify_m02-m08_static  PASSED
Total @Test methods    264
Maven test result      264 passed / 0 failed / 0 errors / 0 skipped
Build result           BUILD SUCCESS
Clean MySQL sequence   M06 11/11 + M07 12/12 + M08 12/12 PASSED
M08 operation logs     DISABLE + ENABLE + PROMOTE_RESCUER verified
Frontend tests         PC 8/8 + Mobile 8/8 PASSED
Frontend static/build  PASSED for PC and Mobile H5
```

After the Frozen-compatible Chinese localization pass, post-freeze profile/overview/file-governance additions, and the rescuer task-page refresh fix, frontend acceptance was rerun without changing API or database contracts:

```text
Frontend tests         PC 20/20 + Mobile 18/18 PASSED
Frontend static/build  PASSED for PC and Mobile H5
Cross-platform E2E     40/40 checks PASSED (USER + ADMIN + RESCUER)
Browser visible text   PC/Mobile task and management pages PASSED
Browser console        no errors; one non-blocking UniApp dependency deprecation warning
```

## Local acceptance order

在 macOS/Linux 上可直接运行 `./scripts/verify-all.sh`，一次执行后端 269 项测试、SQL/M02～M08 静态门禁、PC 20/20、Mobile 18/18、两端生产构建、前端静态检查和交付包哈希核对。脚本不包含需要外部 MySQL 服务的真实冒烟；跨平台真实业务闭环另行运行 `PETLINK_E2E_OPERATOR_PASSWORD='...' node scripts/cross-platform-e2e.mjs`，也不替代正式部署/真机现场验收。

Use JDK 17 + MySQL 8.0.16+:

```powershell
cd server

.\mvnw.cmd test
python .\scripts\verify_schema.py
python .\scripts\verify_m02_static.py
python .\scripts\verify_m03_static.py
python .\scripts\verify_m04_static.py
python .\scripts\verify_m05_static.py
python .\scripts\verify_m06_static.py
python .\scripts\verify_m07_static.py
python .\scripts\verify_m08_static.py

.\mvnw.cmd spring-boot:run
.\scripts\smoke-m01.ps1
.\scripts\smoke-m02.ps1
.\scripts\smoke-m03.ps1
.\scripts\smoke-m04.ps1
.\scripts\smoke-m05.ps1
.\scripts\smoke-m06.ps1
.\scripts\smoke-m07.ps1
.\scripts\smoke-m08.ps1
```

`smoke-m04.ps1` is ASCII-only for Windows PowerShell 5.1 and creates its own M02/M03 prerequisite business chain before exercising M04.

`smoke-m05.ps1` is also ASCII-only. It creates its own rescue-success and three-animals prerequisite chain, then covers WITHDRAWN, REJECTED, APPROVED, INVALIDATED, AdoptionRecord, final-adopter media access, and responsible-RESCUER overview. The real-MySQL flow has passed and M05 is Frozen.

`smoke-m06.ps1` and `smoke-m07.ps1` are ASCII-only and self-create their business prerequisites. Both passed consecutively against a freshly initialized MySQL 8.0.46 database. Their final Frozen details are under `server/docs/`.

`smoke-m08.ps1` is also ASCII-only. Final acceptance ran M06, M07, and M08 consecutively against a newly initialized MySQL 8.0.46 database; all stages passed.

## Post-freeze cross-cutting additions

The following items were completed after the module Frozen snapshots and covered by focused regression tests:

- PC `/profile` personal profile page with nickname and phone editing;
- PC responsible-animal adoption overview;
- Mobile adoption overview response-field alignment (`pendingApplicationCount`);
- Chinese network/error fallbacks and protected-image 404 placeholder behavior;
- 24-hour formal-file orphan scanning and scheduled retry cleanup for residual `BOUND` temporary-file rows.

These additions preserve the Frozen API/database contracts. Deployment still needs operational observation of scheduled cleanup logs and production file permissions.
