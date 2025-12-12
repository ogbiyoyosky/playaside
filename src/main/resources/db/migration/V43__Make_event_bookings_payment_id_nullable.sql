-- Make payment_id column nullable in event_bookings table
-- This allows creating event bookings for wallet payments and free events

ALTER TABLE event_bookings
ALTER COLUMN payment_id DROP NOT NULL;