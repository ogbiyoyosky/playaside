-- Create enum type for venue booking status
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'venue_booking_status') THEN
        CREATE TYPE venue_booking_status AS ENUM ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELED');
    END IF;
END $$;

-- Create venue_bookings table
CREATE TABLE venue_bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    venue_id UUID NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    rent_type VARCHAR(20) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    status venue_booking_status NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_reference VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_venue_bookings_venue_id ON venue_bookings(venue_id);
CREATE INDEX idx_venue_bookings_user_id ON venue_bookings(user_id);
CREATE INDEX idx_venue_bookings_status ON venue_bookings(status);
CREATE INDEX idx_venue_bookings_time_range ON venue_bookings(venue_id, start_time, end_time);


