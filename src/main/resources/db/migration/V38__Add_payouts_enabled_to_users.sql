-- Mark whether a user’s Stripe connected account is fully enabled for payouts
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE;


