
CREATE EXTENSION IF NOT EXISTS vector;



CREATE TABLE master_cvs (
    id                        UUID         PRIMARY KEY,
    user_id                   UUID         NOT NULL UNIQUE,             -- 1:1 з користувачем

    full_name                 VARCHAR(255),
    headline                  VARCHAR(255),                              -- "Junior Java Developer"
    email                     VARCHAR(255),
    phone                     VARCHAR(50),
    linkedin_url              VARCHAR(500),
    github_url                VARCHAR(500),
    summary                   TEXT,

    language                  VARCHAR(10)  NOT NULL,                     -- PL / EN

    source_file_name          VARCHAR(255),                              -- "cv_2026.pdf"
    source_file_hash          VARCHAR(64),                               -- SHA-256 для кешу
    extraction_prompt_version VARCHAR(20),
    extracted_at              TIMESTAMPTZ,

    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_master_cvs_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT check_master_cv_language
        CHECK (language IN ('PL', 'EN'))
);


-- ─── experiences: досвід роботи, N:1 на master_cvs ──────────────────
CREATE TABLE experiences (
    id            UUID         PRIMARY KEY,
    master_cv_id  UUID         NOT NULL,

    position      VARCHAR(255) NOT NULL,
    company       VARCHAR(255) NOT NULL,
    location      VARCHAR(255),
    start_date    DATE         NOT NULL,
    end_date      DATE,                                                  -- null = поточна
    description   TEXT,

    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_experiences_master_cv
        FOREIGN KEY (master_cv_id) REFERENCES master_cvs(id) ON DELETE CASCADE,
    CONSTRAINT check_experience_dates
        CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Найчастіший запит: "усі досвіди мого CV, від найновішого"
CREATE INDEX idx_experiences_master_cv_start
    ON experiences (master_cv_id, start_date DESC);


-- ─── educations: освіта ─────────────────────────────────────────────
CREATE TABLE educations (
    id              UUID         PRIMARY KEY,
    master_cv_id    UUID         NOT NULL,

    institution     VARCHAR(255) NOT NULL,
    degree          VARCHAR(100),                                        -- "Bachelor", "Master"
    field_of_study  VARCHAR(255),
    location        VARCHAR(255),
    start_date      DATE,
    end_date        DATE,
    description     TEXT,

    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_educations_master_cv
        FOREIGN KEY (master_cv_id) REFERENCES master_cvs(id) ON DELETE CASCADE,
    CONSTRAINT check_education_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_educations_master_cv ON educations (master_cv_id);


-- ─── skills: SHARED catalog. Один рядок на всю систему ──────────────
CREATE TABLE skills (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,                            -- нормалізоване, lowercase
    category    VARCHAR(20),                                             -- nullable, бо може бути unknown
    embedding   vector(1536),                                            -- pgvector, OpenAI dims

    created_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT check_skill_category
        CHECK (category IS NULL OR category IN (
            'LANGUAGE', 'FRAMEWORK', 'DATABASE', 'TOOL', 'SOFT', 'OTHER'
        ))
);

-- HNSW index на embedding для швидкого ANN similarity search.
-- vector_cosine_ops — для cosine distance (`<=>` оператор).
-- m=16, ef_construction=64 — defaults, balance швидкості/якості.
CREATE INDEX idx_skills_embedding_hnsw
    ON skills USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Швидкий lookup за name при upsert (хоча UNIQUE вже дає index — для readability)
CREATE INDEX idx_skills_name ON skills (name);


-- ─── master_cv_skills: junction CV ↔ Skill з per-user level ─────────
CREATE TABLE master_cv_skills (
    id            UUID         PRIMARY KEY,
    master_cv_id  UUID         NOT NULL,
    skill_id      UUID         NOT NULL,
    level         VARCHAR(20),                                           -- nullable

    created_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_master_cv_skills_cv
        FOREIGN KEY (master_cv_id) REFERENCES master_cvs(id) ON DELETE CASCADE,
    CONSTRAINT fk_master_cv_skills_skill
        FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    -- Юзер не може двічі додати один скіл
    CONSTRAINT uk_master_cv_skills UNIQUE (master_cv_id, skill_id),
    CONSTRAINT check_skill_level
        CHECK (level IS NULL OR level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'))
);

CREATE INDEX idx_master_cv_skills_master_cv ON master_cv_skills (master_cv_id);
CREATE INDEX idx_master_cv_skills_skill ON master_cv_skills (skill_id);


-- ─── projects: pet-projects, university projects ─────────────────────
CREATE TABLE projects (
    id            UUID         PRIMARY KEY,
    master_cv_id  UUID         NOT NULL,

    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    url           VARCHAR(500),                                          -- GitHub / demo
    technologies  TEXT,                                                  -- CSV: "Java, Spring, Postgres"
    start_date    DATE,
    end_date      DATE,

    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_projects_master_cv
        FOREIGN KEY (master_cv_id) REFERENCES master_cvs(id) ON DELETE CASCADE,
    CONSTRAINT check_project_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_projects_master_cv ON projects (master_cv_id);


-- ─── languages: розмовні мови з рівнем ──────────────────────────────
CREATE TABLE languages (
    id            UUID         PRIMARY KEY,
    master_cv_id  UUID         NOT NULL,

    name          VARCHAR(100) NOT NULL,                                 -- "English", "Polish"
    level         VARCHAR(20)  NOT NULL,                                 -- CEFR + NATIVE

    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_languages_master_cv
        FOREIGN KEY (master_cv_id) REFERENCES master_cvs(id) ON DELETE CASCADE,
    CONSTRAINT check_language_level
        CHECK (level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2', 'NATIVE'))
);

CREATE INDEX idx_languages_master_cv ON languages (master_cv_id);