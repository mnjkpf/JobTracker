package com.jobtracker.backendJobTracker.cv.tailored.ats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredCv;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredExperience;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredProject;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredSkill;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredCvRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredEducationRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredExperienceRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredProjectRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredSkillRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;

/**
 * Unit-тести для {@link AtsScoringService} з Mockito.
 * <p>
 * Перевіряємо score = matched/total×100 (округлено), contains-matching,
 * склад cvText (summary+descriptions+skill names+project tech),
 * total==0 -> 0, guard опису, tenant safety та case-insensitive matching.
 */
@ExtendWith(MockitoExtension.class)
class AtsScoringServiceTest {

    @Mock private AtsKeywordExtractor atsKeywordExtractor;
    @Mock private TailoredCvRepository tailoredCvRepository;
    @Mock private TailoredExperienceRepository experienceRepository;
    @Mock private TailoredEducationRepository educationRepository;
    @Mock private TailoredSkillRepository skillRepository;
    @Mock private TailoredProjectRepository projectRepository;

    @InjectMocks private AtsScoringService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID tcvId = UUID.randomUUID();

    private TailoredCv cvWith(String description, String summary) {
        Application app = new Application();
        app.setDescription(description);
        TailoredCv cv = new TailoredCv();
        cv.setId(tcvId);
        cv.setApplication(app);
        cv.setSummary(summary);
        when(tailoredCvRepository.findByIdAndApplication_User_Id(tcvId, userId)).thenReturn(Optional.of(cv));
        return cv;
    }

    private TailoredSkill skill(String name) {
        TailoredSkill s = new TailoredSkill();
        s.setName(name);
        return s;
    }

    // ─── scoring ────────────────────────────────────────────────────────

    @Test
    @DisplayName("score = matched/total × 100 округлено; присутній -> matched, відсутній -> missing")
    void score_roundedRatio() {
        cvWith("job desc", "experienced with java and spring");
        // 2 з 3 matched -> round(66.66) = 67
        when(atsKeywordExtractor.extract("job desc"))
                .thenReturn(List.of("java", "spring", "kubernetes"));

        AtsScoreResponse response = service.score(userId, tcvId);

        assertThat(response.getScore()).isEqualTo(67);
        assertThat(response.getTotalKeywords()).isEqualTo(3);
        assertThat(response.getMatchedCount()).isEqualTo(2);
        assertThat(response.getMatched()).containsExactlyInAnyOrder("java", "spring");
        assertThat(response.getMissing()).containsExactly("kubernetes");
    }

    @Test
    @DisplayName("buildCvText містить summary + experience desc + skill names + project tech")
    void buildCvText_includesAllSources() {
        cvWith("job desc", "leadership summary");

        TailoredExperience exp = new TailoredExperience();
        exp.setDescription("microservices architecture");
        when(experienceRepository.findByTailoredCv_IdOrderByStartDateDesc(tcvId)).thenReturn(List.of(exp));
        when(skillRepository.findByTailoredCv_Id(tcvId)).thenReturn(List.of(skill("java")));
        TailoredProject project = new TailoredProject();
        project.setName("JobTracker");
        project.setTechnologies("docker compose");
        when(projectRepository.findByTailoredCv_IdOrderByStartDateDesc(tcvId)).thenReturn(List.of(project));

        when(atsKeywordExtractor.extract("job desc"))
                .thenReturn(List.of("leadership", "microservices", "java", "docker", "missingkw"));

        AtsScoreResponse response = service.score(userId, tcvId);

        // leadership(summary), microservices(exp desc), java(skill), docker(project tech) -> matched
        assertThat(response.getMatched())
                .containsExactlyInAnyOrder("leadership", "microservices", "java", "docker");
        assertThat(response.getMissing()).containsExactly("missingkw");
    }

    @Test
    @DisplayName("case-insensitive matching: keyword 'java' matchиться у 'Java Spring'")
    void matching_caseInsensitive() {
        cvWith("job desc", null);
        when(skillRepository.findByTailoredCv_Id(tcvId)).thenReturn(List.of(skill("Java Spring")));
        when(atsKeywordExtractor.extract("job desc")).thenReturn(List.of("java"));

        AtsScoreResponse response = service.score(userId, tcvId);

        assertThat(response.getMatched()).containsExactly("java");
        assertThat(response.getScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("total == 0 (нема keywords) -> score 0")
    void noKeywords_scoreZero() {
        cvWith("job desc", "some summary");
        when(atsKeywordExtractor.extract("job desc")).thenReturn(List.of());

        AtsScoreResponse response = service.score(userId, tcvId);

        assertThat(response.getScore()).isEqualTo(0);
        assertThat(response.getTotalKeywords()).isEqualTo(0);
    }

    // ─── guards / tenant safety ─────────────────────────────────────────

    @Test
    @DisplayName("немає опису заявки -> BusinessRuleException, extractor НЕ викликається")
    void noDescription_throws() {
        cvWith("  ", "summary");

        assertThatThrownBy(() -> service.score(userId, tcvId))
                .isInstanceOf(BusinessRuleException.class);

        org.mockito.Mockito.verify(atsKeywordExtractor, org.mockito.Mockito.never()).extract(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("чужий tailored CV -> ResourceNotFoundException")
    void foreignCv_throws() {
        when(tailoredCvRepository.findByIdAndApplication_User_Id(tcvId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.score(userId, tcvId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
