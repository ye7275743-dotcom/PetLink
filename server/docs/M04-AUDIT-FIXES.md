# M04 candidate audit fixes

Audit date: 2026-09-01.

The following delivery and regression issues were corrected before workspace integration:

1. Fixed an extra closing parenthesis in `AnimalControllerSecurityWebTest` that prevented test compilation.
2. Restored the Frozen M03 hidden-resource rule: a non-owner RESCUER querying a task receives `40401`, not `40301`.
3. Added a Web security assertion proving unauthenticated visitors can read an authorized public Animal image binary.
4. Extended `verify_m03_static.py` and `verify_m04_static.py` so the two permission rules above cannot silently regress.

Verification after the fixes:

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
```

Real-MySQL acceptance subsequently passed all 15 stages:

```text
M04 smoke flow PASSED. animal=3 AVAILABLE
```

M04 is therefore Frozen.
