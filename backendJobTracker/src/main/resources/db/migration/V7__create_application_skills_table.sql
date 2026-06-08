

CREATE TABLE application_skills (
    id              UUID         PRIMARY KEY,
    application_id  UUID         NOT NULL,
    skill_id        UUID         NOT NULL,
    required        BOOLEAN      NOT NULL,                -- true = must-have, false = nice-to-have
    created_at      TIMESTAMPTZ  NOT NULL,                

    CONSTRAINT fk_application_skills_application
        FOREIGN KEY (application_id)
        REFERENCES applications(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_application_skills_skill
        FOREIGN KEY (skill_id)
        REFERENCES skills(id)
        ON DELETE CASCADE,

    
    CONSTRAINT uk_application_skills
        UNIQUE (application_id, skill_id)
);


CREATE INDEX idx_application_skills_application
    ON application_skills (application_id);


CREATE INDEX idx_application_skills_skill
    ON application_skills (skill_id);