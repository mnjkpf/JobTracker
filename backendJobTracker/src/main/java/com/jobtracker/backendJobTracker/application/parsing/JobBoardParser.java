package com.jobtracker.backendJobTracker.application.parsing;

import java.util.Optional;

import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.parsing.dto.ParsedJobPosting;

/**
 * Контракт парсера job board. Кожен board має свою імплементацію.
 * GenericLlmParser — універсальний fallback.
 * <p>
 * Повертає Optional — empty означає "не зміг розпарсити" (сигнал для fallback),
 * НЕ помилка. Exception лише при технічних збоях (network timeout у HtmlFetcher).
 */
public interface JobBoardParser {
 
    /** Який board цей парсер обслуговує. */
    SourceBoard supportedBoard();
 
    /**
     * @param html сирий HTML (вже завантажений HtmlFetcher)
     * @param url  оригінальний URL (деякі парсери витягують дані з slug/id)
     * @return розпарсені дані, або empty якщо не вдалось
     */
    Optional<ParsedJobPosting> parse(String html, String url);
}

