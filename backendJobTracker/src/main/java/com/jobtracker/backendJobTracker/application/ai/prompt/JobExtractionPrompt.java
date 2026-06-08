package com.jobtracker.backendJobTracker.application.ai.prompt;

/**
 * Prompt для extraction структурованих даних вакансії з сирого тексту.
 * Жорстка вимога ONLY JSON, явна схема з enum-значеннями, null для невідомого.
 */
public final class JobExtractionPrompt {
 
    private JobExtractionPrompt() {
    }
 
    public static final String VERSION = "v1";
 
    public static String build(String jobText) {
        return SYSTEM_INSTRUCTION + "\n\n=== JOB POSTING TEXT ===\n" + jobText;
    }
 
    private static final String SYSTEM_INSTRUCTION = """
            You extract structured data from job postings. The text may be in
            Polish or English. Output ONLY a valid JSON object — no markdown code
            fences, no explanation, no preamble. Just the raw JSON.
 
            Use exactly this schema:
            {
              "position": string,          // job title, e.g. "Junior Java Developer"
              "companyName": string,       // hiring company name
              "description": string,       // 2-4 sentence summary of responsibilities and requirements
              "seniority": string|null,    // one of: INTERN, JUNIOR, JUNIOR_PLUS, MID, SENIOR, LEAD
              "contractType": string|null, // one of: UOP, B2B, UZ, UMOWA_O_DZIELO, NOT_SPECIFIED
              "workMode": string|null,     // one of: ONSITE, HYBRID, REMOTE, NOT_SPECIFIED
              "location": string|null,     // city, e.g. "Warsaw", "Kraków", or "Remote"
              "salaryMin": number|null,    // monthly minimum, integer, no currency symbol
              "salaryMax": number|null,    // monthly maximum, integer
              "salaryCurrency": string|null // 3-letter code: PLN, EUR, USD
            }
 
            Rules:
            - If a field is not present in the text, use null. Do NOT guess or invent.
            - For seniority: map Polish terms (junior→JUNIOR, mid/regular→MID, senior→SENIOR).
            - For contractType: "B2B" or "kontrakt B2B"→B2B; "umowa o pracę"→UOP;
              "umowa zlecenie"→UZ; "umowa o dzieło"→UMOWA_O_DZIELO; unclear→NOT_SPECIFIED.
            - For workMode: "zdalna"/"remote"→REMOTE; "hybrydowa"/"hybrid"→HYBRID;
              "stacjonarna"/"onsite"→ONSITE; unclear→NOT_SPECIFIED.
            - Salary: extract numbers only. "8 000 - 12 000 PLN" → salaryMin=8000,
              salaryMax=12000, salaryCurrency="PLN". Ignore "/month", "netto", "brutto" for the number.
            - position and companyName are required — extract your best guess even if uncertain.
            """;
}

