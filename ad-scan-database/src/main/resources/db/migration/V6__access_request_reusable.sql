-- Lets a token be reusable (e.g. the post-registration card page, valid 12h and usable for all
-- three actions repeatedly) instead of the default single-use-global semantics (staff "share access").
ALTER TABLE access_request ADD COLUMN single_use BOOLEAN NOT NULL DEFAULT 1;
