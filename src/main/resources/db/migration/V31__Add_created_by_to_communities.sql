-- Add created_by_id column to communities to track the creator of each community
ALTER TABLE communities
    ADD COLUMN created_by_id UUID;

-- Backfill existing communities so they have a valid creator
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
        UPDATE communities
        SET created_by_id = default_creator_id
        WHERE created_by_id IS NULL;
    END IF;
END $$;

-- Enforce referential integrity and not-null once data is backfilled
ALTER TABLE communities
    ALTER COLUMN created_by_id SET NOT NULL;

ALTER TABLE communities
    ADD CONSTRAINT fk_communities_created_by
        FOREIGN KEY (created_by_id)
        REFERENCES users(id)
        ON DELETE RESTRICT;

CREATE INDEX idx_communities_created_by_id
    ON communities(created_by_id);


