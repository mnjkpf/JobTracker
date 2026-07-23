package com.jobtracker.backendJobTracker.cv.tailored.ats;

import java.time.Instant;
import java.util.List;
 
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class AtsScoreResponse {
 
    /** Інтегральна оцінка 0-100. matchedCount/totalKeywords × 100, округлене. */
    private int score;
 
    /** Скільки ключових слів LLM витягнув з вакансії. */
    private int totalKeywords;
 
    /** Скільки з них реально знайдено у tailored CV (case-insensitive contains). */
    private int matchedCount;
 
    /** Знайдені у CV — для UI зеленим. */
    private List<String> matched;
 
    /** Не знайдені у CV — список який треба додати/підкреслити. UI червоним. */
    private List<String> missing;
 
    /** Коли проведено аналіз. */
    private Instant analyzedAt;
}
 
