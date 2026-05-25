CREATE TABLE verification_tokens (
    id          UUID         PRIMARY KEY,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    token_type  VARCHAR(50)  NOT NULL,
    user_id     UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,                                -- nullable, single-use enforcement
    CONSTRAINT fk_verification_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT check_token_type
        CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);
 
-- Active tokens юзера певного типу — найчастіший запит при invalidateAllForUser
CREATE INDEX idx_verification_tokens_user_active
    ON verification_tokens (user_id, token_type)
    WHERE used_at IS NULL;
 
-- Cleanup job може періодично видаляти used + expired
CREATE INDEX idx_verification_tokens_expires_at
    ON verification_tokens (expires_at)
    WHERE used_at IS NULL;
 
