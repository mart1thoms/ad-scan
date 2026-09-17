-- Filière (A&I / Info) et alternance, jusqu'ici demandées par le formulaire d'inscription mais
-- jamais enregistrées : nécessaires aux statistiques de soirée.
ALTER TABLE member ADD COLUMN filiere TEXT;
ALTER TABLE member ADD COLUMN alternant BOOLEAN;

-- La formation ne porte plus que l'année ("3A") : la filière et l'alternance ont leurs propres colonnes.
UPDATE member SET formation = '3A' WHERE formation LIKE '3A%';
UPDATE member SET formation = '4A' WHERE formation LIKE '4A%';
UPDATE member SET formation = '5A' WHERE formation LIKE '5A%';

-- Une soirée fermée n'accepte plus d'entrées (closed_at renseigné), ses statistiques restent consultables.
ALTER TABLE event ADD COLUMN closed_at TIMESTAMP;

-- Recréation de scan_log pour étendre la contrainte CHECK aux nouveaux statuts :
-- PENDING_PAYMENT (adhésion non confirmée, ne compte pas comme entrée) et EXPIRED (adhésion terminée).
CREATE TABLE scan_log_new (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id     INTEGER NOT NULL REFERENCES event (id),
    member_id    TEXT REFERENCES member (id),
    scanned_key  TEXT NOT NULL,
    status       TEXT NOT NULL CHECK (status IN ('OK', 'DUPLICATE', 'UNKNOWN_KEY', 'TOKEN_INVALIDATED',
                                                 'PENDING_PAYMENT', 'EXPIRED')),
    scanned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO scan_log_new (id, event_id, member_id, scanned_key, status, scanned_at)
SELECT id, event_id, member_id, scanned_key, status, scanned_at FROM scan_log;

DROP TABLE scan_log;
ALTER TABLE scan_log_new RENAME TO scan_log;
CREATE INDEX idx_scan_log_event ON scan_log (event_id);
