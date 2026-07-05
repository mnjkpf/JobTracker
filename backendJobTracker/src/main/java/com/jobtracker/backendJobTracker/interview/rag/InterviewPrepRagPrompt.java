package com.jobtracker.backendJobTracker.interview.rag;

import java.util.List;
import java.util.Locale;

import com.jobtracker.backendJobTracker.interview.notes.dto.SimilarInterviewNote;

public final class InterviewPrepRagPrompt {
 
    private InterviewPrepRagPrompt() {
    }
 
    /**
     * Top-N нотаток з результатів. Більше шкодить фокусу LLM —
     * context window розмивається, питання стають менш цільові.
     */
    public static final int MAX_NOTES_IN_PROMPT = 5;
 
    /**
     * Форматує нотатки у текст для секції === RELEVANT PAST INTERVIEW NOTES ===
     * у InterviewPrepPrompt. Беремо top-N (sorted by similarity DESC).
     *
     * @param notes нотатки від RAG retrieval (вже sorted)
     * @return відформатований текст, або {@code null} якщо нема нотаток
     *         ({@link InterviewPrepPrompt#build} пропустить секцію якщо null)
     */
    public static String formatPastNotes(List<SimilarInterviewNote> notes) {
        if (notes == null || notes.isEmpty()) {
            return null;
        }
 
        StringBuilder sb = new StringBuilder();
        notes.stream().limit(MAX_NOTES_IN_PROMPT).forEach(note -> {
            sb.append("[Type: ").append(note.getNoteType())
                    .append(", relevance: ").append(String.format(Locale.US, "%.2f", note.getSimilarity()))
                    .append("]\n");
            sb.append(note.getContent()).append("\n");
            sb.append("---\n");
        });
        return sb.toString();
    }
}

