CREATE TABLE member (
    id                TEXT PRIMARY KEY,
    first_name        TEXT NOT NULL,
    last_name         TEXT NOT NULL,
    email             TEXT NOT NULL UNIQUE,
    start_date        DATE NOT NULL,
    end_date          DATE,
    access_token      TEXT NOT NULL UNIQUE,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE event (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL,
    event_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One row per (event, member): tracks the aggregate scan count for an attendee at an event.
CREATE TABLE participation (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id         INTEGER NOT NULL REFERENCES event (id),
    member_id        TEXT NOT NULL REFERENCES member (id),
    first_scanned_at TIMESTAMP NOT NULL,
    scan_count       INTEGER NOT NULL DEFAULT 1,
    UNIQUE (event_id, member_id)
);

-- One row per scan attempt (including duplicates and unknown keys): the audit trail.
CREATE TABLE scan_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id     INTEGER NOT NULL REFERENCES event (id),
    member_id    TEXT REFERENCES member (id),
    scanned_key  TEXT NOT NULL,
    status       TEXT NOT NULL CHECK (status IN ('OK', 'DUPLICATE', 'UNKNOWN_KEY')),
    scanned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_participation_event ON participation (event_id);
CREATE INDEX idx_scan_log_event ON scan_log (event_id);
CREATE INDEX idx_member_access_token ON member (access_token);

-- Short-lived, single-use token letting an unauthenticated phone (scanning a QR shown by staff)
-- fetch a member's Google Wallet link, Apple Wallet pass, or access-card PDF without logging in.
-- One token is shared by all three actions; whichever is used first invalidates the other two.
CREATE TABLE access_request (
    id         TEXT PRIMARY KEY,
    member_id  TEXT NOT NULL REFERENCES member (id),
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_access_request_member ON access_request (member_id);
