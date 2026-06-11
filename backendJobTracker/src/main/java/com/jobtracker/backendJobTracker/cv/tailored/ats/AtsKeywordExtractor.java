package com.jobtracker.backendJobTracker.cv.tailored.ats;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.cv.dto.parse.ParsedSkill;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredSkill;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtsKeywordExtractor {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(AtsKeywordExtractor.class);
 
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    // ВИДАЛЕНО: private final AtsKeywordExtractionPrompt prompt;
    // Це static utility клас — викликаємо AtsKeywordExtractionPrompt.build(...)
 
    public List<String> extract(String jobDescription) {
        if (jobDescription == null || jobDescription.isBlank()) {
            return List.of();
        }
 
        // ВИПРАВЛЕНО: static call. Перейменував змінну щоб не плутала з полем.
        String promptText = AtsKeywordExtractionPrompt.build(jobDescription);
        String json = aiService.complete(promptText);
 
        if (json == null || json.isBlank()) {
            throw new BusinessRuleException("LLM returned empty response for ATS keywords");
        }
 
        return parseAndNormalize(json);
    }
 
    private List<String> parseAndNormalize(String json) {
        List<String> raw;
        try {
            String cleaned = stripMarkdownFences(json);
            raw = objectMapper.readValue(cleaned, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // ВИПРАВЛЕНО: не ковтаємо помилку. Логуємо + кидаємо.
            // Інакше дебагінг неможливий — score 0% без причини.
            LOGGER.error("Failed to parse LLM JSON: {} | raw: {}", e.getMessage(), json);
            throw new BusinessRuleException("Could not parse ATS keywords response");
        }
 
        if (raw == null) return List.of();
 
        // ДОДАНО: normalize + dedup. LLM може повернути "Java", "java", " Java ".
        // Без normalize matching проти CV буде нестабільним.
        return raw.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }
 
    private String stripMarkdownFences(String s) {
        String trimmed = s.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}

