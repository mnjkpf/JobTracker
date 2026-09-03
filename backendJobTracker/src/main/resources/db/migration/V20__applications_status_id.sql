-- Move applications from the fixed status enum column to a FK into status_categories.
ALTER TABLE applications ADD COLUMN status_id UUID;

-- Map each application's old enum value to the same user's seeded status of that system_type.
UPDATE applications a
SET status_id = s.id
FROM status_categories s
WHERE s.user_id = a.user_id
  AND s.system_type = a.status;

ALTER TABLE applications ALTER COLUMN status_id SET NOT NULL;
ALTER TABLE applications
    ADD CONSTRAINT fk_applications_status
    FOREIGN KEY (status_id) REFERENCES status_categories(id) ON DELETE RESTRICT;

-- Drop the old enum column (also drops its CHECK constraint).
ALTER TABLE applications DROP COLUMN status;

CREATE INDEX idx_applications_user_status_id
    ON applications (user_id, status_id)
    WHERE archived = FALSE;
