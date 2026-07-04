package com.jobtracker.backendJobTracker.interview.notes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jobtracker.backendJobTracker.ai.EmbeddingService;

/**
 * Unit-тести {@link InterviewNoteEmbeddingGenerator} (7B, async embedding).
 */
@ExtendWith(MockitoExtension.class)
class InterviewNoteEmbeddingGeneratorTest {

    @Mock private InterviewNoteRepository noteRepository;
    @Mock private EmbeddingService embeddingService;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private InterviewNoteEmbeddingGenerator generator;

    private final UUID noteId = UUID.randomUUID();

    private InterviewNote noteWithContent(String content) {
        InterviewNote note = new InterviewNote();
        note.setId(noteId);
        note.setContent(content);
        return note;
    }

    @Test
    @DisplayName("generate: нотатки нема -> без embedding, без UPDATE")
    void generate_noteNotFound() {
        when(noteRepository.findById(noteId)).thenReturn(Optional.empty());

        generator.generate(noteId);

        verifyNoInteractions(embeddingService, jdbcTemplate);
    }

    @Test
    @DisplayName("generate: оновлює саме interview_notes (не skills!) з vector-літералом")
    void generate_updatesInterviewNotes() {
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(noteWithContent("Failed N+1")));
        when(embeddingService.getEmbedding("Failed N+1")).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        when(jdbcTemplate.update(anyString(), any(), any())).thenReturn(1);

        generator.generate(noteId);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> literal = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(sql.capture(), literal.capture(), eq(noteId));

        assertThat(sql.getValue()).contains("UPDATE interview_notes").contains("embedding");
        assertThat((String) literal.getValue()).startsWith("[").endsWith("]").contains(",").contains("0.1");
    }

    @Test
    @DisplayName("generate: embedding кидає виняток -> проковтується, UPDATE не викликається, без падіння")
    void generate_embeddingFailsGracefully() {
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(noteWithContent("text")));
        when(embeddingService.getEmbedding(anyString())).thenThrow(new RuntimeException("OpenAI down"));

        assertThatCode(() -> generator.generate(noteId)).doesNotThrowAnyException();

        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }
}
