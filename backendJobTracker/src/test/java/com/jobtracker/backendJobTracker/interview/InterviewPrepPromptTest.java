package com.jobtracker.backendJobTracker.interview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit-тести {@link InterviewPrepPrompt} (7A/7C — формування prompt-рядка).
 */
class InterviewPrepPromptTest {

    private static final String JOB = "Position: Java Dev";
    private static final String CV = "Name: Jan";

    @Test
    @DisplayName("VERSION == v2 (RAG-версія)")
    void version_isV2() {
        assertThat(InterviewPrepPrompt.VERSION).isEqualTo("v2");
    }

    @Test
    @DisplayName("pastNotesContext == null -> секція PAST NOTES відсутня")
    void nullPastNotes_noSection() {
        String prompt = InterviewPrepPrompt.build(JOB, CV, null);
        assertThat(prompt)
                .doesNotContain("RELEVANT PAST INTERVIEW NOTES ===\n")
                .contains("=== JOB POSTING ===")
                .contains("=== CANDIDATE CV ===");
    }

    @Test
    @DisplayName("pastNotesContext blank -> секція PAST NOTES відсутня")
    void blankPastNotes_noSection() {
        String prompt = InterviewPrepPrompt.build(JOB, CV, "   ");
        assertThat(prompt).doesNotContain("=== RELEVANT PAST INTERVIEW NOTES ===\n");
    }

    @Test
    @DisplayName("pastNotesContext заданий -> секція PAST NOTES присутня з контентом")
    void withPastNotes_sectionPresent() {
        String prompt = InterviewPrepPrompt.build(JOB, CV, "[Type: PREP_NOTE] failed N+1");
        assertThat(prompt)
                .contains("=== RELEVANT PAST INTERVIEW NOTES ===")
                .contains("failed N+1");
    }

    @Test
    @DisplayName("порядок секцій: JOB POSTING перед CANDIDATE CV перед PAST NOTES")
    void sectionsOrder() {
        String prompt = InterviewPrepPrompt.build(JOB, CV, "past notes here");
        int job = prompt.indexOf("=== JOB POSTING ===");
        int cv = prompt.indexOf("=== CANDIDATE CV ===");
        int notes = prompt.indexOf("=== RELEVANT PAST INTERVIEW NOTES ===");
        assertThat(job).isLessThan(cv);
        assertThat(cv).isLessThan(notes);
        // jobContext/cvContext значення підставлені
        assertThat(prompt).contains(JOB).contains(CV);
    }
}
