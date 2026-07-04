package com.jobtracker.backendJobTracker.interview.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.gap.ApplicationSkill;
import com.jobtracker.backendJobTracker.application.gap.ApplicationSkillRepository;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.interview.notes.InterviewNoteSimilaritySearch;
import com.jobtracker.backendJobTracker.interview.notes.dto.SimilarInterviewNote;

/**
 * Unit-тести {@link InterviewRagService} (7C — RAG retrieval + фільтрація).
 * <p>
 * {@code similarityThreshold} та {@code maxResults} — @Value поля; у unit-тесті
 * без Spring-контексту виставляємо їх через {@link ReflectionTestUtils}.
 */
@ExtendWith(MockitoExtension.class)
class InterviewRagServiceTest {

    @Mock private InterviewNoteSimilaritySearch noteSimilaritySearch;
    @Mock private ApplicationSkillRepository applicationSkillRepository;

    @InjectMocks private InterviewRagService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID currentAppId = UUID.randomUUID();
    private final UUID otherAppId = UUID.randomUUID();

    @BeforeEach
    void injectValueFields() {
        ReflectionTestUtils.setField(service, "similarityThreshold", 0.55);
        ReflectionTestUtils.setField(service, "maxResults", 10);
    }

    private Application app(String name, String description) {
        Application a = new Application();
        a.setId(currentAppId);
        a.setName(name);
        a.setDescription(description);
        a.setSeniority(Seniority.JUNIOR);
        return a;
    }

    private ApplicationSkill skill(String name) {
        Skill s = new Skill();
        s.setId(UUID.randomUUID());
        s.setName(name);
        ApplicationSkill as = new ApplicationSkill();
        as.setSkill(s);
        return as;
    }

    private SimilarInterviewNote simNote(UUID appId, double similarity) {
        SimilarInterviewNote n = new SimilarInterviewNote();
        n.setId(UUID.randomUUID());
        n.setApplicationId(appId);
        n.setSimilarity(similarity);
        return n;
    }

    // ─── defensive guards ───────────────────────────────────────────────

    @Test
    @DisplayName("null userId -> empty, пошук не запускається")
    void nullUserId_empty() {
        assertThat(service.findRelevantPastNotes(null, app("Java", "desc"))).isEmpty();
        verifyNoInteractions(noteSimilaritySearch);
    }

    @Test
    @DisplayName("null app -> empty")
    void nullApp_empty() {
        assertThat(service.findRelevantPastNotes(userId, null)).isEmpty();
        verifyNoInteractions(noteSimilaritySearch);
    }

    // ─── query text building ────────────────────────────────────────────

    @Test
    @DisplayName("query text містить position, seniority, description")
    void queryText_containsCoreFields() {
        Application app = app("Java Developer", "Spring Boot backend role");
        when(noteSimilaritySearch.findSimilar(eq(userId), anyString(), anyInt())).thenReturn(List.of());

        service.findRelevantPastNotes(userId, app);

        ArgumentCaptor<String> q = ArgumentCaptor.forClass(String.class);
        verify(noteSimilaritySearch).findSimilar(eq(userId), q.capture(), anyInt());
        assertThat(q.getValue())
                .contains("Position: Java Developer")
                .contains("Seniority: JUNIOR")
                .contains("Description: Spring Boot backend role");
    }

    @Test
    @DisplayName("query text містить Technologies коли ApplicationSkills існують")
    void queryText_withTechnologies() {
        Application app = app("Java Developer", "desc");
        when(applicationSkillRepository.findByApplicationId(currentAppId))
                .thenReturn(List.of(skill("java"), skill("spring")));
        when(noteSimilaritySearch.findSimilar(eq(userId), anyString(), anyInt())).thenReturn(List.of());

        service.findRelevantPastNotes(userId, app);

        ArgumentCaptor<String> q = ArgumentCaptor.forClass(String.class);
        verify(noteSimilaritySearch).findSimilar(eq(userId), q.capture(), anyInt());
        assertThat(q.getValue()).contains("Technologies: java, spring");
    }

