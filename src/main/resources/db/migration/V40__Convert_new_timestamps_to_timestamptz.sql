-- Convert newer TIMESTAMP columns to TIMESTAMP WITH TIME ZONE for proper UTC handling
-- This migration handles tables created after V39 that still use TIMESTAMP instead of TIMESTAMPTZ

-- Function to safely alter column type only if column exists
CREATE OR REPLACE FUNCTION alter_column_to_timestamptz(p_table_name TEXT, p_column_name TEXT)
RETURNS VOID AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = p_table_name
        AND column_name = p_column_name
        AND table_schema = 'public'
    ) THEN
        EXECUTE format('ALTER TABLE %I ALTER COLUMN %I TYPE TIMESTAMPTZ', p_table_name, p_column_name);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Payouts table (created in V35)
SELECT alter_column_to_timestamptz('payouts', 'scheduled_payout_date');
SELECT alter_column_to_timestamptz('payouts', 'processed_at');
SELECT alter_column_to_timestamptz('payouts', 'created_at');
SELECT alter_column_to_timestamptz('payouts', 'updated_at');

-- Transactions table (created in V36)
SELECT alter_column_to_timestamptz('transactions', 'created_at');
SELECT alter_column_to_timestamptz('transactions', 'updated_at');

-- Event bookings table (created in V37)
SELECT alter_column_to_timestamptz('event_bookings', 'booked_at');
SELECT alter_column_to_timestamptz('event_bookings', 'created_at');
SELECT alter_column_to_timestamptz('event_bookings', 'updated_at');

-- Private chat messages table (created in V32, missed in V39 due to wrong table name)
SELECT alter_column_to_timestamptz('private_chat_messages', 'created_at');

-- Community chat messages table (created in V32, missed in V39 due to wrong table name)
SELECT alter_column_to_timestamptz('community_chat_messages', 'created_at');

-- Clean up the function
DROP FUNCTION alter_column_to_timestamptz(TEXT, TEXT);