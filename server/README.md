# PetLink Server - M01-M08 Frozen

Recommended: JDK 17, MySQL 8.0.16+.

M01-M08 are Frozen. The current full suite passes 269 tests with no failures, errors, or skips. Database structure and M02-M08 static verifiers pass. M06, M07, and M08 passed their 11-stage, 12-stage, and 12-stage smoke flows consecutively against a freshly initialized MySQL 8.0.46 database. Post-freeze file cleanup/orphan scanning is implemented and covered by focused tests.

Final evidence is in `docs/M06-IMPLEMENTATION-FROZEN.md`, `docs/M07-IMPLEMENTATION-FROZEN.md`, and `docs/M08-IMPLEMENTATION-FROZEN.md`.

Workspace-level final acceptance reports are under `../docs/testing/`. The implementation-complete traceability baseline is `../docs/PetLink-需求追踪矩阵-v1.2-Implemented-Frozen.xlsx`.

## Before start

1. execute `../sql/petlink.sql`;
2. execute `../sql/dev-seed-admin.sql`, `../sql/dev-seed-rescuer.sql`, and `../sql/dev-seed-user.sql` for local smoke;
3. configure DB/JWT environment variables;
4. run all static verifiers.

## Tests

```powershell
.\mvnw.cmd test
```

## Static verification

```powershell
python .\scripts\verify_schema.py
python .\scripts\verify_m02_static.py
python .\scripts\verify_m03_static.py
python .\scripts\verify_m04_static.py
python .\scripts\verify_m05_static.py
python .\scripts\verify_m06_static.py
python .\scripts\verify_m07_static.py
python .\scripts\verify_m08_static.py
```

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Smoke

```powershell
.\scripts\smoke-m01.ps1
.\scripts\smoke-m02.ps1
.\scripts\smoke-m03.ps1
.\scripts\smoke-m04.ps1
.\scripts\smoke-m05.ps1
.\scripts\smoke-m06.ps1
.\scripts\smoke-m07.ps1
.\scripts\smoke-m08.ps1
```

## 50-user performance acceptance

With the backend and a real MySQL database running:

```bash
python3 scripts/performance_m01_m08.py
```

The script excludes account setup/login from timing, executes 50 concurrent authenticated users, writes token-free JSON evidence under `../docs/testing/evidence/`, and fails if success rate or the Frozen P95 targets are not met.

Frozen API baselines are under `../docs/api/`.
