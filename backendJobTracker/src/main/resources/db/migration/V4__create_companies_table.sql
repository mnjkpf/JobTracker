CREATE TABLE companies (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    website     VARCHAR(255),
    industry    VARCHAR(100),
    description TEXT,
    size        VARCHAR(50),
    user_id     UUID         NOT NULL,

    CONSTRAINT fk_companies_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_companies_user_name
        UNIQUE (user_id, name),

    CONSTRAINT check_company_size
        CHECK (size IS NULL OR size IN ('STARTUP', 'SMALL', 'MEDIUM', 'LARGE', 'ENTERPRISE'))
);


CREATE INDEX idx_companies_user_id ON companies (user_id);