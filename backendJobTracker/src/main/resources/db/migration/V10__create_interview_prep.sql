
CREATE TABLE interview_preps (
    id              UUID         PRIMARY KEY,
    application_id  UUID         NOT NULL UNIQUE,         
    status          VARCHAR(50)  NOT NULL,
    
    prompt_version  VARCHAR(20),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_interview_preps_application
        FOREIGN KEY (application_id)
        REFERENCES applications(id)
        ON DELETE CASCADE,                                

    CONSTRAINT check_interview_preps_status
        CHECK (status IN ('DRAFT', 'GENERATED', 'COMPLETED'))
);                                                     

CREATE INDEX idx_interview_preps_application
    ON interview_preps (application_id);



CREATE TABLE interview_questions (
    id                  UUID         PRIMARY KEY,
    interview_prep_id   UUID         NOT NULL,
    category            VARCHAR(50)  NOT NULL,
    question            TEXT         NOT NULL,
    
    suggested_answer    TEXT,
    display_order       INTEGER      NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    -- ВИДАЛЕНО updated_at — InterviewQuestion immutable, regenerate = delete+insert

    CONSTRAINT fk_interview_questions_interview_prep
        FOREIGN KEY (interview_prep_id)
        REFERENCES interview_preps(id)
        ON DELETE CASCADE,                               

    CONSTRAINT check_interview_questions_category
        CHECK (category IN ('TECHNICAL', 'BEHAVIORAL', 'QUESTION_TO_ASK'))
);                                                        

CREATE INDEX idx_interview_questions_interview_prep
    ON interview_questions (interview_prep_id);

-- Композитний index для query "усі питання категорії" (нечасто треба, але дешево)
CREATE INDEX idx_interview_questions_prep_category
    ON interview_questions (interview_prep_id, category);