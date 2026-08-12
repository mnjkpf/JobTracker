-- V13 relaxed phone/linkedin_url/github_url on tailored_cvs but missed the rest
-- of the hero block. The TailoredCv entity has never marked full_name, headline,
-- email or summary as nullable=false, the LLM prompt schema explicitly allows
-- "headline": string|null and "summary": string|null, and TailoredCvService
-- coalesces parsed -> master with no blank guard (unlike every attach* method
-- for sub-entities) — so whenever both the LLM output AND the master CV lack
-- one of these (headline/summary are optional on master_cvs), the INSERT hits
-- this leftover V10 NOT NULL constraint and generation fails with a 500.
ALTER TABLE tailored_cvs ALTER COLUMN full_name DROP NOT NULL;
ALTER TABLE tailored_cvs ALTER COLUMN headline DROP NOT NULL;
ALTER TABLE tailored_cvs ALTER COLUMN email DROP NOT NULL;
ALTER TABLE tailored_cvs ALTER COLUMN summary DROP NOT NULL;
