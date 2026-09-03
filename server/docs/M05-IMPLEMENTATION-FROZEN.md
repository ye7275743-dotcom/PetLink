# M05 Adoption Management - Frozen

## Baseline protection

M05 was selectively merged on top of the accepted M01-M04 Frozen workspace. The M03 hidden-resource `40401` test, the M04 public AnimalImage media test, the M03/M04 static verifiers, and all earlier Frozen documents remain unchanged.

## Implemented API surface

1. `POST /api/animals/{animalId}/adoption-applications`
2. `GET /api/adoption-applications/me`
3. `GET /api/adoption-applications/{applicationId}`
4. `POST /api/adoption-applications/{applicationId}/withdraw`
5. `GET /api/admin/adoption-applications`
6. `POST /api/admin/adoption-applications/{applicationId}/audit`
7. `GET /api/adoption-records/me`
8. `GET /api/adoption-records/{recordId}`
9. `GET /api/animals/{animalId}/adoption-overview`

## Frozen transaction rules

- submit locks `Animal` and requires `AVAILABLE`;
- `user_id` always comes from the authenticated principal;
- only `uk_adoption_application_user_animal` maps to `40907`;
- withdraw uses an owner + `PENDING` conditional update;
- reject locks only the target `AdoptionApplication`;
- approve lock order is `Animal -> target Application -> other PENDING Applications by id ASC`;
- approval atomically creates `AdoptionRecord`, changes `Animal` to `ADOPTED`, invalidates other pending applications, and writes all required operation logs;
- historical self-review by an ADMIN is rejected with `40301`;
- a final adopter retains authorized AnimalImage access, while an invalidated historical applicant receives no unusable cover URL.

## Verification evidence

Completed on 2026-09-01:

```text
verify_schema.py      PASSED
verify_m02_static.py  PASSED
verify_m03_static.py  PASSED
verify_m04_static.py  PASSED
verify_m05_static.py  PASSED

Tests run: 137
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS

smoke-m05.ps1         PASSED against real MySQL
```

The 13-stage real-MySQL smoke flow covered application withdrawal, rejection, approval, automatic invalidation, immutable AdoptionRecord creation, Animal adoption, final-adopter media authorization, and responsible-RESCUER adoption overview.

## Frozen conclusion

M05 has passed source-level, automated, static, and real-MySQL acceptance. Any later change to M05 must preserve the Frozen API, transaction lock order, status machine, privacy rules, and M01-M04 regression checks.
