package com.jobtracker.backendJobTracker.interview;





/**
 * Prompt для генерації personalized interview prep guide.
 * <p>
 * <b>v2:</b> додано pastNotesContext — для RAG cross-interview learning (7C).
 * LLM використовує нотатки з минулих preps юзера як reference.
 */
public final class InterviewPrepPrompt {

    private InterviewPrepPrompt() {
    }

    // ВИПРАВЛЕНО: v1 → v2 — у БД зможемо відрізнити preps з RAG від без RAG
    public static final String VERSION = "v2";

    /**
     * @param jobContext         "Position: ...\nCompany: ...\n..."
     * @param cvContext          "Name: ...\nSummary: ...\n..."
     * @param pastNotesContext   нотатки з минулих preps (може бути null/blank)
     */
    public static String build(String jobContext, String cvContext, String pastNotesContext) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_INSTRUCTION);
        sb.append("\n\n=== JOB POSTING ===\n").append(jobContext);
        sb.append("\n\n=== CANDIDATE CV ===\n").append(cvContext);

        // Past notes секція — лише якщо є. Graceful: перші співбесіди без RAG.
        if (pastNotesContext != null && !pastNotesContext.isBlank()) {
            sb.append("\n\n=== RELEVANT PAST INTERVIEW NOTES ===\n").append(pastNotesContext);
        }

        return sb.toString();
    }

    private static final String SYSTEM_INSTRUCTION = """
            You generate a personalized interview preparation guide for a candidate
            applying to a specific job. Output ONLY a valid JSON object — no
            markdown code fences, no explanation, no preamble. Just the raw JSON.

            Use exactly this schema:
            {
              "technical": [
                { "question": string, "suggestedAnswer": string }
              ],
              "behavioral": [
                { "question": string, "suggestedAnswer": null }
              ],
              "questionsToAsk": [
                { "question": string, "suggestedAnswer": null }
              ]
            }

            Counts: technical 8-10, behavioral 5, questionsToAsk 5.

            === TECHNICAL QUESTIONS ===

            1. Use the ACTUAL tech stack from job posting.
            2. Match candidate's seniority (Junior/Mid/Senior).
            3. Mix categories: core language, framework, database, general.
            4. suggestedAnswer: 2-3 sentences max, outline what good answer covers.

            === BEHAVIORAL QUESTIONS ===

            5. Adapt to company type (infer from description):
               - Bank/Corporate: process, responsibility, compliance, legacy code
               - Startup: autonomy, ambiguous requirements, prioritization
               - Product company: user-focus, cross-team collaboration
            6. suggestedAnswer: ALWAYS null. Candidate writes own STAR.

            === QUESTIONS TO ASK ===

            7. Genuinely useful, specific (not "what's the culture like").
            8. suggestedAnswer: ALWAYS null.

            === USING RELEVANT PAST NOTES (when provided) ===

            If RELEVANT PAST INTERVIEW NOTES section is present below, this
            candidate has interviewed before for similar roles. The notes are
            from those past interviews — both PREP_NOTE (preparation thoughts)
            and POST_INTERVIEW (reflections on what actually happened).

            Use these notes to:
            - If a past note mentions "asked about X" — strongly consider
              including X-related question in technical (this company asks similar)
            - If a note mentions "I failed Y" or "I confused Y with Z" — include
              question about Y/Z with a MORE DETAILED suggestedAnswer that
              addresses the specific confusion
            - If a note mentions a useful follow-up question the candidate
              asked — consider adding similar to questionsToAsk
            - If a note describes company-specific patterns (e.g. "Polish banks
              tested SQL hard") — adapt the prep accordingly

            DO NOT:
            - Copy past notes verbatim into the prep guide — synthesize
            - Force questions that are clearly off-topic for current role
            - Mention "based on your past interviews" in the output

            If no RELEVANT PAST INTERVIEW NOTES section is provided, generate
            the prep guide normally — this is the candidate's first interview
            in this domain.

            === GENERAL RULES ===

            - Language: match job posting (Polish job → Polish prep).
            - DO NOT invent skills the candidate doesn't have.
            - Order: most important first within each category.
            """;
}