-- Milestone 9: Room Finder Search Preferences & FCM Notifications
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512),
    ADD COLUMN IF NOT EXISTS target_search_location geography(Point, 4326),
    ADD COLUMN IF NOT EXISTS target_search_locality VARCHAR(100),
    ADD COLUMN IF NOT EXISTS search_radius_km DOUBLE PRECISION DEFAULT 10.0,
    ADD COLUMN IF NOT EXISTS is_looking_for_room BOOLEAN DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_app_users_target_search_location
    ON app_users USING GIST (target_search_location);
