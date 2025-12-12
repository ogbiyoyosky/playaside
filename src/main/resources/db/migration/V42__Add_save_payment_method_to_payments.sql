-- Add save_payment_method column to payments table
ALTER TABLE payments ADD COLUMN save_payment_method BOOLEAN NOT NULL DEFAULT FALSE;