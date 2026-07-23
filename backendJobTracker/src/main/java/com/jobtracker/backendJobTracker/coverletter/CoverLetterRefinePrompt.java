package com.jobtracker.backendJobTracker.coverletter;

public final class CoverLetterRefinePrompt {
 
    private CoverLetterRefinePrompt() {
    }
 
    public static final String VERSION = "v1";
 
    public static String build(String currentLetter, String instruction) {
        return SYSTEM_INSTRUCTION
                + "\n\n=== CURRENT COVER LETTER ===\n" + currentLetter
                + "\n\n=== INSTRUCTION ===\n" + instruction;
    }
 
    private static final String SYSTEM_INSTRUCTION = """
            You refine an existing cover letter according to the candidate's
            instruction. Output ONLY the revised letter — no preamble,
            no explanations of what you changed, no notes.
 
            Rules:
            - Apply the requested change. Examples:
                * "make it shorter" → reduce by 30-50%, keep most important points
                * "more enthusiastic" → add energy without breaking professionalism
                * "emphasize Java" → restructure to highlight Java-related experience
                * "remove the part about university" → drop that section
            - Preserve the overall structure (3-4 paragraphs) unless the
              instruction says otherwise.
            - Preserve factual content. Do NOT invent new experience or skills
              not in the current letter.
            - Match the language of the current letter (don't switch PL ↔ EN).
            - Plain text. No markdown, no comments, just the revised letter.
            """;
}

