DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'location_stock' AND column_name = 'version'
    ) THEN
        ALTER TABLE location_stock
            ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
    END IF;
END $$;
