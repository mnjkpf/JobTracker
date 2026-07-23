package com.jobtracker.backendJobTracker.application;

import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.APPLIED;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.FINAL;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.GHOSTED;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.INTERVIEW;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.OFFER;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.REJECTED;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.SAVED;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.SCREENING;
import static com.jobtracker.backendJobTracker.application.enums.ApplicationStatus.WITHDRAWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;

/**
 * Unit-тести для {@link ApplicationStateMachine}.
 * <p>
 * Стратегія: дублюємо очікуваний граф дозволених переходів незалежно від SUT
 * і прогоняємо ВСІ 9×9 = 81 комбінацій (from × to). Кожна комбінація:
 *  - canTransition() має дорівнювати "пара є у дозволеному графі",
 *  - validateTransition() кидає BusinessRuleException рівно тоді, коли перехід недозволений.
 * <p>
 * Це автоматично покриває: ~30 валідних, ~51 невалідний перехід, self-loops
 * (INTERVIEW→INTERVIEW дозволено, решта X→X — ні) і термінальні статуси.
 */
class ApplicationStateMachineTest {

    private final ApplicationStateMachine stateMachine = new ApplicationStateMachine();

    /** Очікуваний граф — навмисно НЕ імпортується з SUT, щоб тест ловив зміни логіки. */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> EXPECTED = Map.of(
            SAVED, Set.of(APPLIED, WITHDRAWN),
            APPLIED, Set.of(SCREENING, REJECTED, GHOSTED, WITHDRAWN),
            SCREENING, Set.of(INTERVIEW, REJECTED, GHOSTED, WITHDRAWN),
            INTERVIEW, Set.of(INTERVIEW, FINAL, REJECTED, GHOSTED, WITHDRAWN),
            FINAL, Set.of(OFFER, REJECTED, GHOSTED, WITHDRAWN),
            OFFER, Set.of(),
            REJECTED, Set.of(),
            GHOSTED, Set.of(),
            WITHDRAWN, Set.of());

    private static boolean isAllowed(ApplicationStatus from, ApplicationStatus to) {
        return EXPECTED.getOrDefault(from, Set.of()).contains(to);
    }

    static Stream<Arguments> allCombinations() {
        return Stream.of(ApplicationStatus.values())
                .flatMap(from -> Stream.of(ApplicationStatus.values())
                        .map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "canTransition({0} -> {1})")
    @MethodSource("allCombinations")
    @DisplayName("canTransition відповідає очікуваному графу для всіх 81 комбінацій")
    void canTransition_matchesGraph(ApplicationStatus from, ApplicationStatus to) {
        assertThat(stateMachine.canTransition(from, to))
                .as("%s -> %s", from, to)
                .isEqualTo(isAllowed(from, to));
    }

    @ParameterizedTest(name = "validateTransition({0} -> {1})")
    @MethodSource("allCombinations")
    @DisplayName("validateTransition кидає BusinessRuleException саме на недозволених переходах")
    void validateTransition_throwsOnlyWhenInvalid(ApplicationStatus from, ApplicationStatus to) {
        if (isAllowed(from, to)) {
            assertThatNoException().isThrownBy(() -> stateMachine.validateTransition(from, to));
        } else {
            assertThatThrownBy(() -> stateMachine.validateTransition(from, to))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid status transition");
        }
    }

    @ParameterizedTest(name = "{0} -> {0} (self)")
    @MethodSource("selfTransitions")
    @DisplayName("Self-transition дозволено лише для INTERVIEW (наступний раунд)")
    void selfTransition_onlyInterviewAllowed(ApplicationStatus status) {
        assertThat(stateMachine.canTransition(status, status))
                .isEqualTo(status == INTERVIEW);
    }

    static Stream<ApplicationStatus> selfTransitions() {
        return Stream.of(ApplicationStatus.values());
    }
}
