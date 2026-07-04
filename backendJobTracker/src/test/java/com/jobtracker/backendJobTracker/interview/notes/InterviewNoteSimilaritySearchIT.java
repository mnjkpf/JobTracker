package com.jobtracker.backendJobTracker.interview.notes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.jobtracker.backendJobTracker.AbstractIntegrationTest;
import com.jobtracker.backendJobTracker.application.ApplicationService;
import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import com.jobtracker.backendJobTracker.application.dto.CreateApplicationRequest;
import com.jobtracker.backendJobTracker.application.dto.UpdateStatusRequest;
import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.interview.InterviewPrep;
import com.jobtracker.backendJobTracker.interview.notes.dto.SimilarInterviewNote;
import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;
import com.jobtracker.backendJobTracker.interview.repos.InterviewPrepRepository;
import com.jobtracker.backendJobTracker.user.User;

/**
 * Integration-тести {@link InterviewNoteSimilaritySearch} проти РЕАЛЬНОГО pgvector
 * (Testcontainers). Валідують: HNSW/cosine пошук, clamp similarity у [0,1],
 * повернення application_id, tenant-фільтр, LIMIT, фільтр null-embedding.
 * <p>
 * {@link EmbeddingModel} замокано — query embedding детермінований (уникаємо OpenAI).
 * Embedding нотаток вставляємо напряму нативним SQL (async flow не комітиться у @Transactional IT).
 */
class InterviewNoteSimilaritySearchIT extends AbstractIntegrationTest {

    @Autowired private InterviewNoteSimilaritySearch search;
    @Autowired private ApplicationService applicationService;
    @Autowired private InterviewNoteRepository noteRepository;
    @Autowired private InterviewPrepRepository prepRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private EmbeddingModel embeddingModel;

    /** Query-вектор: перша вісь. */
    private static final float[] QUERY = vec(1f, 0f, 0f);

    private static float[] vec(float... dims) {
        float[] v = new float[1536];
        System.arraycopy(dims, 0, v, 0, dims.length);
        return v;
    }

    private static String literal(float... dims) {
        float[] v = vec(dims);
        StringBuilder sb = new StringBuilder(v.length * 4);
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }

    /** Унікальний query-текст на виклик — щоб Redis-кеш embedding не заважав моку. */
    private String uniqueQuery() {
        return "query-" + UUID.randomUUID();
    }

    private InterviewPrep interviewPrepFor(UUID userId, String urlSuffix) {
        CreateApplicationRequest r = new CreateApplicationRequest();
        r.setName("Java Backend Developer");
        r.setCompanyName("Acme");
        r.setUrl("https://example.com/job/" + urlSuffix);
        r.setDescription("Java Spring backend role");
        r.setContractType(ContractType.B2B);
        r.setSeniority(Seniority.MID);
        r.setWorkMode(WorkMode.REMOTE);
        ApplicationResponse app = applicationService.create(userId, r);

        UUID appId = app.getId();
        for (ApplicationStatus s : List.of(ApplicationStatus.APPLIED,
                ApplicationStatus.SCREENING, ApplicationStatus.INTERVIEW)) {
            UpdateStatusRequest sr = new UpdateStatusRequest();
            sr.setStatus(s);
            applicationService.updateStatus(userId, appId, sr);
        }
        return prepRepository.findByApplicationId(appId).orElseThrow();
    }

    /** Створює нотатку і задає embedding нативним UPDATE. */
    private UUID insertNote(InterviewPrep prep, String content, float[] embedding) {
        InterviewNote note = new InterviewNote();
        note.setInterviewPrep(prep);
        note.setNoteType(NoteType.PREP_NOTE);
        note.setContent(content);
        note = noteRepository.saveAndFlush(note);
        String lit = "[" + toCsv(embedding) + "]";
        jdbcTemplate.update(
                "UPDATE interview_notes SET embedding = CAST(? AS vector) WHERE id = ?",
                lit, note.getId());
        return note.getId();
    }

