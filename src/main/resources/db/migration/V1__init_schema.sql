CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(128) NOT NULL UNIQUE,
    primary_auth_provider VARCHAR(20) NOT NULL,
    
    -- Mandatory Core (Set during 20-sec signup)
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    date_of_birth DATE NOT NULL,
    avatar_url TEXT,
    
    -- Optional Context (Updated later in Profile)
    occupation_category VARCHAR(30), -- STUDENT, PROFESSIONAL
    occupation_title VARCHAR(100),
    bio VARCHAR(500),
    
    -- Progressive Lifestyle Habits (Nullable until answered)
    smoking_habit VARCHAR(20),       -- NULL until set
    dietary_preference VARCHAR(20),  -- NULL until set
    sleep_schedule VARCHAR(20),      -- NULL until set
    cleanliness_level VARCHAR(20),   -- NULL until set
    guest_tolerance VARCHAR(20),     -- NULL until set
    
    -- Location Context (Last active / search anchor)
    last_known_location GEOGRAPHY(Point, 4326),
    last_known_locality VARCHAR(100) DEFAULT 'Kolkata',
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_users_location ON app_users USING GIST(last_known_location);