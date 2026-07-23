package com.jobtracker.backendJobTracker.interview.notes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.interview.InterviewPrep;
import com.jobtracker.backendJobTracker.interview.InterviewPrepPrompt;
import com.jobtracker.backendJobTracker.interview.notes.dto.CreateNoteRequest;
import com.jobtracker.backendJobTracker.interview.notes.dto.InterviewNoteResponse;
import com.jobtracker.backendJobTracker.interview.notes.dto.PostInterviewReflectionRequest;
import com.jobtracker.backendJobTracker.interview.notes.dto.UpdateNoteRequest;
import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;
import com.jobtracker.backendJobTracker.interview.repos.InterviewPrepRepository;

/**
 * Unit-тести {@link InterviewNoteService} (7B).
 * <p>
 * Мокаємо всіх колабораторів. Async embedding: поза Spring-транзакцією
 * {@code TransactionSynchronizationManager.isSynchronizationActive()} == false,
 * тому {@code scheduleEmbeddingGeneration} викликає generator напряму — це й
 * перевіряємо через {@code verify(embeddingGenerator).generate(...)}.
 */
@ExtendWith(MockitoExtension.class)
class InterviewNoteServiceTest {

    @Mock private InterviewNoteRepository noteRepository;
    @Mock private InterviewPrepRepository prepRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private InterviewNoteEmbeddingGenerator embeddingGenerator;
    @Mock private NoteMapper mapper;

    @InjectMocks private InterviewNoteService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID appId = UUID.randomUUID();
    private final UUID prepId = UUID.randomUUID();
    private final UUID noteId = UUID.randomUUID();

    /** Мокає tenant-safe шлях: заявка юзера існує + prep існує. */
    private InterviewPrep stubOwnedPrep() {
        Application app = new Application();
        app.setId(appId);
        InterviewPrep prep = new InterviewPrep();
        prep.setId(prepId);
        prep.setApplication(app);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(prepRepository.findByApplicationId(appId)).thenReturn(Optional.of(prep));
        return prep;
    }

    private void stubSaveAssignsId() {
        when(noteRepository.save(any(InterviewNote.class))).thenAnswer(inv -> {
            InterviewNote n = inv.getArgument(0);
            n.setId(noteId);
            return n;
        });
        when(mapper.toInterviewNoteResponse(any())).thenReturn(new InterviewNoteResponse());
    }

    private CreateNoteRequest createReq(String content, NoteType type) {
        CreateNoteRequest r = new CreateNoteRequest();
        r.setContent(content);
        r.setNoteType(type);
        return r;
    }

    // ─── createNote ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createNote: зберігає з content/noteType/promptVersion і планує embedding")
    void createNote_happyPath() {
        InterviewPrep prep = stubOwnedPrep();
        stubSaveAssignsId();

        service.createNote(userId, appId, createReq("Review @Transactional", NoteType.PREP_NOTE));

        ArgumentCaptor<InterviewNote> captor = ArgumentCaptor.forClass(InterviewNote.class);
        verify(noteRepository).save(captor.capture());
        InterviewNote saved = captor.getValue();
        assertThat(saved.getContent()).isEqualTo("Review @Transactional");
        assertThat(saved.getNoteType()).isEqualTo(NoteType.PREP_NOTE);
        assertThat(saved.getInterviewPrep()).isSameAs(prep);
        assertThat(saved.getPromptVersion()).isEqualTo(InterviewPrepPrompt.VERSION);
        verify(embeddingGenerator).generate(noteId);
    }

