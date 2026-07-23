package com.jobtracker.backendJobTracker.interview.notes;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewNoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterviewNoteService.class);

    private final InterviewNoteRepository noteRepository;
    private final InterviewPrepRepository prepRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewNoteEmbeddingGenerator embeddingGenerator;
    private final NoteMapper mapper;

    

    @Transactional   // ДОДАНО: клас readOnly, цей метод пише
    public InterviewNoteResponse createNote(UUID userId, UUID applicationId, CreateNoteRequest request) {
        
        InterviewPrep prep = fetchOwnedPrep(userId, applicationId);

        InterviewNote note = new InterviewNote();
        note.setInterviewPrep(prep);
        note.setContent(request.getContent());
        note.setNoteType(request.getNoteType());
        // ДОДАНО: prompt version для tracking embedding model version
        note.setPromptVersion(InterviewPrepPrompt.VERSION);
        // ВИДАЛЕНО: setCreatedAt/setUpdatedAt — @PrePersist робить це автоматично

        note = noteRepository.save(note);

        
        scheduleEmbeddingGeneration(note.getId());

        return mapper.toInterviewNoteResponse(note);
    }

   

    @Transactional   // ДОДАНО
    public InterviewNoteResponse updateNote(UUID userId, UUID noteId, UpdateNoteRequest request) {
        // OK — findByIdAndInterviewPrep_Application_User_Id вже tenant-safe (graph navigation)
        InterviewNote note = noteRepository.findByIdAndInterviewPrep_Application_User_Id(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + noteId));

        note.setContent(request.getContent());
        // ВИДАЛЕНО: setUpdatedAt — @PreUpdate робить автоматично

        note = noteRepository.save(note);

        // Re-embed: content змінився → старе embedding більше не релевантне.
        scheduleEmbeddingGeneration(note.getId());

        return mapper.toInterviewNoteResponse(note);
    }


    // ДОДАНО: бракувало повністю
    public List<InterviewNoteResponse> list(UUID userId, UUID applicationId) {
        InterviewPrep prep = fetchOwnedPrep(userId, applicationId);
        return noteRepository.findByInterviewPrepIdOrderByCreatedAtDesc(prep.getId()).stream()
                .map(mapper::toInterviewNoteResponse)
                .toList();
    }

    

    // ДОДАНО: бракувало повністю
    @Transactional
    public void delete(UUID userId, UUID noteId) {
        InterviewNote note = noteRepository.findByIdAndInterviewPrep_Application_User_Id(noteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + noteId));
        noteRepository.delete(note);
    }

    
    @Transactional
    public InterviewNoteResponse addPostInterviewReflection(
            UUID userId, UUID applicationId, PostInterviewReflectionRequest request) {

        StringBuilder content = new StringBuilder(request.getContent());

        // Append optional list of questions for searchability
        if (request.getQuestionsAsked() != null && !request.getQuestionsAsked().isEmpty()) {
            content.append("\n\nQuestions asked during interview:\n");
            for (String q : request.getQuestionsAsked()) {
                content.append("- ").append(q).append("\n");
            }
        }

        CreateNoteRequest createReq = new CreateNoteRequest();
        createReq.setContent(content.toString());
        createReq.setNoteType(NoteType.POST_INTERVIEW);
        return createNote(userId, applicationId, createReq);
    }

    
    private InterviewPrep fetchOwnedPrep(UUID userId, UUID applicationId) {
        // Спершу перевіряємо ownership заявки (tenant safety)
        applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found: " + applicationId));

        return prepRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interview prep not found for application: " + applicationId
                                + " — move application to INTERVIEW status first"));
    }

    
    private void scheduleEmbeddingGeneration(UUID noteId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    embeddingGenerator.generate(noteId);
                }
            });
        } else {
            // Edge case: викликаються поза транзакцією. Запускаємо одразу.
            // На практиці не повинно траплятись, але страховка.
            LOGGER.warn("scheduleEmbeddingGeneration called outside transaction for note {}", noteId);
            embeddingGenerator.generate(noteId);
        }
    }
}