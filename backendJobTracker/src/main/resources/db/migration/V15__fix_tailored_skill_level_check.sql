-- check_tailored_skills_level wrongly used CEFR language levels (A1..NATIVE),
-- copy-pasted from tailored_languages. Skill level is BEGINNER/INTERMEDIATE/
-- ADVANCED/EXPERT (or null). Replace the constraint with the correct set.
ALTER TABLE tailored_skills DROP CONSTRAINT check_tailored_skills_level;
ALTER TABLE tailored_skills ADD CONSTRAINT check_tailored_skills_level
    CHECK (level IS NULL OR level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'));
