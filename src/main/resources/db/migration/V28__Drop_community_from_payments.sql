-- Drop community reference from payments table now that payments are user-based
ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS payments_community_id_fkey;

DROP INDEX IF EXISTS idx_payments_community_id;

ALTER TABLE payments
    DROP COLUMN IF EXISTS community_id;


