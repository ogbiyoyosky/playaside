-- Align PostgreSQL enum payment_type with Java PaymentType.WALLET_DEPOSIT

DO $$
BEGIN
    -- Only attempt rename if the payment_type enum and the old label exist
    IF EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_enum e ON e.enumtypid = t.oid
        WHERE t.typname = 'payment_type'
          AND e.enumlabel = 'COMMUNITY_WALLET_DEPOSIT'
    ) THEN
        ALTER TYPE payment_type
            RENAME VALUE 'COMMUNITY_WALLET_DEPOSIT' TO 'WALLET_DEPOSIT';
    END IF;
END $$;






