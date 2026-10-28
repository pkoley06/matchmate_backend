-- Milestone 10: Notification Toggles & Developer Support (Buy Me a Coffee)
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS notify_room_finder BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS notify_roommate_finder BOOLEAN DEFAULT TRUE;

ALTER TABLE app_branding
    ADD COLUMN IF NOT EXISTS developer_upi_id VARCHAR(100) DEFAULT 'matchmate.dev@upi',
    ADD COLUMN IF NOT EXISTS developer_qr_code_url TEXT,
    ADD COLUMN IF NOT EXISTS developer_support_note TEXT DEFAULT 'Made with ❤️ for flatmates everywhere. If MatchMate helped you find a home, buy the developer a coffee!';
