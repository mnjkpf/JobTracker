CREATE TABLE cover_letters (
    id              UUID         PRIMARY KEY,
    application_id  UUID         NOT NULL,
    content         TEXT         NOT NULL,
    version         INTEGER      NOT NULL,
    tone            VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,                -- ВИПРАВЛЕНО: було TIMESTAMPZ (typo)
 
    CONSTRAINT fk_cover_letters_application
        FOREIGN KEY (application_id)
        REFERENCES applications(id)
        ON DELETE CASCADE,
 
    CONSTRAINT check_cover_letter_tone
        CHECK (tone IN ('FORMAL', 'CONVERSATIONAL', 'ENTHUSIASTIC')),
 
    
    CONSTRAINT uk_cover_letters_app_version
        UNIQUE (application_id, version)
);

 

CREATE INDEX idx_cover_letters_application
    ON cover_letters (application_id);
 
