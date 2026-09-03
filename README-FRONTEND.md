# PetLink Frontend V1.0 中文化交付版 — Frozen

Frontend-only delivery based on the Frozen PetLink UI/API plan.

- `petlink-pc/`: Vue 3 + Vite 5 + Element Plus, for ADMIN / RESCUER workbench.
- `petlink-mobile/`: UniApp + Vue 3 standard CLI `src/` project, for VISITOR / USER / RESCUER.
- 29 mobile route views and 10 PC workbench pages remain aligned with the Frozen prototype scope.
- Rev4 closes the two delivery blockers from the latest audit: reproducible complete npm lockfiles and machine-verifiable page-level Mobile route guards.
- The delivery localization pass replaces user-visible English labels with Chinese while preserving every Frozen API field, role code, status code, and request value. The follow-up visual pass adds premium layered surfaces, responsive depth, and consistent touch/keyboard feedback across both clients.
- The latest mobile design refinement adds a branded status/header hierarchy, floating navigation feedback, editorial section markers, richer card surfaces, and consistent form/upload states without changing the Frozen business contracts.

Status: **Frozen** after clean installation, automated tests, static verification, M08 contract alignment, Chinese UI and semantic SVG icon browser acceptance, cross-platform E2E, and both production builds. Final acceptance was rerun on 2026-09-03.

## Reproducible verification

```bash
cd petlink-pc
npm ci
npm test
npm run build

cd ../petlink-mobile
npm ci
npm test
npm run build:h5

cd ..
python scripts/verify_frontend.py
```

Verified results for this revision:

- PC `npm ci`: 81 packages installed from the committed lockfile.
- PC tests: 20/20 passed, including profile/overview contracts, Chinese display mapping, missing protected-image fallback, rescuer waiting-task workflow, request tracing, pagination race protection, premium depth surfaces, visual token, semantic SVG icon dictionary, hero asset checks, and login failure feedback.
- PC Vite production build: passed.
- Mobile `npm ci`: 771 packages installed from the committed lockfile.
- Mobile tests: 18/18 passed, including all 29 page-level Guard checks, overview response mapping, Chinese display mapping, handled initial-load network failures, rescuer task-page refresh, request tracing, pagination race protection, premium touch surfaces, visual token, semantic SVG navigation/actions, and hero asset checks.
- Mobile UniApp H5 production build: passed.
- `scripts/verify_frontend.py`: passed with 29 Mobile routes, 7 feature domains, 10 PC workbench pages plus login, and 72 documented Frozen APIs.
- PC runtime dependency audit: 0 vulnerabilities after the compatible Axios/Element Plus security update. The coordinated UniApp upgrade item is documented separately under `docs/testing/DEPENDENCY-AUDIT-2026-09-02.md`.

See `docs/frontend/FRONTEND-CHINESE-LOCALIZATION-CHANGE.md` for the Chinese localization boundary and evidence, and `docs/frontend/FRONTEND-REV4-AUDIT-FIXES.md` for the original Rev4 changes. The current UI, icon system, cross-platform evidence and remaining device checks are recorded in `docs/testing/UI-OPTIMIZATION-ACCEPTANCE-20260903.md`, `docs/testing/ICON-SYSTEM-ACCEPTANCE-20260903.md`, `docs/testing/CROSS-PLATFORM-E2E-ACCEPTANCE-20260903.md`, `docs/testing/FINAL-ACCEPTANCE-SUMMARY.md`, and `docs/testing/COMPATIBILITY-ACCEPTANCE-REPORT.md`.
See `docs/frontend/MOBILE-UI-DESIGN-UPGRADE-20260903.md` for the latest mobile-only visual change record and browser evidence.

For macOS/Linux, the repository-level `scripts/verify-all.sh` runs the complete local automated gate, including backend tests, SQL/module static checks, both frontend test suites and builds, frontend contract verification, and delivery-package hash verification.
