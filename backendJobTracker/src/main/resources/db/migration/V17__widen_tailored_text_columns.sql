-- Production: "value too long for type character varying(255)" on
-- tailored_projects insert. The LLM generates free-text descriptions
-- (especially in Polish) that regularly exceed 255 chars, but several
-- LLM-generated columns were left as VARCHAR(255) in V10. Widen every
-- free-text column with no natural length limit to TEXT.
--
-- Columns like name/url/company/institution/level/category are left
-- untouched — they're short, structured values, not LLM prose.

ALTER TABLE tailored_educations ALTER COLUMN description TYPE TEXT;
ALTER TABLE tailored_experiences ALTER COLUMN description TYPE TEXT;
ALTER TABLE tailored_projects ALTER COLUMN description TYPE TEXT;
ALTER TABLE tailored_cvs ALTER COLUMN headline TYPE TEXT;

-- master_cvs.headline has the same LLM-generated, unbounded-length shape
-- (extracted/rewritten headline tagline) and the same VARCHAR(255) limit.
ALTER TABLE master_cvs ALTER COLUMN headline TYPE TEXT;
