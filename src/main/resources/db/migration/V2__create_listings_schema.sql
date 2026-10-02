-- 2. Room & Flat Listings Table
CREATE TABLE listings (
    id BIGSERIAL PRIMARY KEY,
    host_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    listing_type VARCHAR(20) NOT NULL,           -- 'HAVE_ROOM', 'NEED_ROOM'
    
    -- Financials & Space
    rent_amount INT NOT NULL,
    deposit_amount INT NOT NULL,
    room_type VARCHAR(20) NOT NULL,              -- 'PRIVATE', 'SHARED'
    furnishing_status VARCHAR(20) NOT NULL,      -- 'FURNISHED', 'SEMI_FURNISHED', 'UNFURNISHED'
    
    -- Strict Spatial Coordinates for Listings
    location GEOGRAPHY(Point, 4326) NOT NULL,
    locality_name VARCHAR(150) NOT NULL,        -- e.g., 'Salt Lake, Sector V'
    city VARCHAR(100) NOT NULL,                 -- e.g., 'Kolkata'
    
    description TEXT,
    photo_urls TEXT[] DEFAULT '{}',             -- Direct URLs on S3/R2
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Spatial & Query Indexes
CREATE INDEX idx_listings_location ON listings USING GIST(location);
CREATE INDEX idx_listings_rent ON listings(rent_amount);
CREATE INDEX idx_listings_host_id ON listings(host_user_id);
CREATE INDEX idx_listings_available ON listings(is_available);
