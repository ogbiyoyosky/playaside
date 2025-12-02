-- Convert matches.status from PostgreSQL enum to VARCHAR
ALTER TABLE matches
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE matches
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE matches
    ALTER COLUMN status SET DEFAULT 'UPCOMING';

UPDATE matches
SET status = 'UPCOMING'
WHERE status IS NULL;

DROP TYPE IF EXISTS match_status;

