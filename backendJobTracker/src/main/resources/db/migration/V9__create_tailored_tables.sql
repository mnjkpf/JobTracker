CREATE TABLE tailored_cvs (
    ID UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    version INTEGER NOT NULL,
    FULL_NAME VARCHAR(255) NOT NULL,
    HEADLINE VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    linkedin_url VARCHAR(255) NOT NULL,
    github_url VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    language VARCHAR(10) NOT NULL,
    promt_version VARCHAR(20) NOT NULL,
    
    CREATED_AT TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_tailored_cvs_application
        FOREIGN KEY (application_id)
        REFERENCES applications(id)
        ON DELETE CASCADE,

    CONSTRAINT check_tailored_cvs_language
        CHECK (tonlanguagee IN ('PL', 'EN')),
    CONSTRAINT uk_tailored_cvs_version
        UNIQUE (application_id, version)
);


CREATE INDEX idx_tailored_cvs_application
    ON tailored_cvs (application_id);

CREATE TABLE tailored_educations (
    ID UUID PRIMARY KEY,
    tailored_cv_id UUID NOT NULL,
    institution VARCHAR(255) NOT NULL,
    degree VARCHAR(255) NOT NULL,
    field_of_study VARCHAR(255),
    location VARCHAR(255),
    start_date DATE,
    end_date DATE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_tailored_educations_tailored_cv
        FOREIGN KEY (tailored_cv_id)
        REFERENCES tailored_cvs(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_tailored_educations_tailored_cv
    ON tailored_educations (tailored_cv_id);

CREATE TABLE tailored_skills (
    ID UUID PRIMARY KEY,
    tailored_cv_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    level VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT check_tailored_skills_level
        CHECK (level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2', 'NATIVE')),
    CONSTRAINT fk_tailored_skills_category
        CHECK (category IN ('LANGUAGE', 'FRAMEWORK', 'DATABASE', 'TOOL', 'SOFT', 'OTHER')),

    CONSTRAINT fk_tailored_skills_tailored_cv
        FOREIGN KEY (tailored_cv_id)
        REFERENCES tailored_cvs(id)
        ON DELETE CASCADE
)

CREATE INDEX idx_tailored_skills_tailored_cv
    ON tailored_skills (tailored_cv_id);

CREATE TABLE tailored_experiences (
    ID UUID PRIMARY KEY,
    tailored_cv_id UUID NOT NULL,
    position VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    start_date DATE,
    end_date DATE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_tailored_experiences_tailored_cv
        FOREIGN KEY (tailored_cv_id)
        REFERENCES tailored_cvs(id)
        ON DELETE CASCADE
)

CREATE INDEX idx_tailored_experiences_tailored_cv
    ON tailored_experiences (tailored_cv_id);

CREATE TABLE tailored_projects (
    ID UUID PRIMARY KEY,
    tailored_cv_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    url VARCHAR(255),
    technologies TEXT,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_tailored_projects_tailored_cv
        FOREIGN KEY (tailored_cv_id)
        REFERENCES tailored_cvs(id)
        ON DELETE CASCADE
)

CREATE INDEX idx_tailored_projects_tailored_cv
    ON tailored_projects (tailored_cv_id);

CREATE TABLE tailored_languages (
    ID UUID PRIMARY KEY,
    tailored_cv_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    level VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT check_tailored_languages_level
        CHECK (level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2', 'NATIVE')),
    CONSTRAINT fk_tailored_languages_tailored_cv
        FOREIGN KEY (tailored_cv_id)
        REFERENCES tailored_cvs(id)
        ON DELETE CASCADE
)

CREATE INDEX idx_tailored_languages_tailored_cv
    ON tailored_languages (tailored_cv_id);

