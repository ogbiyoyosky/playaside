-- Create event_bookings table to track bookings created after successful payments
CREATE TABLE IF NOT EXISTS event_bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Link to the originating payment
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,

    -- User who made the booking
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Financial details for the booking
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'GBP',

    -- Generated human-friendly reference for the booking
    reference VARCHAR(100) NOT NULL UNIQUE
        DEFAULT ('EVB_' || replace(uuid_generate_v4()::text, '-', '')),

    -- Time the booking was created
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_event_bookings_payment_id ON event_bookings(payment_id);
CREATE INDEX IF NOT EXISTS idx_event_bookings_user_id ON event_bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_event_bookings_reference ON event_bookings(reference);

-- Trigger to maintain updated_at column
CREATE TRIGGER update_event_bookings_updated_at
    BEFORE UPDATE ON event_bookings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();


