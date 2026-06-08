package com.jobtracker.backendJobTracker.application.parsing;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.parsing.dto.ParsedJobPosting;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobPostingExtractionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobPostingExtractionService.class);

    private final GenericLlmParser genericLlmParser;
    private final JustJoinItParser justJoinItParser;
    private final PracujParser pracujParser;
    private final NoFluffJobsParser noFluffJobsParser;
    private final JobBoardDetector jobBoardDetector;
    private final HtmlFetcher htmlFetcher;

    /**
     * Main orchestration flow:
     * 1. load HTML,
     * 2. detect job board,
     * 3. try deterministic board parser,
     * 4. fallback to LLM when deterministic parsing cannot extract minimum data.
     */
    public Optional<ParsedJobPosting> parse(String url) {
        if (url == null || url.isBlank()) {
            throw new ParsingException("Job posting URL is required.");
        }

        String html = fetchHtml(url);
        SourceBoard board = detectBoard(url);

        Optional<ParsedJobPosting> parsed = parseWithSpecificParser(board, html, url);
        if (parsed.isPresent() && parsed.get().hasMinimumData()) {
            return parsed;
        }

        LOGGER.info("Falling back to LLM parser for board {} and url {}", board, url);
        return parseWithFallback(html, url);
    }

    /**
     * Uses only deterministic parsers for known boards.
     * Unknown boards return empty here so the caller can decide about fallback.
     */
    private Optional<ParsedJobPosting> parseWithSpecificParser(SourceBoard board, String html, String url) {
        return switch (board) {
            case JUSTJOINIT -> justJoinItParser.parse(html, url);
            case NOFLUFFJOBS -> noFluffJobsParser.parse(html, url);
            case PRACUJ -> pracujParser.parse(html, url);
            default -> Optional.empty();
        };
    }

    /**
     * Generic parser is intentionally separated from specific parsers.
     * This prevents accidental use of LLM as a board-specific parser.
     */
    private Optional<ParsedJobPosting> parseWithFallback(String html, String url) {
        Optional<ParsedJobPosting> parsed = genericLlmParser.parse(html, url);
        if (parsed.isPresent() && parsed.get().hasMinimumData()) {
            return parsed;
        }

        LOGGER.warn("Could not extract minimum job posting data from {}", url);
        return Optional.empty();
    }

    private String fetchHtml(String url) {
        return htmlFetcher.fetch(url);
    }

    private SourceBoard detectBoard(String url) {
        return jobBoardDetector.detect(url);
    }
}