    @Test
    @DisplayName("query text БЕЗ Technologies коли ApplicationSkills порожні (fallback на description)")
    void queryText_noTechnologiesFallback() {
        Application app = app("Java Developer", "rich description carries semantics");
        when(noteSimilaritySearch.findSimilar(eq(userId), anyString(), anyInt())).thenReturn(List.of());

        service.findRelevantPastNotes(userId, app);

        ArgumentCaptor<String> q = ArgumentCaptor.forClass(String.class);
        verify(noteSimilaritySearch).findSimilar(eq(userId), q.capture(), anyInt());
        assertThat(q.getValue())
                .doesNotContain("Technologies:")
                .contains("Description: rich description carries semantics");
    }

    @Test
    @DisplayName("description обрізається до MAX_DESCRIPTION_CHARS (800)")
    void queryText_descriptionTruncated() {
        String longDesc = "D".repeat(800) + "E".repeat(100); // хвіст 'E' має відрізатись
        Application app = app("Java", longDesc);
        when(noteSimilaritySearch.findSimilar(eq(userId), anyString(), anyInt())).thenReturn(List.of());

        service.findRelevantPastNotes(userId, app);

        ArgumentCaptor<String> q = ArgumentCaptor.forClass(String.class);
        verify(noteSimilaritySearch).findSimilar(eq(userId), q.capture(), anyInt());
        assertThat(q.getValue()).contains("D".repeat(800)).doesNotContain("E");
    }

    @Test
    @DisplayName("maxResults з @Value передається у findSimilar")
    void maxResults_passedToSearch() {
        ReflectionTestUtils.setField(service, "maxResults", 7);
        Application app = app("Java", "desc");
        when(noteSimilaritySearch.findSimilar(eq(userId), anyString(), eq(7))).thenReturn(List.of());

        service.findRelevantPastNotes(userId, app);

        verify(noteSimilaritySearch).findSimilar(eq(userId), anyString(), eq(7));
    }

    // ─── filtering ──────────────────────────────────────────────────────

    @Test
    @DisplayName("порожній результат пошуку -> empty")
    void emptySearch_empty() {
        Application app = app("Java", "desc");
        when(noteSimilaritySearch.findSimilar(eq(userId), anyString(), anyInt())).thenReturn(List.of());

        assertThat(service.findRelevantPastNotes(userId, app)).isEmpty();
    }

    @Test
    @DisplayName("threshold=0.55: з [0.9,0.8,0.6,0.5,0.3] залишаються 3 (>= 0.55)")
    void thresholdFilter() {
        Application app = app("Java", "desc");
        when(noteSimilaritySearch.findSimilar(eq(userId), anyString(), anyInt())).thenReturn(List.of(
                simNote(otherAppId, 0.9),
                simNote(otherAppId, 0.8),
                simNote(otherAppId, 0.6),
                simNote(otherAppId, 0.5),
                simNote(otherAppId, 0.3)));

        List<SimilarInterviewNote> result = service.findRelevantPastNotes(userId, app);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(n -> n.getSimilarity() >= 0.55);
    }

    @Test
    @DisplayName("нотатки поточної заявки відфільтровуються (cross-interview only)")
    void currentApplicationFiltered() {
        ReflectionTestUtils.setField(service, "similarityThreshold", 0.0); // поріг не заважає
        Application app = app("Java", "desc");
        when(noteSimilaritySearch.findSimilar(eq(userId), anyString(), anyInt())).thenReturn(List.of(
                simNote(currentAppId, 0.95), // своя заявка — має відпасти
                simNote(otherAppId, 0.60)));  // інша заявка — лишається

        List<SimilarInterviewNote> result = service.findRelevantPastNotes(userId, app);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApplicationId()).isEqualTo(otherAppId);
    }
}
