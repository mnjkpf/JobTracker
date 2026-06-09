package com.jobtracker.backendJobTracker.application.gap;

public class JobSkillExtractionPrompt {

    private JobSkillExtractionPrompt() {
    }
 
    public static final String VERSION = "v1";
 
    public static String build(String jobText) {
        return SYSTEM_INSTRUCTION + "\n\n=== JOB POSTING TEXT ===\n" + jobText;
    }
 
    
    private static final String SYSTEM_INSTRUCTION = """
            You extract skills required by a job posting. The text may be in Polish
            or English. Output ONLY a valid JSON object — no markdown code fences,
            no explanation, no preamble. Just the raw JSON.
 
            Use exactly this schema:
            {
              "required": [
                { "name": string, "category": "LANGUAGE"|"FRAMEWORK"|"DATABASE"|"TOOL"|"SOFT"|"OTHER"|null }
              ],
              "niceToHave": [
                { "name": string, "category": "LANGUAGE"|"FRAMEWORK"|"DATABASE"|"TOOL"|"SOFT"|"OTHER"|null }
              ]
            }
 
            Rules:
            - name: lowercase, e.g. "java", "spring boot", "postgresql". No version
              numbers, no version suffixes ("java 17" → "java").
            - category mapping: programming/query languages (Java, SQL) → LANGUAGE;
              libraries/frameworks (Spring, React) → FRAMEWORK; data stores
              (Postgres, MongoDB, Redis) → DATABASE; tools/platforms (Docker, Git,
              AWS) → TOOL; soft skills (teamwork, communication) → SOFT; everything
              else → OTHER. If unclear → null.
            - required vs niceToHave classification:
                * "Requirements", "Must have", "Required skills" → required
                * "Wymagania", "Wymagane umiejętności" (Polish) → required
                * "Nice to have", "Would be a plus", "Bonus", "Plus" → niceToHave
                * "Mile widziane", "Atutem będzie" (Polish) → niceToHave
                * Якщо неясно — required (краще fail safe — gap analysis покаже все).
            - Do NOT infer skills not explicitly stated. CV says "experience with
              backend" — це не "java".
            - Both arrays must always be present (use [] if empty), never null.
            - Skip generic terms like "experience", "knowledge", "ability to" —
              витягуй лише назву технології/навички.
            """;


}
