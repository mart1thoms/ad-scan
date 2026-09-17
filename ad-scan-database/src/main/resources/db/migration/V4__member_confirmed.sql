-- Members created by staff are trusted immediately (default 1). Public self-registration
-- (see /register) explicitly creates its rows as unconfirmed ("non cotisant") until a staff
-- member validates them from the back-office.
ALTER TABLE member ADD COLUMN confirmed BOOLEAN NOT NULL DEFAULT 1;
