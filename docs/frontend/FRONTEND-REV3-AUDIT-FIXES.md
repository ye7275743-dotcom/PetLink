# PetLink Frontend V1.0 Rev3 Candidate — audit fixes

This revision is frontend-only. It does not change the Spring Boot M04/M05 backend candidates.

## Mandatory fixes applied

1. Announcement publish/withdraw now send `{ version }` on both PC and mobile API clients.
2. PC FAILED task resolution distinguishes dialog outcomes: confirm => REOPEN, explicit cancel button => CLOSE, X/ESC/modal dismiss => no request.
3. Mobile direct URL access is protected by per-page route guards; USER cannot enter RESCUER-only pages and unsupported mobile roles are cleared back to login.
4. PC only accepts ADMIN/RESCUER workbench roles. A USER session is cleared and redirected to `/login`, avoiding `/dashboard` self-redirect loops.
5. Mobile tests import from `../src/composables/...` after migration to the standard `src/` layout.

## Delivery-layer improvements carried into this revision

- Mobile source is under `petlink-mobile/src/`.
- H5 `index.html` is present.
- UniApp Vite plugin loading is tolerant of direct/default export interop.
- Both package-lock files are present. Because this execution environment cannot reach the npm registry, lock completeness and fresh `npm ci` cannot be independently regenerated here; use the locally audited lockfiles/build evidence as the final freeze gate.

## Automated checks executed here

- PC Node policy/contract tests: 6/6 passed.
- Mobile Node policy tests: 7/7 passed.
- `scripts/verify_frontend.py`: PASSED.

This revision remains Candidate until the exact delivered ZIP passes `npm ci`, both test suites, PC build, and Mobile H5 build in the project environment.
