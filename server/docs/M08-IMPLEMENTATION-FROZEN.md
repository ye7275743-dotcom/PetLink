# M08 Administration - Frozen

## Baseline protection

M08 was implemented as an independent `modules/admin` layer on the accepted M01-M07 Frozen workspace. It reuses the Frozen RescueTask, Animal, and AdoptionRecord response projections and introduces no database migration or duplicate business action.

## Implemented API surface

1. `GET /api/admin/users`
2. `GET /api/admin/users/{userId}`
3. `POST /api/admin/users/{userId}/enable`
4. `POST /api/admin/users/{userId}/disable`
5. `POST /api/admin/users/{userId}/promote-rescuer`
6. `GET /api/admin/rescue-tasks`
7. `GET /api/admin/animals`
8. `GET /api/admin/adoption-records`
9. `GET /api/admin/stats/overview`
10. `GET /api/admin/stats/trends`

## Frozen user-management rules

- All M08 routes require ADMIN.
- User summaries/details use fixed projections and never expose `passwordHash`.
- User details always include the four derived counters, including zero values.
- Enable and disable are conditional state updates and return `40901` when the prerequisite state no longer matches.
- An ADMIN cannot disable their own account (`40301`).
- Disabling a RESCUER changes only `sys_user`; existing rescue tasks remain untouched for explicit M03 handling.
- Promotion supports only `USER -> RESCUER`; it does not add demotion or ADMIN promotion routes.
- The existing JWT filter reloads status and role from `sys_user` on every protected request. Disable therefore invalidates an existing token immediately; enable restores it; promotion gives the existing token RESCUER authorization immediately.
- ENABLE, DISABLE, and PROMOTE_RESCUER write same-transaction SYS_USER operation logs.

## Frozen supervision and statistics rules

- Rescue-task supervision supports status, rescuerId, and clueId and returns the Frozen `TaskSummaryResponse`.
- Animal supervision supports status, species, and rescueTaskId and returns the Frozen `AnimalSummaryResponse`.
- Adoption-record supervision supports userId, animalId, and applicationId and returns the Frozen immutable `AdoptionRecordResponse`.
- All supervision lists use the Frozen descending sort orders.
- Overview always emits all six RescueClue states, five RescueTask states, five Animal states, and five AdoptionApplication states, even when a count is zero.
- AdoptionRecord and FollowUpRecord statistics are historical totals.
- Trends accept only DAY, use inclusive `from/to`, report `Asia/Shanghai`, count successful tasks by `finished_at`, count adoptions by `adopted_at`, and insert zero-valued points for dates without data.

## Verification evidence

Completed on 2026-09-02 with Amazon Corretto 17.0.20.1, MySQL 8.0.46, and PowerShell 7.6.5:

```text
M08 endpoints            10
M08-focused tests        38
Full Maven suite         264 passed / 0 failed / 0 errors / 0 skipped
BUILD SUCCESS
verify_schema.py         PASSED (16 tables, 52 CHECK, 26 FK, 17 UNIQUE)
verify_m02-m08_static    PASSED
smoke-m08.ps1            PowerShell AST parse PASSED; ASCII-only
clean real-MySQL smoke   M06 11/11 + M07 12/12 + M08 12/12 PASSED consecutively
```

The M08 smoke covered user filtering/projections, four derived counters, self-disable protection, immediate JWT disable/enable behavior, immediate promotion authorization, state conflicts, missing resources, all three supervision filters, complete zero-preserving overview maps, inclusive DAY points, and invalid trend requests.

Direct database verification confirmed:

```text
SYS_USER DISABLE           ENABLED  -> DISABLED
SYS_USER ENABLE            DISABLED -> ENABLED
SYS_USER PROMOTE_RESCUER   NULL     -> NULL
```

Frontend acceptance after aligning the PC dashboard/user-detail fields with the Frozen M08 DTOs:

```text
PC tests              8/8 passed
Mobile tests          8/8 passed
verify_frontend.py    PASSED
PC production build   PASSED
Mobile H5 build       PASSED
```

The existing pinned frontend dependency audit warnings and the PC Element Plus large-chunk warning remain non-blocking dependency/performance backlog; no forced breaking dependency upgrade was mixed into M08 freeze.

## Frozen conclusion

M08 has passed source, automated, static, authorization, conditional-state, JWT-refresh, operation-log, cross-domain query, statistics, frontend-contract, production-build, and clean real-MySQL acceptance. The planned M01-M08 V1.0 backend scope is now Frozen.
