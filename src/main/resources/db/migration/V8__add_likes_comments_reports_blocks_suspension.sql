-- V8: Add Likes, Comments, Reports, User Blocks, and Account Suspension

-- 1. Add suspension fields to app_users
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS is_suspended BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS suspended_reason VARCHAR(255);

-- 2. Create listing_likes table
CREATE TABLE IF NOT EXISTS listing_likes (
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, listing_id)
);

CREATE INDEX IF NOT EXISTS idx_listing_likes_listing_id ON listing_likes(listing_id);
CREATE INDEX IF NOT EXISTS idx_listing_likes_user_id ON listing_likes(user_id);

-- 3. Create listing_comments table
CREATE TABLE IF NOT EXISTS listing_comments (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_listing_comments_listing_id ON listing_comments(listing_id);
CREATE INDEX IF NOT EXISTS idx_listing_comments_user_id ON listing_comments(user_id);

-- 4. Create reports table
CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    reported_user_id BIGINT REFERENCES app_users(id) ON DELETE CASCADE,
    reported_listing_id BIGINT REFERENCES listings(id) ON DELETE CASCADE,
    report_type VARCHAR(20) NOT NULL, -- 'USER' or 'LISTING'
    reason VARCHAR(50) NOT NULL,       -- 'SPAM', 'INAPPROPRIATE', 'SCAM', 'HARASSMENT', 'OTHER'
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'RESOLVED', 'DISMISSED'
    action_taken VARCHAR(50),          -- 'USER_SUSPENDED', 'LISTING_DELISTED', 'LISTING_DELETED', 'NO_ACTION'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_type ON reports(report_type);

-- 5. Create user_blocks table
CREATE TABLE IF NOT EXISTS user_blocks (
    blocker_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    blocked_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (blocker_id, blocked_id)
);

CREATE INDEX IF NOT EXISTS idx_user_blocks_blocker_id ON user_blocks(blocker_id);
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked_id ON user_blocks(blocked_id);
