package com.jobtracker.backendJobTracker.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.interview.dto.InterviewPrepResponse;
import com.jobtracker.backendJobTracker.interview.dto.InterviewQuestionResponse;
import com.jobtracker.backendJobTracker.interview.dto.parse.ParsedPrepGuide;
import com.jobtracker.backendJobTracker.interview.dto.parse.ParsedQuestion;
import com.jobtracker.backendJobTracker.interview.enums.InterviewPrepStatus;
import com.jobtracker.backendJobTracker.interview.enums.QuestionCategory;
import com.jobtracker.backendJobTracker.interview.repos.InterviewPrepRepository;
import com.jobtracker.backendJobTracker.interview.repos.InterviewQuestionRepository;

/**
 * Unit-тести {@link InterviewPrepService} (7A — orchestration prep + generate).
 */
@ExtendWith(MockitoExtension.class)
class InterviewPrepServiceTest {

    @Mock private InterviewPrepRepository interviewPrepRepository;
    @Mock private InterviewQuestionRepository interviewQuestionRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private MasterCvRepository masterCvRepository;
    @Mock private InterviewPrepMapper mapper;
    @Mock private InterviewPrepGenerator generator;

    @InjectMocks private InterviewPrepService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID appId = UUID.randomUUID();
    private final UUID prepId = UUID.randomUUID();

    private Application appWithDescription() {
        Application a = new Application();
        a.setId(appId);
        a.setDescription("Java Spring backend role");
        return a;
    }

    private ParsedQuestion pq(String question, String answer) {
        return ParsedQuestion.builder().question(question).suggestedAnswer(answer).build();
    }

    private InterviewQuestion question(QuestionCategory category) {
        InterviewQuestion q = new InterviewQuestion();
        q.setCategory(category);
        q.setQuestion("q-" + category);
        return q;
    }

    // ─── createIfNotExists ──────────────────────────────────────────────

