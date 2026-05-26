CREATE TABLE applications (
    id              UUID         PRIMARY KEY,
    user_id         UUID         NOT NULL,
    company_id      UUID         NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    notes           TEXT,
    url             VARCHAR(2048) NOT NULL,
    status          VARCHAR(50)  NOT NULL,
    contract_type   VARCHAR(50)  NOT NULL,
    seniority       VARCHAR(50)  NOT NULL,
    work_mode       VARCHAR(50)  NOT NULL,
    source_board    VARCHAR(50)  NOT NULL,
    location        VARCHAR(255),
    salary_min      INTEGER,
    salary_max      INTEGER,
    salary_currency VARCHAR(3),
    applied_at      TIMESTAMPTZ,
    archived        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_applications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_applications_company
        FOREIGN KEY (company_id)
        REFERENCES companies(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_applications_user_url
        UNIQUE (user_id, url),

    CONSTRAINT check_application_status
        CHECK (status IN (
            'SAVED', 'APPLIED', 'SCREENING', 'INTERVIEW',
            'FINAL', 'OFFER', 'REJECTED', 'GHOSTED', 'WITHDRAWN'
        )),

    CONSTRAINT check_application_contract_type
        CHECK (contract_type IN ('UOP', 'B2B', 'UZ', 'UMOWA_O_DZIELO', 'NOT_SPECIFIED')),

    CONSTRAINT check_application_seniority
        CHECK (seniority IN ('INTERN', 'JUNIOR', 'JUNIOR_PLUS', 'MID', 'SENIOR', 'LEAD')),

    CONSTRAINT check_application_work_mode
        CHECK (work_mode IN ('ONSITE', 'HYBRID', 'REMOTE', 'NOT_SPECIFIED')),

    CONSTRAINT check_application_source_board
        CHECK (source_board IN (
            'NOFLUFFJOBS', 'JUSTJOINIT', 'PRACUJ',
            'LINKEDIN', 'COMPANY_SITE', 'OTHER', 'MANUAL'
        )),

    CONSTRAINT check_application_salary_range
        CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
);


CREATE INDEX idx_applications_user_active_created
    ON applications (user_id, created_at DESC)
    WHERE archived = FALSE;


CREATE INDEX idx_applications_user_status
    ON applications (user_id, status)
    WHERE archived = FALSE;


CREATE INDEX idx_applications_company_id ON applications (company_id);