
CREATE TABLE interview_notes (
    id                  UUID         PRIMARY KEY,

    
    interview_prep_id   UUID         NOT NULL,

    
    embedding           vector(1536),

    
    prompt_version      VARCHAR(20),

    
    note_type           VARCHAR(20)  NOT NULL,

    
    content             TEXT         NOT NULL,

    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_interview_notes_interview_prep
        FOREIGN KEY (interview_prep_id)
        REFERENCES interview_preps(id)
        ON DELETE CASCADE,

    CONSTRAINT check_interview_notes_note_type
        CHECK (note_type IN ('PREP_NOTE', 'POST_INTERVIEW'))
);

-- Найчастіший запит: "усі нотатки одного prep" (для GET list)
CREATE INDEX idx_interview_notes_prep
    ON interview_notes (interview_prep_id);


CREATE INDEX idx_interview_notes_embedding
    ON interview_notes
    USING hnsw (embedding vector_cosine_ops);