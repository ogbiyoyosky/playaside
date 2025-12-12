-- Convert all TIMESTAMP columns to TIMESTAMP WITH TIME ZONE for proper UTC handling
-- This ensures all datetime values are stored with timezone information

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

-- Users table
SELECT alter_column_to_timestamptz('users', 'created_at');
SELECT alter_column_to_timestamptz('users', 'updated_at');

-- User roles table
SELECT alter_column_to_timestamptz('user_roles', 'created_at');
SELECT alter_column_to_timestamptz('user_roles', 'deleted_at');

-- Communities table
SELECT alter_column_to_timestamptz('communities', 'created_at');
SELECT alter_column_to_timestamptz('communities', 'updated_at');
SELECT alter_column_to_timestamptz('communities', 'banned_at');

-- Community members table
SELECT alter_column_to_timestamptz('community_members', 'created_at');
SELECT alter_column_to_timestamptz('community_members', 'updated_at');
SELECT alter_column_to_timestamptz('community_members', 'banned_at');

-- Matches table
SELECT alter_column_to_timestamptz('matches', 'match_date');
SELECT alter_column_to_timestamptz('matches', 'registration_deadline');
SELECT alter_column_to_timestamptz('matches', 'created_at');
SELECT alter_column_to_timestamptz('matches', 'updated_at');

-- Teams table
SELECT alter_column_to_timestamptz('teams', 'created_at');
SELECT alter_column_to_timestamptz('teams', 'updated_at');

-- Team players table
SELECT alter_column_to_timestamptz('team_players', 'created_at');
SELECT alter_column_to_timestamptz('team_players', 'updated_at');

-- Availabilities table
SELECT alter_column_to_timestamptz('availabilities', 'created_at');
SELECT alter_column_to_timestamptz('availabilities', 'updated_at');

-- Payments table
SELECT alter_column_to_timestamptz('payments', 'processed_at');
SELECT alter_column_to_timestamptz('payments', 'created_at');
SELECT alter_column_to_timestamptz('payments', 'updated_at');

-- Subscriptions table
SELECT alter_column_to_timestamptz('subscriptions', 'trial_start_date');
SELECT alter_column_to_timestamptz('subscriptions', 'trial_end_date');
SELECT alter_column_to_timestamptz('subscriptions', 'current_period_start');
SELECT alter_column_to_timestamptz('subscriptions', 'current_period_end');
SELECT alter_column_to_timestamptz('subscriptions', 'next_billing_date');
SELECT alter_column_to_timestamptz('subscriptions', 'canceled_at');
SELECT alter_column_to_timestamptz('subscriptions', 'created_at');
SELECT alter_column_to_timestamptz('subscriptions', 'updated_at');

-- Password reset tokens table
SELECT alter_column_to_timestamptz('password_reset_tokens', 'expires_at');
SELECT alter_column_to_timestamptz('password_reset_tokens', 'created_at');

-- Refresh tokens table
SELECT alter_column_to_timestamptz('refresh_tokens', 'expires_at');
SELECT alter_column_to_timestamptz('refresh_tokens', 'created_at');

-- User files table
SELECT alter_column_to_timestamptz('user_files', 'uploaded_at');

-- Device tokens table
SELECT alter_column_to_timestamptz('device_tokens', 'created_at');
SELECT alter_column_to_timestamptz('device_tokens', 'updated_at');

-- Chat messages table
SELECT alter_column_to_timestamptz('chat_messages', 'created_at');

-- Venues table
SELECT alter_column_to_timestamptz('venues', 'created_at');
SELECT alter_column_to_timestamptz('venues', 'updated_at');

-- Venue bookings table
SELECT alter_column_to_timestamptz('venue_bookings', 'start_time');
SELECT alter_column_to_timestamptz('venue_bookings', 'end_time');
SELECT alter_column_to_timestamptz('venue_bookings', 'booked_at');
SELECT alter_column_to_timestamptz('venue_bookings', 'created_at');
SELECT alter_column_to_timestamptz('venue_bookings', 'updated_at');

-- Waitlist entries table
SELECT alter_column_to_timestamptz('waitlist_entries', 'created_at');

-- Match registrations table
SELECT alter_column_to_timestamptz('match_registrations', 'joined_at');

-- Wallets table
SELECT alter_column_to_timestamptz('wallets', 'created_at');
SELECT alter_column_to_timestamptz('wallets', 'updated_at');

-- Private chat messages table
SELECT alter_column_to_timestamptz('private_chat_messages', 'created_at');

-- Community chat messages table
SELECT alter_column_to_timestamptz('community_chat_messages', 'created_at');

-- Chat read states tables
SELECT alter_column_to_timestamptz('private_chat_read_states', 'last_read_at');
SELECT alter_column_to_timestamptz('community_chat_read_states', 'last_read_at');
SELECT alter_column_to_timestamptz('match_chat_read_states', 'last_read_at');

-- Payouts table
SELECT alter_column_to_timestamptz('payouts', 'scheduled_payout_date');
SELECT alter_column_to_timestamptz('payouts', 'processed_at');
SELECT alter_column_to_timestamptz('payouts', 'created_at');
SELECT alter_column_to_timestamptz('payouts', 'updated_at');

-- Transactions table
SELECT alter_column_to_timestamptz('transactions', 'created_at');
SELECT alter_column_to_timestamptz('transactions', 'updated_at');

-- Event bookings table
SELECT alter_column_to_timestamptz('event_bookings', 'booked_at');
SELECT alter_column_to_timestamptz('event_bookings', 'created_at');
SELECT alter_column_to_timestamptz('event_bookings', 'updated_at');

-- Clean up the function
DROP FUNCTION alter_column_to_timestamptz(TEXT, TEXT);