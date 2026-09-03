# M03 Rescue Task Rev2 Candidate Notes

Baseline: Frozen M02/M03 REST API design. No Frozen database or state-machine rule changes.

Rev2 keeps the audited M03 business implementation and fixes delivery regressions:

- selective M03 merge on top of M02 Rev2;
- M02 `StateActionResponse` test import restored;
- audited M02 smoke script retained;
- M03 smoke collection counts made Windows PowerShell 5.1-safe;
- 7 controller/security Web tests added;
- source test count: 74 total, 26 M03-focused.

Freeze gate:

1. `mvnw.cmd test` passes on this exact Rev2 package.
2. SQL + M02 + M03 static verification passes.
3. MySQL real initialization succeeds.
4. Spring Boot starts.
5. M01/M02/M03 smoke scripts pass.
6. Audit confirms no Frozen-rule regression.
