CREATE TABLE status_categories (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL,
    name         VARCHAR(100) NOT NULL,
    color        VARCHAR(20)  NOT NULL,
    position     INTEGER      NOT NULL,
    system_type  VARCHAR(20),
    is_terminal  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_status_categories_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_status_categories_user_name UNIQUE (user_id, name),
    CONSTRAINT check_status_categories_system_type
        CHECK (system_type IS NULL OR system_type IN (
            'SAVED','APPLIED','SCREENING','INTERVIEW','FINAL',
            'OFFER','REJECTED','WITHDRAWN','GHOSTED'))
);

CREATE INDEX idx_status_categories_user_position
    ON status_categories (user_id, position);

-- Seed the 9 default statuses for every existing user. PG13+ has gen_random_uuid().
INSERT INTO status_categories
    (id, user_id, name, color, position, system_type, is_terminal, created_at, updated_at)
SELECT gen_random_uuid(), u.id, d.name, d.color, d.position, d.system_type, d.is_terminal,
       now(), now()
FROM users u
CROSS JOIN (VALUES
    ('Saved',     '#64748b', 0, 'SAVED',     FALSE),
    ('Applied',   '#3b82f6', 1, 'APPLIED',   FALSE),
    ('Screening', '#6366f1', 2, 'SCREENING', FALSE),
    ('Interview', '#a855f7', 3, 'INTERVIEW', FALSE),
    ('Final',     '#f59e0b', 4, 'FINAL',     FALSE),
    ('Offer',     '#22c55e', 5, 'OFFER',     TRUE),
    ('Rejected',  '#ef4444', 6, 'REJECTED',  TRUE),
    ('Withdrawn', '#9ca3af', 7, 'WITHDRAWN', TRUE),
    ('Ghosted',   '#9ca3af', 8, 'GHOSTED',   TRUE)
) AS d(name, color, position, system_type, is_terminal);
