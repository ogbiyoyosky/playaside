-- Add manual draft state tracking columns to matches table
ALTER TABLE matches
    ADD COLUMN draft_in_progress BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE matches
    ADD COLUMN current_picking_team_id UUID REFERENCES teams(id) ON DELETE SET NULL;

ALTER TABLE matches
    ADD COLUMN manual_draft_order TEXT;

ALTER TABLE matches
    ADD COLUMN manual_draft_index INTEGER NOT NULL DEFAULT 0;

