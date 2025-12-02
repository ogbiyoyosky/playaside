-- Convert availabilities.status from PostgreSQL enum to VARCHAR
ALTER TABLE availabilities
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE availabilities
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE availabilities
    ALTER COLUMN status SET DEFAULT 'AVAILABLE';

UPDATE availabilities
SET status = 'AVAILABLE'
WHERE status IS NULL;

DROP TYPE IF EXISTS availability_status;


