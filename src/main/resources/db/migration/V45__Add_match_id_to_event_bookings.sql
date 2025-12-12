-- Add nullable match_id column to event_bookings table
-- This allows linking event bookings to specific matches

ALTER TABLE event_bookings
ADD COLUMN IF NOT EXISTS match_id UUID NULL REFERENCES matches(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_event_bookings_match_id ON event_bookings(match_id);