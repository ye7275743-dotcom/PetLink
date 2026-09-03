# M04 Animal implementation — Frozen

Baseline: Frozen `docs/api/rest-api-m04-m05.md`.

Implemented: 10 M04 APIs, public AVAILABLE browsing, privileged projections, responsible-rescuer access, optimistic basic PATCH, append/delete images, Animal media authorization including the final adopter, append-only health records, and four legal status actions with OperationLog.

Locking: image and health writes lock Animal first, plain-read RescueTask for responsibility, then lock TemporaryFile rows by id ascending when needed.

Verification evidence on 2026-09-01:

```text
verify_schema.py      PASSED
verify_m02_static.py  PASSED
verify_m03_static.py  PASSED
verify_m04_static.py  PASSED

Tests run             103
Failures              0
Errors                0
Skipped               0
Build                  SUCCESS

Real-MySQL smoke      15/15 PASSED
Final result           M04 smoke flow PASSED. animal=3 AVAILABLE
```

Status: **M04 Frozen**. The next implementation module is M05 Adoption Management.
