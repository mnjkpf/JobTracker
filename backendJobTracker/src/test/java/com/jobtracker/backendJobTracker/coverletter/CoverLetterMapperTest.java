package com.jobtracker.backendJobTracker.coverletter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jobtracker.backendJobTracker.coverletter.dto.CoverLetterResponse;
import com.jobtracker.backendJobTracker.coverletter.dto.CoverLetterSummaryResponse;
import com.jobtracker.backendJobTracker.coverletter.enums.CoverLetterTone;

/**
 * Unit-тести для згенерованого MapStruct-маппера {@link CoverLetterMapperImpl}.
 * Перевіряємо повний маппінг toResponse та логіку contentPreview (100 символів + "...").
 */
class CoverLetterMapperTest {

    private final CoverLetterMapper mapper = new CoverLetterMapperImpl();

    private CoverLetter coverLetter(String content) {
        CoverLetter cl = new CoverLetter();
        cl.setId(UUID.randomUUID());
        cl.setContent(content);
        cl.setVersion(2);
        cl.setTone(CoverLetterTone.FORMAL);
        cl.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        return cl;
    }

    @Test
    @DisplayName("toResponse: мапить усі поля")
    void toResponse_mapsAllFields() {
        CoverLetter cl = coverLetter("Full cover letter content");

        CoverLetterResponse response = mapper.toResponse(cl);

        assertThat(response.getId()).isEqualTo(cl.getId());
        assertThat(response.getContent()).isEqualTo("Full cover letter content");
        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getTone()).isEqualTo(CoverLetterTone.FORMAL);
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    @DisplayName("toSummaryResponse: content > 100 символів -> перші 100 + '...'")
    void toSummaryResponse_longContent_truncatedWithEllipsis() {
        String content = "x".repeat(150);
        CoverLetter cl = coverLetter(content);

        CoverLetterSummaryResponse summary = mapper.toSummaryResponse(cl);

        assertThat(summary.getContentPreview()).hasSize(103); // 100 + "..."
        assertThat(summary.getContentPreview()).endsWith("...");
        assertThat(summary.getContentPreview()).isEqualTo("x".repeat(100) + "...");
        assertThat(summary.getVersion()).isEqualTo(2);
        assertThat(summary.getTone()).isEqualTo(CoverLetterTone.FORMAL);
    }

    @Test
    @DisplayName("toSummaryResponse: content < 100 символів -> без '...'")
    void toSummaryResponse_shortContent_noEllipsis() {
        CoverLetter cl = coverLetter("Short content");

        CoverLetterSummaryResponse summary = mapper.toSummaryResponse(cl);

        assertThat(summary.getContentPreview()).isEqualTo("Short content");
        assertThat(summary.getContentPreview()).doesNotEndWith("...");
    }

    @Test
    @DisplayName("toSummaryResponse: content == null -> contentPreview null")
    void toSummaryResponse_nullContent_nullPreview() {
        CoverLetter cl = coverLetter(null);

        CoverLetterSummaryResponse summary = mapper.toSummaryResponse(cl);

        assertThat(summary.getContentPreview()).isNull();
    }
}
