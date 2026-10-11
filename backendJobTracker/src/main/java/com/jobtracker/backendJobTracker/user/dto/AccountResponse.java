package com.jobtracker.backendJobTracker.user.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * Стан поточного акаунта для фронтенду: чи це гість, коли згорає його сесія
 * і скільки AI-генерацій йому лишилось.
 */
@Getter
@Setter
public class AccountResponse {

    private UUID id;

    /** Null для гостьового акаунта. */
    private String email;

    private String displayName;

    /** true → акаунт анонімний, дані згорять і AI обмежений. */
    private boolean guest;

    /** Коли згорає гостьова сесія. Null для зареєстрованих. */
    private Instant guestExpiresAt;

    /** Скільки AI-генерацій лишилось гостю. Null → без обмежень. */
    private Integer aiGenerationsRemaining;
}