    @Test
    @DisplayName("createNote: заявка не знайдена -> ResourceNotFoundException, нічого не зберігається")
    void createNote_appNotFound() {
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createNote(userId, appId, createReq("x", NoteType.PREP_NOTE)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(noteRepository, embeddingGenerator);
    }

    @Test
    @DisplayName("createNote: prep не знайдено (INTERVIEW не виставлено) -> ResourceNotFoundException")
    void createNote_prepNotFound() {
        Application app = new Application();
        app.setId(appId);
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.of(app));
        when(prepRepository.findByApplicationId(appId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createNote(userId, appId, createReq("x", NoteType.PREP_NOTE)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(noteRepository, embeddingGenerator);
    }

    @Test
    @DisplayName("createNote: ownership заявки перевіряється ПЕРЕД prep (tenant safety order)")
    void createNote_checksApplicationBeforePrep() {
        stubOwnedPrep();
        stubSaveAssignsId();

        service.createNote(userId, appId, createReq("x", NoteType.PREP_NOTE));

        InOrder order = inOrder(applicationRepository, prepRepository);
        order.verify(applicationRepository).findByIdAndUserId(appId, userId);
        order.verify(prepRepository).findByApplicationId(appId);
    }

    // ─── updateNote ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateNote: оновлює content, noteType НЕ змінюється, re-embed запускається")
    void updateNote_happyPath() {
        InterviewNote existing = new InterviewNote();
        existing.setId(noteId);
        existing.setContent("old");
        existing.setNoteType(NoteType.PREP_NOTE);
        when(noteRepository.findByIdAndInterviewPrep_Application_User_Id(noteId, userId))
                .thenReturn(Optional.of(existing));
        when(noteRepository.save(any(InterviewNote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toInterviewNoteResponse(any())).thenReturn(new InterviewNoteResponse());

        UpdateNoteRequest req = new UpdateNoteRequest();
        req.setContent("new content");
        service.updateNote(userId, noteId, req);

        assertThat(existing.getContent()).isEqualTo("new content");
        assertThat(existing.getNoteType()).isEqualTo(NoteType.PREP_NOTE); // незмінний
        verify(embeddingGenerator).generate(noteId);
    }

    @Test
    @DisplayName("updateNote: нотатка не знайдена (tenant graph) -> 404, embedding не тригериться")
    void updateNote_notFound() {
        when(noteRepository.findByIdAndInterviewPrep_Application_User_Id(noteId, userId))
                .thenReturn(Optional.empty());

        UpdateNoteRequest req = new UpdateNoteRequest();
        req.setContent("x");
        assertThatThrownBy(() -> service.updateNote(userId, noteId, req))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(embeddingGenerator, never()).generate(any());
    }

    // ─── list / delete ──────────────────────────────────────────────────

    @Test
    @DisplayName("list: tenant-safe fetch prep, віддає нотатки за спаданням createdAt")
    void list_happyPath() {
        stubOwnedPrep();
        InterviewNote n1 = new InterviewNote();
        n1.setId(UUID.randomUUID());
        when(noteRepository.findByInterviewPrepIdOrderByCreatedAtDesc(prepId)).thenReturn(List.of(n1));
        when(mapper.toInterviewNoteResponse(n1)).thenReturn(new InterviewNoteResponse());

        List<InterviewNoteResponse> result = service.list(userId, appId);

        assertThat(result).hasSize(1);
        verify(noteRepository).findByInterviewPrepIdOrderByCreatedAtDesc(prepId);
    }

    @Test
    @DisplayName("list: чужа заявка -> 404")
    void list_notOwned() {
        when(applicationRepository.findByIdAndUserId(appId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.list(userId, appId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete: tenant-safe fetch, repository.delete викликається")
    void delete_happyPath() {
        InterviewNote note = new InterviewNote();
        note.setId(noteId);
        when(noteRepository.findByIdAndInterviewPrep_Application_User_Id(noteId, userId))
                .thenReturn(Optional.of(note));

        service.delete(userId, noteId);

        verify(noteRepository).delete(note);
    }

    @Test
    @DisplayName("delete: не знайдено -> 404, delete не викликається")
    void delete_notFound() {
        when(noteRepository.findByIdAndInterviewPrep_Application_User_Id(noteId, userId))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(userId, noteId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(noteRepository, never()).delete(any());
    }

    // ─── addPostInterviewReflection ─────────────────────────────────────

    @Test
    @DisplayName("reflection: noteType=POST_INTERVIEW, questionsAsked додаються у content")
    void reflection_withQuestions() {
        stubOwnedPrep();
        stubSaveAssignsId();

        PostInterviewReflectionRequest req = new PostInterviewReflectionRequest();
        req.setContent("Went ok, failed N+1");
        req.setQuestionsAsked(List.of("Tell me about yourself", "Explain @Transactional"));

        service.addPostInterviewReflection(userId, appId, req);

        ArgumentCaptor<InterviewNote> captor = ArgumentCaptor.forClass(InterviewNote.class);
        verify(noteRepository).save(captor.capture());
        InterviewNote saved = captor.getValue();
        assertThat(saved.getNoteType()).isEqualTo(NoteType.POST_INTERVIEW);
        assertThat(saved.getContent())
                .contains("Went ok, failed N+1")
                .contains("Questions asked during interview:")
                .contains("- Tell me about yourself")
                .contains("- Explain @Transactional");
    }

    @Test
    @DisplayName("reflection: порожній questionsAsked -> тільки content, без секції питань")
    void reflection_emptyQuestions() {
        stubOwnedPrep();
        stubSaveAssignsId();

        PostInterviewReflectionRequest req = new PostInterviewReflectionRequest();
        req.setContent("Just my thoughts");
        req.setQuestionsAsked(List.of());

        service.addPostInterviewReflection(userId, appId, req);

        ArgumentCaptor<InterviewNote> captor = ArgumentCaptor.forClass(InterviewNote.class);
        verify(noteRepository).save(captor.capture());
        assertThat(captor.getValue().getContent())
                .isEqualTo("Just my thoughts")
                .doesNotContain("Questions asked");
    }
}
