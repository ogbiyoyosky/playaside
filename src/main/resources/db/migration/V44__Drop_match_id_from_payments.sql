-- Drop match_id column from payments table
-- This removes the optional link between payments and matches

-- Drop the index first
DROP INDEX IF EXISTS idx_payments_match_id;

-- Drop the column
ALTER TABLE payments
DROP COLUMN IF EXISTS match_id;