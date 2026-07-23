package com.jobtracker.backendJobTracker.cv.tailored;

public final class TailoredCvPrompt {
 
    private TailoredCvPrompt() {
    }
 
    public static final String VERSION = "v1";
 
    /**
     * Static — як JobExtractionPrompt, CoverLetterPrompt. Не Spring bean.
     */
    public static String build(String jobContext, String cvContext, String emphasisInstructions) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_INSTRUCTION);
        if (emphasisInstructions != null && !emphasisInstructions.isBlank()) {
            sb.append("\n\nEMPHASIS INSTRUCTIONS FROM CANDIDATE: ").append(emphasisInstructions);
        }
        sb.append("\n\n=== JOB POSTING ===\n").append(jobContext);
        sb.append("\n\n=== MASTER CV ===\n").append(cvContext);
        return sb.toString();
    }
 
    private static final String SYSTEM_INSTRUCTION = """
            You tailor a candidate's master CV to a specific job posting. Output
            ONLY a valid JSON object — no markdown code fences, no explanation,
            no preamble. Just the raw JSON.
 
            Use exactly this schema:
            {
              "language": "PL" | "EN",
              "fullName": string|null,
              "headline": string|null,
              "email": string|null,
              "phone": string|null,
              "linkedInUrl": string|null,
              "githubUrl": string|null,
              "summary": string|null,
              "experiences": [
                {
                  "position": string,
                  "company": string,
                  "location": string|null,
                  "startDate": string,            // "YYYY-MM-DD" or "YYYY-MM" or "YYYY"
                  "endDate": string|null,         // same format, or "present"
                  "description": string|null      // REWRITTEN with emphasis on relevant tech
                }
              ],
              "educations": [
                {
                  "institution": string,
                  "degree": string|null,
                  "fieldOfStudy": string|null,
                  "location": string|null,
                  "startDate": string|null,
                  "endDate": string|null,
                  "description": string|null
                }
              ],
              "skills": [
                {
                  "name": string,                 // lowercase
                  "category": "LANGUAGE"|"FRAMEWORK"|"DATABASE"|"TOOL"|"SOFT"|"OTHER"|null,
                  "level": "BEGINNER"|"INTERMEDIATE"|"ADVANCED"|"EXPERT"|null
                }
              ],
              "projects": [
                {
                  "name": string,
                  "description": string|null,
                  "url": string|null,
                  "technologies": string|null,
                  "startDate": string|null,
                  "endDate": string|null
                }
              ],
              "languages": [
                {
                  "name": string,
                  "level": "A1"|"A2"|"B1"|"B2"|"C1"|"C2"|"NATIVE"
                }
              ]
            }
 
            TAILORING RULES (the core of this prompt):
 
            1. NEVER invent experience, skills, education, projects, or languages
               not present in the master CV. Use ONLY what's there.
 
            2. Factual data is sacred — keep dates, company names, institutions,
               and project names EXACTLY as in master CV. No changes.
 
            3. REORDER items by relevance to the job:
               - experiences: most relevant first, even if not most recent
               - skills: most relevant first
               - projects: most relevant first
 
            4. DROP items that are clearly irrelevant:
               - Summer cashier job when applying for Java backend
               - High school education if candidate has bachelor's+
               - Unrelated hobbies/projects
 
            5. REWRITE descriptions to EMPHASIZE relevant technologies and
               accomplishments. Example:
                 master: "Built backend services for online store"
                 job:    Java + Spring + PostgreSQL
                 tailored: "Built Java/Spring backend services with PostgreSQL
                            handling e-commerce transactions"
               Do NOT add tech not in candidate's actual experience.
 
            6. SUMMARY: rewrite to align with job. Keep length 2-4 sentences.
               If master has no summary, generate one based on top experiences.
 
            7. HEADLINE: align with job position. Master "Backend Engineer" → if
               job is "Java Developer", headline can be "Java Backend Engineer".
               Only if it's truthful given the experience.
 
            8. LANGUAGE: match the job posting language.
               - Polish job → Polish CV (translate descriptions if needed)
               - English job → English CV
               - Keep contact data (email/phone/urls) as-is.
 
            9. SKILLS section: include skills the candidate actually has from
               master CV, ordered by relevance. Don't filter out — list ALL,
               but ordering matters.
 
            10. Arrays must always be present (use [] if empty), never null.
            """;
}

