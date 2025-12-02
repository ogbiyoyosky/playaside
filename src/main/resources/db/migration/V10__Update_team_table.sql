-- Create subscription status enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'team_availability_status') THEN
        CREATE TYPE team_availability_status AS ENUM ('SELECTED', 'RESERVE');
    END IF;
END $$;


-- Alter team_players table to add team_availability_status column with default value of SELECTED
ALTER TABLE team_players ADD COLUMN team_availability_status team_availability_status NOT NULL DEFAULT 'SELECTED';  