    @Test
    @DisplayName("createIfNotExists: prep вже існує -> без save (idempotent)")
    void createIfNotExists_alreadyExists() {
        when(interviewPrepRepository.existsByApplicationId(appId)).thenReturn(true);

        service.createIfNotExists(appId);

        verify(interviewPrepRepository, never()).save(any());
        verify(applicationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createIfNotExists: prep нема -> зберігає DRAFT з application")
    void createIfNotExists_creates() {
        Application app = appWithDescription();
        when(interviewPrepRepository.existsByApplicationId(appId)).thenReturn(false);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        service.createIfNotExists(appId);

        ArgumentCaptor<InterviewPrep> captor = ArgumentCaptor.forClass(InterviewPrep.class);
        verify(interviewPrepRepository).save(captor.capture());
        InterviewPrep saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(InterviewPrepStatus.DRAFT);
        assertThat(saved.getApplication()).isSameAs(app);
    }

    @Test
    @DisplayName("createIfNotExists: заявки нема -> ResourceNotFoundException")
    void createIfNotExists_appNotFound() {
        when(interviewPrepRepository.existsByApplicationId(appId)).thenReturn(false);
        when(applicationRepository.findById(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createIfNotExists(appId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(interviewPrepRepository, never()).save(any());
    }

    // ─── generate: guards ───────────────────────────────────────────────

    @Test
    @DisplayName("generate: чужа/відсутня заявка -> ResourceNotFoundException")
    void generate_appNotOwned() {
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(userId, appId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("generate: порожній опис -> BusinessRuleException")
    void generate_noDescription() {
        Application app = new Application();
        app.setId(appId);
        app.setDescription("   ");
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.generate(userId, appId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("generate: немає CV -> BusinessRuleException (upload CV first)")
    void generate_noCv() {
        when(applicationRepository.findByIdAndUserId(appId, userId))
                .thenReturn(Optional.of(appWithDescription()));
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(userId, appId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CV");
    }

    // ─── generate: happy path ───────────────────────────────────────────

    private InterviewPrep stubGenerateReady(ParsedPrepGuide guide) {
        Application app = appWithDescription();
        InterviewPrep prep = new InterviewPrep();
        prep.setId(prepId);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(masterCvRepository.findByUserId(userId)).thenReturn(Optional.of(new MasterCv()));
        when(interviewPrepRepository.existsByApplicationId(appId)).thenReturn(true);
        when(interviewPrepRepository.findByApplicationId(appId)).thenReturn(Optional.of(prep));
        when(generator.generate(eq(userId), eq(app), any(MasterCv.class))).thenReturn(guide);
        when(mapper.toInterviewPrepResponse(prep)).thenReturn(new InterviewPrepResponse());
        when(interviewQuestionRepository.findByInterviewPrepIdOrderByDisplayOrderAsc(prepId))
                .thenReturn(List.of());
        return prep;
    }

    @Test
    @DisplayName("generate: delete старих ПЕРЕД save, статус GENERATED, promptVersion=v2")
    void generate_happyPath() {
        ParsedPrepGuide guide = ParsedPrepGuide.builder()
                .technical(List.of(pq("T1", "a1"), pq("T2", "a2")))
                .behavioral(List.of(pq("B1", null)))
                .questionsToAsk(List.of(pq("A1", null)))
                .build();
        InterviewPrep prep = stubGenerateReady(guide);

        service.generate(userId, appId);

        InOrder order = inOrder(interviewQuestionRepository);
        order.verify(interviewQuestionRepository).deleteByInterviewPrepId(prepId);
        order.verify(interviewQuestionRepository).flush();

        assertThat(prep.getStatus()).isEqualTo(InterviewPrepStatus.GENERATED);
        assertThat(prep.getPromptVersion()).isEqualTo(InterviewPrepPrompt.VERSION);
        verify(interviewPrepRepository).save(prep);
    }

    @Test
    @DisplayName("generate: displayOrder інкрементиться глобально через категорії (1,2 -> 3 -> 4)")
    void generate_displayOrderGlobal() {
        ParsedPrepGuide guide = ParsedPrepGuide.builder()
                .technical(List.of(pq("T1", "a1"), pq("T2", "a2")))
                .behavioral(List.of(pq("B1", null)))
                .questionsToAsk(List.of(pq("A1", null)))
                .build();
        stubGenerateReady(guide);

        service.generate(userId, appId);

        ArgumentCaptor<InterviewQuestion> captor = ArgumentCaptor.forClass(InterviewQuestion.class);
        verify(interviewQuestionRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(InterviewQuestion::getDisplayOrder)
                .containsExactly(1, 2, 3, 4);
        assertThat(captor.getAllValues())
                .extracting(InterviewQuestion::getCategory)
                .containsExactly(QuestionCategory.TECHNICAL, QuestionCategory.TECHNICAL,
                        QuestionCategory.BEHAVIORAL, QuestionCategory.QUESTION_TO_ASK);
    }

    @Test
    @DisplayName("generate: питання з null/blank text пропускаються")
    void generate_skipsBlankQuestions() {
        ParsedPrepGuide guide = ParsedPrepGuide.builder()
                .technical(java.util.Arrays.asList(pq("T1", "a1"), pq("   ", "blank"), pq("T3", "a3")))
                .behavioral(List.of())
                .questionsToAsk(List.of())
                .build();
        stubGenerateReady(guide);

        service.generate(userId, appId);

        ArgumentCaptor<InterviewQuestion> captor = ArgumentCaptor.forClass(InterviewQuestion.class);
        verify(interviewQuestionRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(InterviewQuestion::getQuestion)
                .containsExactly("T1", "T3");
    }

    // ─── getByApplication ───────────────────────────────────────────────

    @Test
    @DisplayName("getByApplication: чужа заявка -> 404")
    void getByApplication_notOwned() {
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByApplication(userId, appId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getByApplication: prep не існує -> 404")
    void getByApplication_prepMissing() {
        when(applicationRepository.findByIdAndUserId(appId, userId))
                .thenReturn(Optional.of(appWithDescription()));
        when(interviewPrepRepository.findByApplicationId(appId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByApplication(userId, appId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getByApplication: питання групуються по 3 категоріях")
    void getByApplication_groupsByCategory() {
        InterviewPrep prep = new InterviewPrep();
        prep.setId(prepId);
        when(applicationRepository.findByIdAndUserId(appId, userId))
                .thenReturn(Optional.of(appWithDescription()));
        when(interviewPrepRepository.findByApplicationId(appId)).thenReturn(Optional.of(prep));
        when(mapper.toInterviewPrepResponse(prep)).thenReturn(new InterviewPrepResponse());
        when(interviewQuestionRepository.findByInterviewPrepIdOrderByDisplayOrderAsc(prepId))
                .thenReturn(List.of(
                        question(QuestionCategory.TECHNICAL),
                        question(QuestionCategory.TECHNICAL),
                        question(QuestionCategory.BEHAVIORAL),
                        question(QuestionCategory.QUESTION_TO_ASK)));
        when(mapper.toInterviewQuestionResponse(any())).thenReturn(new InterviewQuestionResponse());

        InterviewPrepResponse resp = service.getByApplication(userId, appId);

        assertThat(resp.getTechnicalQuestions()).hasSize(2);
        assertThat(resp.getBehavioralQuestions()).hasSize(1);
        assertThat(resp.getQuestionsToAsk()).hasSize(1);
    }

    // ─── delete ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: tenant-safe fetch, repository.delete викликається")
    void delete_happyPath() {
        InterviewPrep prep = new InterviewPrep();
        prep.setId(prepId);
        when(applicationRepository.findByIdAndUserId(appId, userId))
                .thenReturn(Optional.of(appWithDescription()));
        when(interviewPrepRepository.findByApplicationId(appId)).thenReturn(Optional.of(prep));

        service.delete(userId, appId);

        verify(interviewPrepRepository).delete(prep);
    }

    @Test
    @DisplayName("delete: чужа заявка -> 404")
    void delete_notOwned() {
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(userId, appId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
