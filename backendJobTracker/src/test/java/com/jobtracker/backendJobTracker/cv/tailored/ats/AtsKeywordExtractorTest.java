package com.jobtracker.backendJobTracker.cv.tailored.ats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;

/**
 * Unit-тести для {@link AtsKeywordExtractor}. ObjectMapper реальний (парсинг
 * JSON-масиву рядків). Перевіряємо парсинг, normalize+dedup, guard порожнього
 * опису, помилку парсингу та strip markdown fences.
 */
@ExtendWith(MockitoExtension.class)
class AtsKeywordExtractorTest {

    @Mock private AiService aiService;
    private AtsKeywordExtractor extractor;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        extractor = new AtsKeywordExtractor(aiService, objectMapper);
    }

    @Test
    @DisplayName("парсинг JSON array -> List<String>")
    void parsesJsonArray() {
        when(aiService.complete(anyString())).thenReturn("[\"java\",\"spring boot\",\"docker\"]");

        List<String> result = extractor.extract("job description");

        assertThat(result).containsExactly("java", "spring boot", "docker");
    }

    @Test
    @DisplayName("normalize+dedup: 'Java', ' java ', 'JAVA' -> 'java' (distinct)")
    void normalizesAndDeduplicates() {
        when(aiService.complete(anyString()))
                .thenReturn("[\"Java\",\" java \",\"JAVA\",\"Spring\"]");

        List<String> result = extractor.extract("job description");

        assertThat(result).containsExactly("java", "spring");
    }

    @Test
    @DisplayName("порожній jobDescription -> empty List, LLM НЕ викликається")
    void emptyDescription_returnsEmpty_noLlm() {
        List<String> result = extractor.extract("   ");

        assertThat(result).isEmpty();
        verify(aiService, never()).complete(anyString());
    }

    @Test
    @DisplayName("невалідний JSON -> BusinessRuleException")
    void invalidJson_throws() {
        when(aiService.complete(anyString())).thenReturn("not a json array");

        assertThatThrownBy(() -> extractor.extract("job description"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("strip markdown fences: ```json [...] ``` парситься коректно")
    void stripsMarkdownFences() {
        when(aiService.complete(anyString()))
                .thenReturn("```json\n[\"java\", \"docker\"]\n```");

        List<String> result = extractor.extract("job description");

        assertThat(result).containsExactly("java", "docker");
    }
}
