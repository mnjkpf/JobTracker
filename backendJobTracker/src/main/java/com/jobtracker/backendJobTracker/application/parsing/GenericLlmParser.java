package com.jobtracker.backendJobTracker.application.parsing;

import java.util.Optional;
 
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.application.ai.AiService;
import com.jobtracker.backendJobTracker.application.ai.prompt.JobExtractionPrompt;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.application.parsing.dto.ParsedJobPosting;
 
import lombok.RequiredArgsConstructor;

/**
 * Універсальний парсер через LLM. Працює на будь-якому тексті. Fallback коли
 * specific parser не зміг, АБО основний метод для невідомих сайтів / manual paste.
 */

@Component
@RequiredArgsConstructor
public class GenericLlmParser implements JobBoardParser {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericLlmParser.class);
 
    private final AiService aiService;
    private final ObjectMapper objectMapper;
 
    @Override
    public SourceBoard supportedBoard() {
        return SourceBoard.OTHER;
    }
 
    @Override
    public Optional<ParsedJobPosting> parse(String html, String url) {
        String visibleText = Jsoup.parse(html).text();
 
        if (visibleText.isBlank()) {
            LOGGER.warn("No visible text extracted from {}", url);
            return Optional.empty();
        }
 
        String trimmed = visibleText.length() > 12_000
                ? visibleText.substring(0, 12_000)
                : visibleText;
 
        String prompt = JobExtractionPrompt.build(trimmed);
        String json;
        try {
            json = aiService.complete(prompt);
        } catch (Exception e) {
            LOGGER.error("LLM extraction failed for {}: {}", url, e.getMessage());
            return Optional.empty();
        }
 
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
 
        return parseJson(json, url);
    }
 
    /** Прямий парсинг тексту для manual paste (без HTML). */
    public Optional<ParsedJobPosting> parseRawText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String prompt = JobExtractionPrompt.build(text);
        String json = aiService.complete(prompt);
        return parseJson(json, "manual-paste");
    }
 
    private Optional<ParsedJobPosting> parseJson(String json, String source) {
        try {
            String cleaned = stripMarkdownFences(json);
            JsonExtraction dto = objectMapper.readValue(cleaned, JsonExtraction.class);
 
            // .builder() зберігся при переході на class — цей виклик не змінився.
            ParsedJobPosting result = ParsedJobPosting.builder()
                    .position(dto.position)
                    .companyName(dto.companyName)
                    .description(dto.description)
                    .seniority(parseEnum(Seniority.class, dto.seniority))
                    .contractType(parseEnum(ContractType.class, dto.contractType))
                    .workMode(parseEnum(WorkMode.class, dto.workMode))
                    .location(dto.location)
                    .salaryMin(dto.salaryMin)
                    .salaryMax(dto.salaryMax)
                    .salaryCurrency(dto.salaryCurrency)
                    .parsedByLlm(true)
                    .build();
 
            return Optional.of(result);
 
        } catch (Exception e) {
            LOGGER.error("Failed to parse LLM JSON for {}: {} | raw: {}", source, e.getMessage(), json);
            return Optional.empty();
        }
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
 
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Unknown enum value '{}' for {}", value, enumClass.getSimpleName());
            return null;
        }
    }
 
    private static class JsonExtraction {
        public String position;
        public String companyName;
        public String description;
        public String seniority;
        public String contractType;
        public String workMode;
        public String location;
        public Integer salaryMin;
        public Integer salaryMax;
        public String salaryCurrency;
    }
}
