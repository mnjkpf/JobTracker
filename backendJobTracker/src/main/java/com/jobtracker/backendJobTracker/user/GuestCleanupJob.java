package com.jobtracker.backendJobTracker.user;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.application.ApplicationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Видаляє гостьові акаунти, що згоріли, разом з усіма їхніми даними.
 * <p>
 * Без цього кожен відвідувач сайту назавжди лишав би рядок у {@code users} плюс
 * заявки й BYTEA-файли CV, яких ніхто не зареєстрував.
 */
@Component
@RequiredArgsConstructor
public class GuestCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuestCleanupJob.class);

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    /** Щодня о 03:17 — поза піком, зі зміщеною хвилиною, щоб не збігатись з іншими джобами. */
    @Scheduled(cron = "0 17 3 * * *")
    @Transactional
    public void deleteExpiredGuests() {
        List<User> expired = userRepository
                .findByIsGuestTrueAndGuestExpiresAtBefore(Instant.now());

        if (expired.isEmpty()) {
            return;
        }

        List<UUID> ids = expired.stream().map(User::getId).toList();

        // Заявки видаляємо явно ПЕРЕД юзером. Причина: applications.status_id має
        // ON DELETE RESTRICT на status_categories, а при каскаді від users Postgres
        // не гарантує, у якому порядку зачистить дві дочірні таблиці — якби
        // status_categories пішли першими, RESTRICT завалив би всю операцію.
        // Решта (cover letters, tailored CVs, interview preps, application CVs,
        // status history, skills) підчищається каскадом від applications.
        applicationRepository.deleteByUserIdIn(ids);
        userRepository.deleteAll(expired);

        LOGGER.info("Deleted {} expired guest account(s)", expired.size());
    }
}
