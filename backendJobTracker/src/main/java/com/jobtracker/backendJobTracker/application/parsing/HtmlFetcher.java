package com.jobtracker.backendJobTracker.application.parsing;
import java.io.IOException;
import java.net.SocketTimeoutException;
 
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Обгортка над JSoup для завантаження HTML. Централізує timeout, user-agent,
 * error handling. Парсери отримують вже завантажений рядок (легше тестувати).
 * <p>
 * УВАГА: JSoup НЕ виконує JavaScript. Для pure CSR SPA HTML буде майже порожній —
 * треба перевірити емпірично для кожного board.
 */
@Component
public class HtmlFetcher {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlFetcher.class);
 
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
 
    private static final int TIMEOUT_MS = 10_000;
 
    public String fetch(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();
 
            String html = doc.html();
            LOGGER.debug("Fetched {} chars from {}", html.length(), url);
            return html;
 
        } catch (SocketTimeoutException e) {
            LOGGER.warn("Timeout fetching {}: {}", url, e.getMessage());
            throw new ParsingException("Job posting site timed out. Try manual paste.", e);
        } catch (IOException e) {
            LOGGER.warn("Failed to fetch {}: {}", url, e.getMessage());
            throw new ParsingException("Could not load job posting. Try manual paste.", e);
        }
    }
}

