package com.jobtracker.backendJobTracker.cv.tailored.ats;

public final class AtsKeywordExtractionPrompt {
 
    private AtsKeywordExtractionPrompt() {
    }
 
    public static final String VERSION = "v1";
 
    public static String build(String jobDescription) {
        return SYSTEM_INSTRUCTION + "\n\n=== JOB POSTING TEXT ===\n" + jobDescription;
    }
 
    private static final String SYSTEM_INSTRUCTION = """
            You extract ATS (Applicant Tracking System) keywords from a job posting.
            ATS software scans CVs for these keywords — anything not in the keyword
            list is invisible to ATS. The candidate uses your output to ensure
            their CV contains the right keywords.
 
            Output ONLY a valid JSON array of strings — no markdown code fences,
            no explanation, no preamble. Just the raw JSON array.
 
            Schema:
            ["keyword1", "keyword2", "keyword3", ...]
 
            Example output:
            ["java", "spring boot", "postgresql", "docker", "kubernetes", "rest api",
             "microservices", "agile", "git", "ci/cd", "junit", "communication"]
 
            Rules:
 
            1. Extract up to 30 most important keywords. Prefer specific over general
               (prefer "spring boot" over "framework"; prefer "postgresql" over "database"
               IF posting names PostgreSQL).
 
            2. Categories to include:
               - Technologies: programming languages, frameworks, libraries, databases,
                 tools, platforms (java, react, postgresql, docker, aws)
               - Soft skills: only IF explicitly mentioned (communication, teamwork,
                 problem solving). Skip generic filler.
               - Certifications: if mentioned (aws certified, scrum master, ocpjp)
               - Methodologies: agile, scrum, tdd, ci/cd
 
            3. Format rules:
               - Lowercase ALL keywords ("Java" → "java", "Spring Boot" → "spring boot")
               - NO version numbers ("Java 17" → "java"; "Python 3.10" → "python")
               - Keep multi-word terms as single string ("spring boot", not split)
               - No punctuation except internal hyphens/slashes ("ci/cd", "test-driven")
 
            4. Polish job postings: extract Polish keywords too if they appear.
               "Wymagania: Java, Spring" → ["java", "spring"]
               "Mile widziane: Docker" → ["docker"]
               Soft skills у польській: "komunikatywność" → "komunikatywność" or
               translate to "communication" if mixed-language posting.
 
            5. Do NOT infer keywords not explicitly stated. If posting doesn't
               mention "kubernetes", don't add "kubernetes" just because it's
               common in similar jobs.
 
            6. Skip generic filler:
               - "experience with X" → extract just X
               - "knowledge of X" → extract just X
               - Years/numbers ("5+ years", "3 years") — skip entirely
               - Vague terms: "passion", "motivation", "team player" (unless
                 specifically called out as required skill)
 
            7. Deduplicate. If posting says "Java" 5 times, output "java" once.
 
            8. Output must be a valid JSON array, even if empty: []
            """;
}

