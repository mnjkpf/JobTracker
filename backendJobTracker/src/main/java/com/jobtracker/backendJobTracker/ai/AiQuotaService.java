package com.jobtracker.backendJobTracker.ai;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.config.JobTrackerProperties;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ліміт AI-генерацій для гостьових акаунтів.
 * <p>
 * Гість не реєструвався, але вже витрачає платні токени Anthropic, тому йому
 * дається фіксована кількість генерацій ({@code jobtracker.guest.ai-generation-limit}),
 * після чого залишається тільки зареєструватись. Для зареєстрованих лічильник
 * ведеться, але ліміт не застосовується.
 * <p>
 * Враховуються лише три «дорогі» генерації — cover letter, tailored CV та interview
 * prep. Gap analysis свідомо безлімітний: він робить одну дешеву екстракцію скілів,
 * кешується в Redis, і без нього гість не побачить сенсу трекера взагалі.
 */
@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private final UserRepository userRepository;
    private final JobTrackerProperties properties;

    /**
     * Списує одну генерацію з квоти. Кидає {@link BusinessRuleException} (422),
     * якщо гість уже вичерпав ліміт.
     */
    @Transactional
    public void consumeGeneration(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Account not found"));

        if (user.isGuest()) {
            int limit = properties.guest().aiGenerationLimit();
            if (user.getAiGenerationsUsed() >= limit) {
                throw new BusinessRuleException(
                        "Guest accounts are limited to " + limit + " AI generations. "
                                + "Create an account to keep generating — everything you "
                                + "already added stays on your board.");
            }
        }

        user.setAiGenerationsUsed(user.getAiGenerationsUsed() + 1);
        userRepository.save(user);
    }

    /** Скільки генерацій ще доступно гостю; {@code null} = без обмежень. */
    @Transactional(readOnly = true)
    public Integer remainingGenerations(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Account not found"));
        if (!user.isGuest()) {
            return null;
        }
        int left = properties.guest().aiGenerationLimit() - user.getAiGenerationsUsed();
        return Math.max(left, 0);
    }
}
