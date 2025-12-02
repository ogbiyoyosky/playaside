-- Alter subscriptions to be user-centric rather than community-centric
-- 1) Drop community-based active unique index if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE schemaname = current_schema() AND indexname = 'idx_subscriptions_community_active'
    ) THEN
        EXECUTE 'DROP INDEX idx_subscriptions_community_active';
    END IF;
END $$;

-- 2) Make community_id nullable to decouple subscriptions from communities
ALTER TABLE subscriptions ALTER COLUMN community_id DROP NOT NULL;

-- 3) Add a unique partial index to ensure one active subscription per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_subscriptions_user_active
ON subscriptions(user_id)
WHERE status IN ('ACTIVE', 'TRIALING');

-- 4) Helpful indexes for user-centric queries
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);

