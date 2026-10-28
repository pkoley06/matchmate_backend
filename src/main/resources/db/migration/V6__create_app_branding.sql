CREATE TABLE app_branding (
    id BIGSERIAL PRIMARY KEY,
    app_name VARCHAR(100) NOT NULL DEFAULT 'MatchMate',
    tagline VARCHAR(255) DEFAULT 'Find your ideal flatmate & room',
    primary_color VARCHAR(10) NOT NULL DEFAULT '#6750A4',
    logo_url VARCHAR(500),
    splash_image_url VARCHAR(500),
    support_email VARCHAR(100) DEFAULT 'support@matchmate.com',
    terms_url VARCHAR(500),
    privacy_url VARCHAR(500),
    maintenance_mode BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO app_branding (id, app_name, tagline, primary_color, support_email)
VALUES (1, 'MatchMate', 'Find your ideal flatmate & room', '#6750A4', 'support@matchmate.com');
