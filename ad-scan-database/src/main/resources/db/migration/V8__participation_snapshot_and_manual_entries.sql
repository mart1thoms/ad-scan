-- Les entrées d'une soirée doivent survivre à la suppression de l'adhérent : on copie sur chaque
-- entrée les informations dont dépendent les stats (nom, formation, filière, alternance) au moment
-- du scan, et member_id devient nullable (mis à NULL quand l'adhérent est supprimé).
-- On y ajoute aussi les entrées "sans badge" saisies manuellement à la porte (source = MANUAL).
CREATE TABLE participation_new (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id         INTEGER NOT NULL REFERENCES event (id),
    member_id        TEXT REFERENCES member (id),
    member_name      TEXT,
    formation        TEXT,
    filiere          TEXT,
    alternant        BOOLEAN,
    source           TEXT NOT NULL DEFAULT 'SCAN' CHECK (source IN ('SCAN', 'MANUAL')),
    first_scanned_at TIMESTAMP NOT NULL,
    scan_count       INTEGER NOT NULL DEFAULT 1,
    UNIQUE (event_id, member_id)
);

INSERT INTO participation_new (id, event_id, member_id, member_name, formation, filiere, alternant, source,
                               first_scanned_at, scan_count)
SELECT p.id, p.event_id, p.member_id, m.first_name || ' ' || m.last_name, m.formation, m.filiere, m.alternant,
       'SCAN', p.first_scanned_at, p.scan_count
FROM participation p
LEFT JOIN member m ON m.id = p.member_id;

DROP TABLE participation;
ALTER TABLE participation_new RENAME TO participation;
CREATE INDEX idx_participation_event ON participation (event_id);

-- Même principe pour l'historique des scans : le nom est conservé une fois l'adhérent supprimé.
ALTER TABLE scan_log ADD COLUMN member_name TEXT;
UPDATE scan_log
SET member_name = (SELECT first_name || ' ' || last_name FROM member WHERE member.id = scan_log.member_id)
WHERE member_id IS NOT NULL;
