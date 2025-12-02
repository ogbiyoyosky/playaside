-- Convert team_players.team_availability_status from PostgreSQL enum to VARCHAR
ALTER TABLE team_players
    ALTER COLUMN team_availability_status DROP DEFAULT;

ALTER TABLE team_players
    ALTER COLUMN team_availability_status TYPE VARCHAR(20);

ALTER TABLE team_players
    ALTER COLUMN team_availability_status SET DEFAULT 'SELECTED';

UPDATE team_players
SET team_availability_status = 'SELECTED'
WHERE team_availability_status IS NULL;

DROP TYPE IF EXISTS team_availability_status;

