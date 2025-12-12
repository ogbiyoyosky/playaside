-- Add connected_account_id to users table for Stripe Connect payouts
ALTER TABLE users
    ADD COLUMN connected_account_id VARCHAR(255);


