package com.jobtracker.backendJobTracker.application.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.jobtracker.backendJobTracker.application.enums.SourceBoard;

/**
 * Unit-тести для {@link JobBoardDetector} — детекція job board за доменом URL.
 */
class JobBoardDetectorTest {

    private final JobBoardDetector detector = new JobBoardDetector();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "https://nofluffjobs.com/pl/job/java-dev,         NOFLUFFJOBS",
            "https://justjoin.it/offers/junior-java,           JUSTJOINIT",
            "https://www.pracuj.pl/praca/junior-java,          PRACUJ",
            "https://pl.linkedin.com/jobs/view/123,            LINKEDIN",
            "https://example.com/careers/123,                 OTHER",
            "https://boards.greenhouse.io/acme/jobs/1,         OTHER"
    })
    @DisplayName("Відомі домени мапляться на свій SourceBoard, невідомі -> OTHER")
    void detect_knownDomains(String url, SourceBoard expected) {
        assertThat(detector.detect(url)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "www-префікс: {0}")
    @ValueSource(strings = {
            "https://www.nofluffjobs.com/job/x",
            "https://WWW.NoFluffJobs.com/job/x"
    })
    @DisplayName("www. префікс і регістр ігноруються")
    void detect_ignoresWwwAndCase(String url) {
        assertThat(detector.detect(url)).isEqualTo(SourceBoard.NOFLUFFJOBS);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-a-valid-url", "::::", "/relative/path"})
    @DisplayName("null / empty / невалідний URL -> OTHER (fallback)")
    void detect_invalidUrlFallsBackToOther(String url) {
        assertThat(detector.detect(url)).isEqualTo(SourceBoard.OTHER);
    }

    @Test
    @DisplayName("Субдомен містить відомий хост -> правильний board")
    void detect_subdomainContainsHost() {
        assertThat(detector.detect("https://career.pracuj.pl/oferta/123"))
                .isEqualTo(SourceBoard.PRACUJ);
    }
}
