package com.jobtracker.backendJobTracker.interview;



/**
 * Prompt для генерації personalized interview prep guide.
 * <p>
 * <b>Що повертає LLM:</b> JSON з трьома масивами питань:
 *  - technical — за стеком вакансії, з suggested answer outline
 *  - behavioral — за типом компанії (LLM визначає тип з опису), без відповідей
 *  - questionsToAsk — що юзер ставить інтерв'юеру, без відповідей
 * <p>
 * <b>Унікальні правила:</b>
 * <ol>
 *  <li>Питання адаптовані ПІД стек вакансії (не generic Java fluff)</li>
 *  <li>Behavioral залежать від типу компанії (банк vs стартап різні)</li>
 *  <li>Suggested answers — точкові підказки, не повні відповіді (юзер сам напише STAR)</li>
 *  <li>Match seniority level — не питати "Implement custom JVM" для junior</li>
 * </ol>
 */
public final class InterviewPrepPrompt {

    private InterviewPrepPrompt() {
    }

    public static final String VERSION = "v1";

    /**
     * @param jobContext "Position: ...\nCompany: ...\nSeniority: ...\nTechnologies: ...\nDescription: ..."
     * @param cvContext  "Name: ...\nSummary: ...\nSkills: ...\nExperience overview: ..."
     */
    public static String build(String jobContext, String cvContext) {
        return SYSTEM_INSTRUCTION
                + "\n\n=== JOB POSTING ===\n" + jobContext
                + "\n\n=== CANDIDATE CV ===\n" + cvContext;
    }

    private static final String SYSTEM_INSTRUCTION = """
            You generate a personalized interview preparation guide for a candidate
            applying to a specific job. Output ONLY a valid JSON object — no
            markdown code fences, no explanation, no preamble. Just the raw JSON.

            Use exactly this schema:
            {
              "technical": [
                {
                  "question": string,
                  "suggestedAnswer": string         // brief outline, NOT full answer
                }
              ],
              "behavioral": [
                {
                  "question": string,
                  "suggestedAnswer": null           // candidate writes own STAR
                }
              ],
              "questionsToAsk": [
                {
                  "question": string,
                  "suggestedAnswer": null           // these are questions FROM candidate
                }
              ]
            }

            Counts:
            - technical: 8-10 questions
            - behavioral: 5 questions
            - questionsToAsk: 5 questions

            === TECHNICAL QUESTIONS ===

            1. Use the ACTUAL tech stack from job posting. If job says
               "Java, Spring Boot, PostgreSQL" — questions about Java/Spring/Postgres,
               NOT generic OOP fluff.

            2. Match the candidate's seniority:
               - Junior: fundamentals, syntax, basic concepts ("what is HashMap",
                 "explain @Transactional")
               - Mid: practical scenarios, design choices ("when use @Async vs Kafka",
                 "explain N+1 problem")
               - Senior: architecture, trade-offs, system design

            3. Mix categories within technical:
               - Core language (Java specifics, Java memory model)
               - Framework (Spring, ORM behavior)
               - Database (queries, indexing, transactions)
               - General (REST, HTTP, concurrency, testing)

            4. suggestedAnswer format:
               - 2-3 sentences max
               - Outline what good answer covers, not full prose
               - Mention key terms candidate should use
               - Example: "Define HashMap as hash table backed by array. Mention
                 load factor (0.75), resize at threshold, collision handling
                 (chaining → tree at threshold 8 in Java 8+). Mention thread-unsafe
                 vs ConcurrentHashMap."

            === BEHAVIORAL QUESTIONS ===

            5. Adapt to company type (infer from description):
               - Bank/Corporate (formal language, compliance keywords):
                 process-oriented, responsibility, conflict with stakeholders,
                 working with legacy code, attention to detail
               - Startup (informal language, "fast-paced", "ownership"):
                 autonomy, ambiguous requirements, wearing multiple hats,
                 dealing with failure, prioritization with limited resources
               - Product company (mid-size, "users", "features"):
                 user-focus, cross-team collaboration, technical decisions
                 with product trade-offs

            6. Standard behavioral patterns:
               - "Tell me about a time when..."
               - "How would you handle..."
               - "Describe a difficult..."

            7. suggestedAnswer: ALWAYS null for behavioral. Candidate writes own
               STAR-format response in notes.

            === QUESTIONS TO ASK ===

            8. Genuinely useful questions for the candidate:
               - About team structure, code review process, tech stack evolution
               - About growth/learning opportunities
               - About one specific aspect from job description that needs clarity
               - About product/project they'll work on
               - About company culture / remote work / decision making

            9. Avoid generic "what's the culture like" — be specific.
               Example good: "Could you walk me through what the first 30 days
               of an engineer on this team typically look like?"

            10. suggestedAnswer: ALWAYS null — these are FROM candidate.

            === GENERAL RULES ===

            - Language: match job posting language (Polish job → Polish prep,
              English job → English prep). For Polish bank jobs, expect Polish
              + English technical terms mix.
            - DO NOT invent skills the candidate doesn't have. If CV doesn't show
              Kafka but job mentions it — generate technical question about Kafka
              (candidate needs to learn it), but suggestedAnswer should reflect
              "starting from basics: pub/sub, topics, consumer groups".
            - Order: most important/likely-to-be-asked first within each category
              (displayOrder set client-side by array order).
            """;
}