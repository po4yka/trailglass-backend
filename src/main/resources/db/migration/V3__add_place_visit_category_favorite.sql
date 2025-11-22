-- Add category and is_favorite fields to place_visits table

ALTER TABLE place_visits
    ADD COLUMN category VARCHAR(50),
    ADD COLUMN is_favorite BOOLEAN NOT NULL DEFAULT false;

-- Create index for category filtering
CREATE INDEX idx_place_visits_category ON place_visits(user_id, category) WHERE category IS NOT NULL;

-- Create index for favorite filtering
CREATE INDEX idx_place_visits_favorite ON place_visits(user_id, is_favorite) WHERE is_favorite = true;
