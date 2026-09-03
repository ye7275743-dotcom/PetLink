# M03 Rev2 audit fixes

This revision is built by **selectively merging M03 files onto the M02 Rev2 candidate baseline**. It is intentionally not produced by unpacking the older M03 ZIP over the workspace.

Audit fixes included:

1. Restored `import com.petlink.modules.clue.vo.StateActionResponse;` in `RescueClueTransactionalServiceTest`.
2. Restored the audited ASCII `smoke-m02.ps1` and its PowerShell 5.1-safe `@(...).Count` collection checks.
3. Updated every scalar-sensitive collection count in `smoke-m03.ps1` to `@(...).Count`.
4. Corrected test-count reporting: the audited pre-Rev2 baseline contained 67 `@Test` methods (48 existing + 19 M03). Rev2 adds 7 M03 controller/security Web tests, so the source count is now 74 total / 26 M03-focused. This count is not a claim that the new 74-test package has already run in the delivery environment.
5. Added `RescueTaskControllerSecurityWebTest` covering:
   - unauthenticated request -> 401;
   - USER cannot accept a rescue task;
   - non-owner RESCUER ownership rejection maps to 403;
   - ADMIN can view task detail and rescue records;
   - ADMIN cannot start a RESCUER task;
   - ADMIN cannot append rescue records;
   - ADMIN cannot submit rescue results.
6. Strengthened `verify_m03_static.py` with regression guards for the M02 import and PowerShell-safe collection counting.

No Frozen database structure or M03 business-state rule is changed by this revision.
