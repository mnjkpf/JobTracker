CREATE TABLE application_cvs (
    id             UUID         PRIMARY KEY,
    application_id UUID         NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
    file_name      VARCHAR(255) NOT NULL,
    content_type   VARCHAR(255) NOT NULL,
    file_size      BIGINT       NOT NULL,
    file_bytes     BYTEA        NOT NULL,
    extracted_text TEXT,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT check_application_cvs_content_type
        CHECK (content_type IN (
            'application/pdf',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document'))
);

CREATE INDEX idx_application_cvs_application ON application_cvs (application_id);
