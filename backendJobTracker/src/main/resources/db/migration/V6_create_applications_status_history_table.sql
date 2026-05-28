CREATE TABLE application_status_history (
    id              UUID         PRIMARY KEY,
    application_id  UUID         NOT NULL,
    from_status     VARCHAR(50),                          
    to_status       VARCHAR(50)  NOT NULL,
    note            TEXT,
    changed_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_status_history_application
        FOREIGN KEY (application_id)
        REFERENCES applications(id)
        ON DELETE CASCADE,                                
    CONSTRAINT check_status_history_from_status
        CHECK (from_status IS NULL OR from_status IN (
            'SAVED', 'APPLIED', 'SCREENING', 'INTERVIEW',
            'FINAL', 'OFFER', 'REJECTED', 'GHOSTED', 'WITHDRAWN'
        )),

    CONSTRAINT check_status_history_to_status
        CHECK (to_status IN (
            'SAVED', 'APPLIED', 'SCREENING', 'INTERVIEW',
            'FINAL', 'OFFER', 'REJECTED', 'GHOSTED', 'WITHDRAWN'
        ))
);


CREATE INDEX idx_status_history_application_changed_at
    ON application_status_history (application_id, changed_at ASC);


CREATE INDEX idx_status_history_to_status_changed_at
    ON application_status_history (to_status, changed_at DESC);