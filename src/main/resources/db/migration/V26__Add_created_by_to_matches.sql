-- Add created_by_id column to matches to track the creator of each match
ALTER TABLE matches
    ADD COLUMN created_by_id UUID;

-- Backfill existing rows (including seeded data) so they have a valid creator
DO $$
DECLARE
    default_creator_id UUID;
BEGIN
    -- Prefer a known seeded user as the default creator if present
    SELECT id INTO default_creator_id
    FROM users
    WHERE email = 'seed_user_01@example.com'
    ORDER BY created_at
    LIMIT 1;

    -- Fallback: pick the earliest created user if the specific seed doesn't exist
    IF default_creator_id IS NULL THEN
        SELECT id INTO default_creator_id
        FROM users
        ORDER BY created_at
        LIMIT 1;
    END IF;

    -- Only update rows that don't already have a creator
    IF default_creator_id IS NOT NULL THEN
        UPDATE matches
        SET created_by_id = default_creator_id
        WHERE created_by_id IS NULL;
    END IF;
END $$;

-- Enforce referential integrity and not-null once data is backfilled
ALTER TABLE matches
    ALTER COLUMN created_by_id SET NOT NULL;

ALTER TABLE matches
    ADD CONSTRAINT fk_matches_created_by
        FOREIGN KEY (created_by_id)
        REFERENCES users(id)
        ON DELETE RESTRICT;

CREATE INDEX idx_matches_created_by_id
    ON matches(created_by_id);


