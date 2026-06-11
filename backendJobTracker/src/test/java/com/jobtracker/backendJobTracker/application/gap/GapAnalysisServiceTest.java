package com.jobtracker.backendJobTracker.application.gap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.application.gap.dto.GapAnalysisResponse;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;

/**
 * Unit-тести для {@link GapAnalysisService} з Mockito.
 * <p>
 * Складність: {@code findBestCvMatch} читає результат через приватний record
 * {@code MatchRow}, який тест не може інстанціювати. Тому ми перехоплюємо
 * {@link RowMapper}, переданий у {@link JdbcTemplate#query}, і виконуємо його
 * проти mock-{@link ResultSet} — так отримуємо реальний рядок без посилання
 * на приватний тип. Distance керує similarity (= 1 - distance) і порогом 0.75.
 */
@ExtendWith(MockitoExtension.class)
class GapAnalysisServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationSkillRepository applicationSkillRepository;
    @Mock private MasterCvRepository masterCvRepository;
    @Mock private JobSkillExtractor jobSkillExtractor;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private GapAnalysisService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID appId = UUID.randomUUID();

    private Application ownedApp() {
        Application app = new Application();
        app.setId(appId);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        return app;
    }

    private ApplicationSkill jobSkill(String name, boolean required) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName(name);
        ApplicationSkill as = new ApplicationSkill();
        as.setSkill(skill);
        as.setRequired(required);
        return as;
    }

    /**
     * Стабимо jdbcTemplate.query так, що distance залежить від id required-скіла.
     * distanceById.get(id) == null -> повертаємо порожній список (немає матчу).
     */
    @SuppressWarnings("unchecked")
    private void stubMatchQuery(java.util.Map<UUID, Double> distanceById) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any())).thenAnswer(inv -> {
            RowMapper<?> mapper = inv.getArgument(1);
            UUID jobSkillId = inv.getArgument(2);
            Double distance = distanceById.get(jobSkillId);
            if (distance == null) {
                return List.of();
            }
            ResultSet rs = mock(ResultSet.class);
            lenient().when(rs.getString("cv_skill_name")).thenReturn("Spring Boot");
            lenient().when(rs.getDouble("distance")).thenReturn(distance);
            Object row = mapper.mapRow(rs, 0);
            return new java.util.ArrayList<>(List.of(row));
        });
    }

    // ─── runAnalysis guards ─────────────────────────────────────────────

    @Test
    @DisplayName("runAnalysis: немає CV -> BusinessRuleException, extractor НЕ викликається")
    void runAnalysis_noCv_throws() {
        ownedApp();
        when(masterCvRepository.existsByUserId(userId)).thenReturn(false);

        assertThatThrownBy(() -> service.runAnalysis(userId, appId))
                .isInstanceOf(BusinessRuleException.class);

        verify(jobSkillExtractor, never()).extractAndSaveSkills(any());
    }

    @Test
    @DisplayName("runAnalysis: викликає extractor + computeAnalysis")
    void runAnalysis_callsExtractorAndComputes() {
        Application app = ownedApp();
        when(masterCvRepository.existsByUserId(userId)).thenReturn(true);
        when(applicationSkillRepository.findByApplicationId(appId)).thenReturn(List.of());

        GapAnalysisResponse response = service.runAnalysis(userId, appId);

        verify(jobSkillExtractor).extractAndSaveSkills(app);
        assertThat(response).isNotNull();
        // Жодного required -> vacuously perfect.
        assertThat(response.getScore()).isEqualTo(1.0);
    }

    // ─── scoring ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAnalysis: score рахується ЛИШЕ по required; nice-to-have не впливає; = matched/total")
    void getAnalysis_scoreOnlyByRequired() {
        ownedApp();
        when(masterCvRepository.existsByUserId(userId)).thenReturn(true);

        ApplicationSkill reqMatched = jobSkill("java", true);     // matched
        ApplicationSkill reqMissing = jobSkill("spring", true);   // missing (sim < 0.75)
        ApplicationSkill niceMatched = jobSkill("docker", false); // matched, але nice-to-have
        when(applicationSkillRepository.findByApplicationId(appId))
                .thenReturn(List.of(reqMatched, reqMissing, niceMatched));

        stubMatchQuery(java.util.Map.of(
                reqMatched.getSkill().getId(), 0.10,   // similarity 0.90 -> match
                reqMissing.getSkill().getId(), 0.40,   // similarity 0.60 -> miss
                niceMatched.getSkill().getId(), 0.05)); // similarity 0.95 -> match

        GapAnalysisResponse response = service.getAnalysis(userId, appId);

        // totalRequired = 2 (java, spring); matchedRequired = 1 (java). nice-to-have docker не рахується.
        assertThat(response.getScore()).isEqualTo(0.5);
        assertThat(response.getTotalRequiredSkills()).isEqualTo(2);
        assertThat(response.getMatchedRequiredSkills()).isEqualTo(1);
        assertThat(response.getMissedRequiredSkills()).isEqualTo(1);
        assertThat(response.getMatchedSkills()).hasSize(2);  // java(req) + docker(nice)
        assertThat(response.getMissingSkills()).hasSize(1);  // spring
        // extractor НЕ викликається на GET.
        verify(jobSkillExtractor, never()).extractAndSaveSkills(any());
    }

    @Test
    @DisplayName("getAnalysis: totalRequired == 0 -> score 1.0")
    void getAnalysis_noRequired_scoreOne() {
        ownedApp();
        when(masterCvRepository.existsByUserId(userId)).thenReturn(true);

        ApplicationSkill nice = jobSkill("docker", false);
        when(applicationSkillRepository.findByApplicationId(appId)).thenReturn(List.of(nice));
        stubMatchQuery(java.util.Map.of(nice.getSkill().getId(), 0.05));

        GapAnalysisResponse response = service.getAnalysis(userId, appId);

        assertThat(response.getScore()).isEqualTo(1.0);
        assertThat(response.getTotalRequiredSkills()).isEqualTo(0);
    }

    @Test
    @DisplayName("matching: similarity >= 0.75 -> matched; < 0.75 -> missing")
    void matching_thresholdBoundary() {
        ownedApp();
        when(masterCvRepository.existsByUserId(userId)).thenReturn(true);

        ApplicationSkill atThreshold = jobSkill("java", true);   // distance 0.25 -> sim 0.75 -> matched
        ApplicationSkill belowThreshold = jobSkill("go", true);  // distance 0.26 -> sim 0.74 -> missing
        when(applicationSkillRepository.findByApplicationId(appId))
                .thenReturn(List.of(atThreshold, belowThreshold));
        stubMatchQuery(java.util.Map.of(
                atThreshold.getSkill().getId(), 0.25,
                belowThreshold.getSkill().getId(), 0.26));

        GapAnalysisResponse response = service.getAnalysis(userId, appId);

        assertThat(response.getMatchedSkills()).hasSize(1);
        assertThat(response.getMatchedSkills().get(0).getRequiredSkillName()).isEqualTo("java");
        assertThat(response.getMissingSkills()).hasSize(1);
        assertThat(response.getMissingSkills().get(0).getRequiredSkillName()).isEqualTo("go");
    }
}
