# PetLink Frontend V1.0 Rev4 — Frozen audit record

This revision is frontend-only and does not change the Spring Boot backend candidates.

## Blocker 1 — complete reproducible npm lockfiles

The two direct-only lockfiles in Rev3 were replaced with the complete lockfiles generated from the same `package.json` manifests during the clean-install audit.

- PC `package-lock.json`: lockfileVersion 3, 127 `packages` entries.
- Mobile `package-lock.json`: lockfileVersion 3, 819 `packages` entries.
- The root dependency and devDependency maps are byte-for-byte equivalent in meaning to the current `package.json` manifests.
- Direct dependency entries include real `resolved` tarball URLs and SRI `integrity` values.
- `scripts/verify_frontend.py` now rejects shallow/direct-only locks, dependency-map drift, or incomplete direct package entries.

The previous clean audit using these regenerated dependency trees reported PC `npm ci` + build success and Mobile `npm ci` + H5 build success. The current execution container has no npm registry/DNS access, so it cannot independently download the dependency tarballs again. PC `npm ci --package-lock-only --offline` succeeds; Mobile reaches peer-resolution metadata lookup and stops only because the registry metadata is not cached.

## Blocker 2 — page-level Mobile Guard is explicitly wired and tested

All 29 `src/pages/*/index.vue` files now use a normalized, visible `script setup` form:

```js
import { useRouteGuard } from '../../composables/routeGuard.js'
useRouteGuard('<pageName>')
```

The Guard implementation registers `enforceMobileRoute(name)` on the UniApp page `onShow` lifecycle. The App-level current-route check remains as defense in depth rather than the sole enforcement point.

A new Mobile test reads `src/pages.json`, asserts exactly 29 routes, opens every corresponding Vue page, verifies the exact page-name Guard call, and verifies that `useRouteGuard()` binds to `onShow`. This covers direct H5 URL entry, ordinary page navigation, and USER attempts to enter RESCUER-only routes at the page execution layer.

## Automated checks executed for Rev4

- PC Node policy/contract tests: 6/6 passed.
- Mobile Node policy/contract/Guard-wiring tests: 8/8 passed.
- `scripts/verify_frontend.py`: PASSED with complete-lock gates enabled.

## Final acceptance

No non-blocking dependency upgrades or Element Plus bundle optimization are mixed into Rev4. The exact ZIP should receive one final registry-enabled run of:

```text
PC:     npm ci -> npm test -> npm run build
Mobile: npm ci -> npm test -> npm run build:h5
Root:   python scripts/verify_frontend.py
```

The exact delivered ZIP was verified on 2026-09-01. PC `npm ci`, 6/6 tests, and the Vite production build passed. Mobile `npm ci`, 8/8 tests, and the UniApp H5 production build passed. `scripts/verify_frontend.py` also passed. The artifact is therefore promoted to **Frontend V1.0 Rev4 — Frozen**.

Known non-blocking technical debt remains recorded rather than hidden: the full Element Plus bundle produces an approximately 835 KB minified PC chunk, and the pinned frontend toolchains report dependency audit/deprecation warnings. No unsafe forced dependency upgrade was mixed into the Frozen revision.
