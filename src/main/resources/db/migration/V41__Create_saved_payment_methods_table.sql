-- Add stripe_customer_id to users table
ALTER TABLE users ADD COLUMN stripe_customer_id VARCHAR(255) UNIQUE;

-- Create saved payment methods table
CREATE TABLE saved_payment_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stripe_payment_method_id VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL, -- card, apple_pay, google_pay, etc.
    card_brand VARCHAR(50), -- visa, mastercard, amex, etc. (for cards)
    card_last4 VARCHAR(4), -- last 4 digits of card
    card_exp_month INTEGER, -- expiration month (for cards)
    card_exp_year INTEGER, -- expiration year (for cards)
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_saved_payment_methods_user_id ON saved_payment_methods(user_id);
CREATE INDEX idx_saved_payment_methods_stripe_payment_method_id ON saved_payment_methods(stripe_payment_method_id);
CREATE INDEX idx_saved_payment_methods_is_default ON saved_payment_methods(user_id, is_default);

-- Ensure only one default payment method per user
CREATE UNIQUE INDEX idx_saved_payment_methods_user_default
ON saved_payment_methods(user_id)
WHERE is_default = TRUE;

-- Create trigger to update updated_at timestamp
CREATE TRIGGER update_saved_payment_methods_updated_at
    BEFORE UPDATE ON saved_payment_methods
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();