package com.jobtracker.backendJobTracker.cv;

public final class CvExtractionPrompt {
 
    private CvExtractionPrompt() {
    }
 
    public static final String VERSION = "v1";
 
    public static String build(String cvText) {
        return SYSTEM_INSTRUCTION + "\n\n=== CV TEXT ===\n" + cvText;
    }
 
    private static final String SYSTEM_INSTRUCTION = """
            You extract structured CV data. The CV text may be in Polish or English.
            Output ONLY a valid JSON object — no markdown code fences, no explanation,
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
                  "startDate": string,           // "YYYY-MM-DD" or "YYYY-MM" or "YYYY"
                  "endDate": string|null,        // same formats, or "present" for current
                  "description": string|null
                }
              ],
              "educations": [
                {
                  "institution": string,
                  "degree": string|null,         // "Bachelor", "Master", "Engineer", "PhD"
                  "fieldOfStudy": string|null,
                  "startDate": string|null,
                  "endDate": string|null,
                  "description": string|null
                }
              ],
              "skills": [
                {
                  "name": string,                // lowercase, e.g. "java", "spring boot"
                  "category": "LANGUAGE"|"FRAMEWORK"|"DATABASE"|"TOOL"|"SOFT"|"OTHER"|null,
                  "level": "BEGINNER"|"INTERMEDIATE"|"ADVANCED"|"EXPERT"|null
                }
              ],
              "projects": [
                {
                  "name": string,
                  "description": string|null,
                  "url": string|null,            // GitHub or demo URL
                  "technologies": string|null,   // CSV: "Java, Spring, Postgres"
                  "startDate": string|null,
                  "endDate": string|null
                }
              ],
              "languages": [
                {
                  "name": string,                // e.g. "English", "Polish"
                  "level": "A1"|"A2"|"B1"|"B2"|"C1"|"C2"|"NATIVE"
                }
              ]
            }
 
            Rules:
            - If a field is not present in the CV, use null. Do NOT guess or invent.
            - language: detect from CV text. If mixed PL+EN, choose dominant.
            - skills.category mapping: programming languages and DB query languages
              (SQL) → LANGUAGE; libraries/frameworks (Spring, React) → FRAMEWORK;
              databases (Postgres, MongoDB) → DATABASE; tools (Docker, Git) → TOOL;
              soft skills (teamwork, communication) → SOFT; everything else → OTHER.
              If unclear → null.
            - skills.level: only set if CV explicitly indicates ("expert", "5+ years",
              etc.). Don't infer from years alone — null if uncertain.
            - educations.degree: Polish mapping — "licencjat"→"Bachelor",
              "inżynier"→"Engineer", "magister"→"Master", "doktor"→"PhD".
            - Date formats:
                "2022-06-15" (preferred) → keep as-is
                "Czerwiec 2022" / "June 2022" → "2022-06"
                "2022" → "2022"
                "obecnie" / "present" / "current" / "do teraz" → "present"
            - experiences[].position, experiences[].company, experiences[].startDate
              are required. If you cannot find them — skip that experience entry,
              do not output partial entries.
            - educations[].institution is required.
            - projects[].name is required.
            - skills[].name is required, must be lowercase.
            - languages[].name and languages[].level are required.
            - Arrays must be present (empty [] if no items), never null.
            """;
}
