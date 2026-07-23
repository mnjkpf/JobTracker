package com.jobtracker.backendJobTracker.interview.notes;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.ai.EmbeddingService;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class InterviewNoteEmbeddingGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterviewNoteEmbeddingGenerator.class);

    private final InterviewNoteRepository noteRepository;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generate(UUID noteId) {
        InterviewNote note = noteRepository.findById(noteId).orElse(null);
        if (note == null) {
            LOGGER.warn("Note {} not found — skipping embedding generation", noteId);
            return;
        }

        try {
            float[] embedding = embeddingService.getEmbedding(note.getContent());

            // ВИПРАВЛЕНО (КРИТИЧНО): UPDATE interview_notes, не UPDATE skills!
            // У SkillService правильно UPDATE skills, але тут — інша таблиця.
            int updated = jdbcTemplate.update(
                    "UPDATE interview_notes SET embedding = CAST(? AS vector) WHERE id = ?",
                    toVectorLiteral(embedding),
                    noteId);

            LOGGER.debug("Generated embedding for note {} (content length: {}): {} dims, {} rows updated",
                    noteId, note.getContent().length(), embedding.length, updated);
        } catch (Exception e) {
            LOGGER.warn("Failed to generate embedding for note {}: {}", noteId, e.getMessage());
        }
    }

    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}