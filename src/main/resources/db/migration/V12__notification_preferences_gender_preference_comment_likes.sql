-- V12: Granular notification preferences, listing gender preference, geocoded
--      address fields and comment likes.

-- ------------------------------------------------------------------
-- 1. Per-user notification preferences (replaces the two booleans on app_users)
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    user_id               BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    notify_likes_comments BOOLEAN NOT NULL DEFAULT TRUE,
    notify_messages       BOOLEAN NOT NULL DEFAULT TRUE,
    notify_seekers_nearby BOOLEAN NOT NULL DEFAULT TRUE,
    notify_rooms_nearby   BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Carry over the legacy toggles: room-finder alerts -> rooms nearby,
-- roommate-finder alerts -> seekers nearby.
INSERT INTO user_notification_preferences (user_id, notify_seekers_nearby, notify_rooms_nearby)
SELECT id,
       COALESCE(notify_roommate_finder, TRUE),
       COALESCE(notify_room_finder, TRUE)
FROM app_users
ON CONFLICT (user_id) DO NOTHING;

ALTER TABLE app_users
    DROP COLUMN IF EXISTS notify_room_finder,
    DROP COLUMN IF EXISTS notify_roommate_finder;

-- ------------------------------------------------------------------
-- 2. Listing gender preference (ALL / MALE / FEMALE / NON_BINARY / OTHER) replaces
--    the open_to_all_genders flag. A listing that was not open to all is assumed
--    to prefer the host's own gender.
-- ------------------------------------------------------------------
ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS gender_preference VARCHAR(20) NOT NULL DEFAULT 'ALL';

UPDATE listings l
SET gender_preference = CASE
        WHEN l.open_to_all_genders THEN 'ALL'
        WHEN u.gender IN ('MALE', 'FEMALE', 'NON_BINARY') THEN u.gender
        WHEN u.gender = 'OTHERS' THEN 'OTHER'
        ELSE 'ALL'
    END
FROM app_users u
WHERE u.id = l.host_user_id;

ALTER TABLE listings DROP COLUMN IF EXISTS open_to_all_genders;

CREATE INDEX IF NOT EXISTS idx_listings_gender_preference ON listings(gender_preference);

-- Reverse-geocoded address parts captured by the create-room wizard.
ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS state_name  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);

-- ------------------------------------------------------------------
-- 3. Comment likes (the heart micro-action on each comment tile)
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS listing_comment_likes (
    comment_id BIGINT NOT NULL REFERENCES listing_comments(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_listing_comment_likes_user_id ON listing_comment_likes(user_id);
CREATE INDEX IF NOT EXISTS idx_listing_comments_created_at ON listing_comments(created_at DESC);
