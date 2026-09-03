# Global file infrastructure follow-up

These items were originally deferred from M02 business completion. They are now implemented as conservative scheduled jobs and remain documented because they affect operational file hygiene.

## 1. Formal-file orphan scanner — implemented

- Scan formal upload areas for files with no valid business-table association.
- Apply a minimum 24-hour safety window before considering deletion.
- Canonicalize every path and keep all operations inside the configured uploads root.
- If DB association or physical-file state is uncertain, preserve the file and log for manual inspection.

## 2. Scheduled retry for residual BOUND temporary records — implemented

- Retry cleanup of `temporary_file.status = BOUND` rows left after post-commit cleanup failure.
- Before deleting the temp original or technical row, reuse the Frozen safety checks: formal business association exists and formal physical file exists.
- Process rows independently so one failure does not block later rows.
- Do not delete inconsistent records automatically. Preserve them for manual inspection.

## Status

Implemented in the V1.0 completion pass. Both jobs reuse the Frozen path-safety and association checks, process bounded batches, preserve uncertain rows/files, and are covered by infrastructure tests. This remains a global file-infrastructure capability rather than a database-schema change.
