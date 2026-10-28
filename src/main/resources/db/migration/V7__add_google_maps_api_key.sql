-- V7: Add Google Maps API key to app_branding table
ALTER TABLE app_branding
    ADD COLUMN IF NOT EXISTS google_maps_api_key VARCHAR(200);
