-- Create venues table
CREATE TABLE venues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    venue_type VARCHAR(50) NOT NULL,
    venue_image_url VARCHAR(255) NULL,

    address VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    province VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    post_code VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,

    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,

    open_monday BOOLEAN NOT NULL DEFAULT FALSE,
    open_tuesday BOOLEAN NOT NULL DEFAULT FALSE,
    open_wednesday BOOLEAN NOT NULL DEFAULT FALSE,
    open_thursday BOOLEAN NOT NULL DEFAULT FALSE,
    open_friday BOOLEAN NOT NULL DEFAULT FALSE,
    open_saturday BOOLEAN NOT NULL DEFAULT FALSE,
    open_sunday BOOLEAN NOT NULL DEFAULT FALSE,

    rent_type VARCHAR(20) NOT NULL,
    price_per_hour DECIMAL(10, 2) NULL,
    price_per_day DECIMAL(10, 2) NULL,
    max_rent_hours INT NULL,
    max_rent_days INT NULL,

    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_venues_city ON venues(city);
CREATE INDEX idx_venues_post_code ON venues(post_code);
CREATE INDEX idx_venues_owner_id ON venues(owner_id);