    private static String toCsv(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 4);
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.toString();
    }

    private void stubQueryEmbedding() {
        when(embeddingModel.embed(anyString())).thenReturn(QUERY);
    }

    @Test
    @DisplayName("найближча за cosine нотатка повертається першою, порядок за similarity DESC")
    void nearestFirst() {
        stubQueryEmbedding();
        User user = persistUser("sim-near@example.com", "Passw0rd!");
        InterviewPrep prep = interviewPrepFor(user.getId(), "near");

        insertNote(prep, "A-identical", vec(1f, 0f, 0f));   // cos=1  dist=0
        insertNote(prep, "C-45deg", vec(1f, 1f, 0f));       // cos~0.707
        insertNote(prep, "B-orthogonal", vec(0f, 1f, 0f));  // cos=0

        List<SimilarInterviewNote> result = search.findSimilar(user.getId(), uniqueQuery(), 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("A-identical");
        assertThat(result).extracting(SimilarInterviewNote::getSimilarity)
                .isSortedAccordingTo((a, b) -> Double.compare(b, a)); // DESC
    }

    @Test
    @DisplayName("similarity клампиться у [0,1]; протилежний вектор -> 0 (не -1)")
    void similarityClamped() {
        stubQueryEmbedding();
        User user = persistUser("sim-clamp@example.com", "Passw0rd!");
        InterviewPrep prep = interviewPrepFor(user.getId(), "clamp");

        insertNote(prep, "identical", vec(1f, 0f, 0f));     // sim 1
        insertNote(prep, "opposite", vec(-1f, 0f, 0f));     // cos=-1 -> dist=2 -> raw -1 -> clamp 0

        List<SimilarInterviewNote> result = search.findSimilar(user.getId(), uniqueQuery(), 10);

        assertThat(result).extracting(SimilarInterviewNote::getSimilarity)
                .allMatch(s -> s >= 0.0 && s <= 1.0);
        SimilarInterviewNote opposite = result.stream()
                .filter(n -> n.getContent().equals("opposite")).findFirst().orElseThrow();
        assertThat(opposite.getSimilarity()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("LIMIT дотримується (12 нотаток, limit=5 -> 5)")
    void limitRespected() {
        stubQueryEmbedding();
        User user = persistUser("sim-limit@example.com", "Passw0rd!");
        InterviewPrep prep = interviewPrepFor(user.getId(), "limit");
        for (int i = 0; i < 12; i++) {
            insertNote(prep, "note-" + i, vec(1f, 0f, 0f));
        }

        assertThat(search.findSimilar(user.getId(), uniqueQuery(), 5)).hasSize(5);
    }

    @Test
    @DisplayName("tenant-фільтр: запит одного юзера не бачить нотаток іншого")
    void tenantIsolation() {
        stubQueryEmbedding();
        User user1 = persistUser("sim-t1@example.com", "Passw0rd!");
        User user2 = persistUser("sim-t2@example.com", "Passw0rd!");
        insertNote(interviewPrepFor(user1.getId(), "t1"), "user1-note", vec(1f, 0f, 0f));
        insertNote(interviewPrepFor(user2.getId(), "t2"), "user2-note", vec(1f, 0f, 0f));

        List<SimilarInterviewNote> asUser1 = search.findSimilar(user1.getId(), uniqueQuery(), 10);

        assertThat(asUser1).hasSize(1);
        assertThat(asUser1.get(0).getContent()).isEqualTo("user1-note");
    }

    @Test
    @DisplayName("порожня БД -> empty list")
    void emptyDatabase() {
        stubQueryEmbedding();
        User user = persistUser("sim-empty@example.com", "Passw0rd!");
        interviewPrepFor(user.getId(), "empty"); // prep без нотаток

        assertThat(search.findSimilar(user.getId(), uniqueQuery(), 10)).isEmpty();
    }

    @Test
    @DisplayName("нотатки з null embedding відфільтровані (WHERE embedding IS NOT NULL)")
    void nullEmbeddingExcluded() {
        stubQueryEmbedding();
        User user = persistUser("sim-null@example.com", "Passw0rd!");
        InterviewPrep prep = interviewPrepFor(user.getId(), "null");

        insertNote(prep, "has-embedding", vec(1f, 0f, 0f));
        // нотатка БЕЗ embedding (не робимо UPDATE)
        InterviewNote noEmb = new InterviewNote();
        noEmb.setInterviewPrep(prep);
        noEmb.setNoteType(NoteType.PREP_NOTE);
        noEmb.setContent("no-embedding");
        noteRepository.saveAndFlush(noEmb);

        List<SimilarInterviewNote> result = search.findSimilar(user.getId(), uniqueQuery(), 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("has-embedding");
    }

    @Test
    @DisplayName("application_id повертається у результаті (Fix 1)")
    void applicationIdReturned() {
        stubQueryEmbedding();
        User user = persistUser("sim-appid@example.com", "Passw0rd!");
        InterviewPrep prep = interviewPrepFor(user.getId(), "appid");
        insertNote(prep, "note", vec(1f, 0f, 0f));

        List<SimilarInterviewNote> result = search.findSimilar(user.getId(), uniqueQuery(), 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApplicationId())
                .isNotNull()
                .isEqualTo(prep.getApplication().getId());
    }
}
