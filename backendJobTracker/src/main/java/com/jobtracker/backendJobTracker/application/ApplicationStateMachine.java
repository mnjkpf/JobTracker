package com.jobtracker.backendJobTracker.application;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.APPLIED;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.FINAL;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.GHOSTED;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.INTERVIEW;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.OFFER;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.REJECTED;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.SAVED;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.SCREENING;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.WITHDRAWN;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;

/**
 * State machine для життєвого циклу Application (заявки на роботу).
 * <p>
 * Централізовано визначає, які переходи між статусами дозволені.
 * Викликається з ApplicationService перед кожним оновленням статусу,
 * щоб бізнес-правила не дублювалися по сервісах і контролерах.
 * <p>
 * @Component — Spring керує єдиним bean-екземпляром, інжектиться через DI.
 */
@Component
public class ApplicationStateMachine {

    /**
     * Граф дозволених переходів: ключ — поточний статус, значення — set статусів,
     * у які з нього можна перейти. Immutable після ініціалізації в static {}.
     */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS;

    static {
        // EnumMap — O(1) lookup, оптимізовано під enum ключі (внутрішньо це масив).
        ALLOWED_TRANSITIONS = new EnumMap<>(ApplicationStatus.class);

        // Початкові та проміжні статуси — кожен має список валідних next-станів.
        // SAVED      — закладка, юзер ще не подався: можна податись або відкликати.
        // APPLIED    — резюме надіслано: чекаємо рекрутера, або відмова/мовчанка/відкликання.
        // SCREENING  — HR-скринінг (телефон/тест): далі інтерв'ю чи терміналка.
        // INTERVIEW  — раунд інтерв'ю: дозволяємо self-loop (наступні раунди), фінал чи термінал.
        // FINAL      — фінальне рішення pending: офер чи термінал.
        ALLOWED_TRANSITIONS.put(SAVED,     EnumSet.of(APPLIED, WITHDRAWN));
        ALLOWED_TRANSITIONS.put(APPLIED,   EnumSet.of(SCREENING, REJECTED, GHOSTED, WITHDRAWN));
        ALLOWED_TRANSITIONS.put(SCREENING, EnumSet.of(INTERVIEW, REJECTED, GHOSTED, WITHDRAWN));
        ALLOWED_TRANSITIONS.put(INTERVIEW, EnumSet.of(INTERVIEW, FINAL, REJECTED, GHOSTED, WITHDRAWN));
        ALLOWED_TRANSITIONS.put(FINAL,     EnumSet.of(OFFER, REJECTED, GHOSTED, WITHDRAWN));

        // Термінальні статуси — порожні sets: з них нікуди не переходимо.
        // OFFER     — отримано офер (success-кінець).
        // REJECTED  — відмова компанією.
        // GHOSTED   — рекрутер замовк (явний "тихий" termination).
        // WITHDRAWN — юзер сам відкликав заявку.
        ALLOWED_TRANSITIONS.put(OFFER,     EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(REJECTED,  EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(GHOSTED,   EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED_TRANSITIONS.put(WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class));
    }

    public boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == to) {
            // Self-loop: INTERVIEW → INTERVIEW дозволено (новий раунд інтерв'ю),
            // для решти статусів no-op переходи заборонені (зайвий API-виклик / помилка).
            return from == INTERVIEW;
        }
        // getOrDefault із порожнім сетом — захист від null, якщо хтось додасть новий status,
        // але забуде зареєструвати його в ALLOWED_TRANSITIONS.
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * Жорстка перевірка переходу — кидає BusinessRuleException якщо невалідно.
     * Викликається з ApplicationService перед збереженням нового статусу;
     * GlobalExceptionHandler перетворює виключення на HTTP 422 ProblemDetail.
     */
    public void validateTransition(ApplicationStatus from, ApplicationStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessRuleException(
                "Invalid status transition: " + from + " → " + to
            );
        }
    }
}
