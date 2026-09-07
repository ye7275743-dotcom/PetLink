-- Additive migration. Apply once before deploying the UI-management extension.
-- Keep historical ownership and account uniqueness; do not physically delete users.
ALTER TABLE sys_user ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE announcement ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
CREATE INDEX idx_sys_user_deleted_role_status ON sys_user (deleted, role_code, status);
CREATE INDEX idx_announcement_deleted_status_created ON announcement (deleted, status, created_at DESC, id DESC);
