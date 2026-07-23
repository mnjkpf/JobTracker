package com.jobtracker.backendJobTracker.interview.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jobtracker.backendJobTracker.interview.notes.dto.SimilarInterviewNote;
import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;

/**
 * Unit-тести {@link InterviewPrepRagPrompt#formatPastNotes} (7C — форматування нотаток).
 */
class InterviewPrepRagPromptTest {

    private SimilarInterviewNote note(NoteType type, String content, double similarity) {
        SimilarInterviewNote n = new SimilarInterviewNote();
        n.setId(UUID.randomUUID());
        n.setNoteType(type);
        n.setContent(content);
        n.setSimilarity(similarity);
        return n;
    }

    @Test
    @DisplayName("null -> null")
    void nullNotes() {
        assertThat(InterviewPrepRagPrompt.formatPastNotes(null)).isNull();
    }

    @Test
    @DisplayName("порожній список -> null")
    void emptyNotes() {
        assertThat(InterviewPrepRagPrompt.formatPastNotes(List.of())).isNull();
    }

    @Test
    @DisplayName("нотатки форматуються з --- роздільниками, типом і relevance")
    void formatsWithSeparators() {
        String out = InterviewPrepRagPrompt.formatPastNotes(List.of(
                note(NoteType.PREP_NOTE, "review transactions", 0.62),
                note(NoteType.POST_INTERVIEW, "failed N+1", 0.55)));

        assertThat(out)
                .contains("[Type: PREP_NOTE")
                .contains("[Type: POST_INTERVIEW")
                .contains("review transactions")
                .contains("failed N+1")
                .contains("---");
        // два роздільники для двох нотаток
        assertThat(out.split("---", -1)).hasSize(3);
    }

    @Test
    @DisplayName("similarity округляється до 2 знаків (не сирий 0.6154321)")
    void similarityFormat() {
        String out = InterviewPrepRagPrompt.formatPastNotes(List.of(
                note(NoteType.PREP_NOTE, "x", 0.6154321)));
        // prod форматує через Locale.US → завжди крапка "0.62", детерміновано
        // (не залежить від locale машини). Округлення до 2 знаків.
        assertThat(out).contains("0.62").doesNotContain("0.6154321");
    }

    @Test
    @DisplayName("більше MAX_NOTES_IN_PROMPT (5) -> лише top-5 у виводі")
    void limitsToMaxNotes() {
        List<SimilarInterviewNote> many = IntStream.range(0, 8)
                .mapToObj(i -> note(NoteType.PREP_NOTE, "note-" + i, 0.9 - i * 0.05))
                .toList();

        String out = InterviewPrepRagPrompt.formatPastNotes(many);

        // top-5 присутні
        for (int i = 0; i < InterviewPrepRagPrompt.MAX_NOTES_IN_PROMPT; i++) {
            assertThat(out).contains("note-" + i);
        }
        // 6-й і далі — відсутні
        assertThat(out).doesNotContain("note-5").doesNotContain("note-7");
    }
}
