-- Add optional match reference to payments (for MATCH_FEE payments tied to a specific match)
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS match_id UUID NULL REFERENCES matches(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_payments_match_id ON payments(match_id);

-- Create match_registrations table to track users joining matches (separate from availability)
CREATE TABLE IF NOT EXISTS match_registrations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payment_id UUID NULL REFERENCES payments(id) ON DELETE SET NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (match_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_match_registrations_match_id ON match_registrations(match_id);
CREATE INDEX IF NOT EXISTS idx_match_registrations_user_id ON match_registrations(user_id);


