-- Fields needed by the Figma "Create listing" flow (Details step) that the
-- original minimal listing schema didn't carry: a short title, an optional
-- move-in date, a free-form amenities list, and a gender-openness flag.
ALTER TABLE listings
    ADD COLUMN title VARCHAR(150),
    ADD COLUMN move_in_date DATE,
    ADD COLUMN amenities TEXT[] DEFAULT '{}',
    ADD COLUMN open_to_all_genders BOOLEAN NOT NULL DEFAULT TRUE;
