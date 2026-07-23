package com.jobtracker.backendJobTracker.application.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.application.parsing.dto.ParsedJobPosting;

/**
 * Unit-тест для {@link PracujParser} на збереженій HTML-фікстурі (offline, без мережі).
 * Перевіряє витягання position/company/salary/contractType + нормалізацію
 * польських діакритиків і неразривних пробілів.
 */
class PracujParserTest {

    private static final String URL = "https://www.pracuj.pl/praca/junior-java-developer-krakow,oferta,1000123";

    private final PracujParser parser = new PracujParser();
    private String html;

    @BeforeEach
    void loadFixture() throws IOException {
        ClassPathResource resource = new ClassPathResource("fixtures/pracuj-junior-spring.html");
        html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("supportedBoard() = PRACUJ")
    void supportedBoard() {
        assertThat(parser.supportedBoard()).isEqualTo(SourceBoard.PRACUJ);
    }

    @Test
    @DisplayName("Парсить position та company з data-test атрибутів")
    void parsesPositionAndCompany() {
        ParsedJobPosting result = parser.parse(html, URL).orElseThrow();

        assertThat(result.getPosition()).isEqualTo("Junior Java Developer (Spring)");
        assertThat(result.getCompanyName()).isEqualTo("Comarch S.A.");
        assertThat(result.hasMinimumData()).isTrue();
        assertThat(result.isParsedByLlm()).isFalse();
    }

    @Test
    @DisplayName("Парсить salary range з неразривними пробілами -> min/max/currency")
    void parsesSalary() {
        ParsedJobPosting result = parser.parse(html, URL).orElseThrow();

        assertThat(result.getSalaryMin()).isEqualTo(8000);
        assertThat(result.getSalaryMax()).isEqualTo(12000);
        assertThat(result.getSalaryCurrency()).isEqualTo("PLN");
    }

    @Test
    @DisplayName("Нормалізує польські діакритики у contract/work-mode/seniority")
    void parsesEnumsWithDiacritics() {
        ParsedJobPosting result = parser.parse(html, URL).orElseThrow();

        // "Umowa o pracę" -> normalize -> "umowa o prace" -> UOP
        assertThat(result.getContractType()).isEqualTo(ContractType.UOP);
        // "Praca hybrydowa" -> "hybryd" -> HYBRID
        assertThat(result.getWorkMode()).isEqualTo(WorkMode.HYBRID);
        // "Junior" -> JUNIOR
        assertThat(result.getSeniority()).isEqualTo(Seniority.JUNIOR);
        // "Kraków, małopolskie" location збережено
        assertThat(result.getLocation()).contains("Krak");
    }

    @Test
    @DisplayName("Порожній/невалідний HTML -> Optional.empty (сигнал для fallback)")
    void emptyHtmlReturnsEmpty() {
        Optional<ParsedJobPosting> result = parser.parse("<html><body>nic tu nie ma</body></html>", URL);

        assertThat(result).isEmpty();
    }
}
