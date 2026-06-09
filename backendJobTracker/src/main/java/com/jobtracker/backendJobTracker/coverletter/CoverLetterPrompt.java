package com.jobtracker.backendJobTracker.coverletter;

public final class CoverLetterPrompt {
 
    private CoverLetterPrompt() {
    }
 
    public static final String VERSION = "v1";
 
   
    public static String build(String jobContext, String cvContext, String tone, String extraInstructions) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_INSTRUCTION);
        sb.append("\n\nTONE: ").append(tone);
        if (extraInstructions != null && !extraInstructions.isBlank()) {
            sb.append("\n\nADDITIONAL INSTRUCTIONS FROM CANDIDATE: ").append(extraInstructions);
        }
        sb.append("\n\n=== JOB POSTING ===\n").append(jobContext);
        sb.append("\n\n=== CANDIDATE CV ===\n").append(cvContext);
        return sb.toString();
    }
 
    private static final String SYSTEM_INSTRUCTION = """
            You write tailored cover letters for job applications. Given a job posting
            and candidate's CV, write a cover letter in plain text (no markdown,
            no headers, no greetings/sign-offs unless the tone calls for it).
 
            Output ONLY the body of the letter. No preamble like "Here is your
            cover letter:". No closing notes from you. Just the letter text.
 
            Rules:
            - 3-4 paragraphs, 200-350 words total.
            - Language: match the job posting language (Polish job → Polish letter,
              English job → English letter). If mixed, default to English.
            - First paragraph: state interest in the specific position at the specific
              company. Mention 1-2 things that attract you to THIS role (from job desc).
            - Middle paragraph(s): connect candidate's experience and skills to job
              requirements. Use specific examples from CV. Match terminology — if
              job says "Spring Boot" and CV has "Spring", say "Spring Boot experience
              (Spring framework in CV)".
            - Last paragraph: brief, forward-looking. Indicate availability / next steps.
            - NEVER invent experience, skills, or achievements not present in the CV.
              If the job requires X and CV doesn't have X — either skip mentioning X
              or honestly position related experience.
            - Avoid clichés: "I am a hard-working team player", "passionate about coding".
              Be specific instead.
 
            Tone rules:
            - FORMAL: traditional business style. "Dear Hiring Manager," opening allowed.
              No contractions ("I am", not "I'm"). Polite, structured.
            - CONVERSATIONAL: friendly, direct, first-person. Contractions OK.
              No "Dear Sir/Madam" formalities — start with the substance.
            - ENTHUSIASTIC: show genuine excitement for the role and company. Still
              professional, but warmer. Action verbs, energetic phrasing.
            """;
}

