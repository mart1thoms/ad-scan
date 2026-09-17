-- member.updated_at, auto-maintained via trigger whenever the row changes (e.g. token regeneration).
ALTER TABLE member ADD COLUMN updated_at TIMESTAMP;
UPDATE member SET updated_at = created_at;

CREATE TRIGGER trg_member_updated_at
AFTER UPDATE ON member
FOR EACH ROW WHEN NEW.updated_at IS OLD.updated_at
BEGIN
    UPDATE member SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- Full history of every access token ever issued to a member. member.access_token always mirrors
-- the currently active one (invalidated_at IS NULL); older, regenerated-away tokens stay here so a
-- scan of one can be reported as "invalidated" rather than looking like a completely unknown key.
CREATE TABLE member_access_token (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    member_id      TEXT NOT NULL REFERENCES member (id),
    token          TEXT NOT NULL UNIQUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invalidated_at TIMESTAMP
);

CREATE INDEX idx_member_access_token_member ON member_access_token (member_id);

-- Seed history with each existing member's current (still active) token.
INSERT INTO member_access_token (member_id, token, created_at, invalidated_at)
SELECT id, access_token, created_at, NULL FROM member;

-- Recreate scan_log so its CHECK constraint also allows the new TOKEN_INVALIDATED status
-- (SQLite has no ALTER ... to change a CHECK constraint in place).
CREATE TABLE scan_log_new (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id     INTEGER NOT NULL REFERENCES event (id),
    member_id    TEXT REFERENCES member (id),
    scanned_key  TEXT NOT NULL,
    status       TEXT NOT NULL CHECK (status IN ('OK', 'DUPLICATE', 'UNKNOWN_KEY', 'TOKEN_INVALIDATED')),
    scanned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO scan_log_new (id, event_id, member_id, scanned_key, status, scanned_at)
SELECT id, event_id, member_id, scanned_key, status, scanned_at FROM scan_log;

DROP TABLE scan_log;
ALTER TABLE scan_log_new RENAME TO scan_log;
CREATE INDEX idx_scan_log_event ON scan_log (event_id);
