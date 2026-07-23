-- Tailored CV sub-entities may legitimately miss these fields (an education
-- without a formal degree name, an uncategorised skill, a language without a
-- CEFR level). V10 made them NOT NULL, which blocks generation. Relax them.
ALTER TABLE tailored_educations ALTER COLUMN degree DROP NOT NULL;
ALTER TABLE tailored_skills ALTER COLUMN category DROP NOT NULL;
ALTER TABLE tailored_languages ALTER COLUMN level DROP NOT NULL;
