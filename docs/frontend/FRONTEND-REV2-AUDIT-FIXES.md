# PetLink Frontend V1.0 Rev2 Candidate — Audit Fixes

## Mandatory audit items

- Pagination: PC data-management views use `el-pagination`; mobile lists use `usePagedList` and `scrolltolower` load-more.
- Mobile Animal filters: public home list maps `species` and `sex` to the Frozen `/api/animals` query.
- Session freshness: PC route/focus/visibility and mobile launch/show refresh `/users/me`; 401 clears session and redirects to login.
- Mobile architecture: removed `ScreenView.vue`; 29 route pages delegate to 7 domain feature components and shared composables.

## Recommended audit items implemented

- Loading / Empty / Error / Retry states for lists and primary details.
- Frontend form validation and submit busy/loading guards.
- Standard UniApp CLI metadata, `.env` configuration and lockfile.
- Image upload preview, progress, failure state and retry.
- Mobile safe-area handling with `100dvh` and `env(safe-area-inset-*)`.
- PC SVG navigation icons, `aria-label` and collapsed title tooltip.
- PC Vite vendor chunks.
- Node built-in policy tests: login validation, role route, pagination, state actions, upload policy, idempotency.

## Candidate gate

This archive is not marked Frozen. Run on the target machine:

```text
python scripts/verify_frontend.py
cd petlink-pc && npm install && npm test && npm run build
cd ../petlink-mobile && npm install && npm test && npm run build:h5
```

Then perform PC + H5/UniApp integration against the Spring Boot API, especially session invalidation, pagination, protected media, upload retry and state-action forms.
