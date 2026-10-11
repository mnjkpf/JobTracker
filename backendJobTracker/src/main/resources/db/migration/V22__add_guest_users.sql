-- Guest-режим: анонімний акаунт створюється при першому заході на сайт і живе
-- обмежений час. При реєстрації той самий рядок users отримує email+пароль,
-- тому всі заявки/CV/генерації залишаються на місці — міграції даних немає.

ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL,
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN is_guest            BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN guest_expires_at    TIMESTAMPTZ,
    ADD COLUMN ai_generations_used INTEGER NOT NULL DEFAULT 0;

-- Гарантія цілісності на рівні БД: гість не має креденшлів і мусить мати дату
-- згоряння; зареєстрований мусить мати і email, і хеш паролю.
ALTER TABLE users
    ADD CONSTRAINT check_users_guest_or_credentials CHECK (
        (is_guest = TRUE
            AND email IS NULL
            AND password_hash IS NULL
            AND guest_expires_at IS NOT NULL)
        OR
        (is_guest = FALSE
            AND email IS NOT NULL
            AND password_hash IS NOT NULL)
    );

-- Partial index — cleanup-job сканує лише гостей, а не всю таблицю.
CREATE INDEX idx_users_guest_expires
    ON users (guest_expires_at)
    WHERE is_guest = TRUE;
