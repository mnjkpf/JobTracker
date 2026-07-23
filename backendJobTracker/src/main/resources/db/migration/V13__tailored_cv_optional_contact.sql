-- Contact URLs/phone are optional: not every candidate has LinkedIn/GitHub or a
-- phone on their master CV, so a tailored CV must be insertable without them.
-- (Original V10 made these NOT NULL, which blocks generation for such CVs.)
ALTER TABLE tailored_cvs ALTER COLUMN linkedin_url DROP NOT NULL;
ALTER TABLE tailored_cvs ALTER COLUMN github_url DROP NOT NULL;
ALTER TABLE tailored_cvs ALTER COLUMN phone DROP NOT NULL;
