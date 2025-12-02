-- Increase matches.status column size to accommodate TEAMS_MANUALLY_SELECTED (24 chars)
ALTER TABLE matches
    ALTER COLUMN status TYPE VARCHAR(30) USING status::text;

