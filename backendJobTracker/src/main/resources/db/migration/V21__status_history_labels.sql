-- History must survive status rename/delete, so store text label snapshots
-- instead of enum values.
ALTER TABLE application_status_history ADD COLUMN from_label VARCHAR(100);
ALTER TABLE application_status_history ADD COLUMN to_label   VARCHAR(100);

UPDATE application_status_history SET from_label = from_status WHERE from_status IS NOT NULL;
UPDATE application_status_history SET to_label   = to_status;

ALTER TABLE application_status_history ALTER COLUMN to_label SET NOT NULL;

-- Drop the old enum columns (also drops their CHECK constraints).
ALTER TABLE application_status_history DROP COLUMN from_status;
ALTER TABLE application_status_history DROP COLUMN to_status;
