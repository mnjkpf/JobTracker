package com.jobtracker.backendJobTracker.cv.tailored.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jobtracker.backendJobTracker.cv.enums.LanguageLevel;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredExperienceResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredLanguageResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredSkillResponse;

/**
 * Тести експорту DOCX — лише Apache POI, без БД і Spring контексту.
 * Збираємо реальний {@link DocxExporter} з реальних {@link CvDocxTemplate} +
 * {@link DocxSectionBuilder}, генеруємо байти, відкриваємо назад і читаємо текст.
 */
class DocxExporterTest {

    private final DocxExporter exporter =
            new DocxExporter(new CvDocxTemplate(new DocxSectionBuilder()));

    private TailoredCvResponse fullCv() {
        TailoredCvResponse cv = new TailoredCvResponse();
        cv.setFullName("Paweł Kowalski");
        cv.setHeadline("Senior Java Developer");
        cv.setEmail("pawel@example.com");
        cv.setPhone("+48 123 456 789");
        cv.setSummary("Doświadczony programista: ą, ć, ś, ż, ó.");

        TailoredExperienceResponse exp = new TailoredExperienceResponse();
        exp.setPosition("Backend Engineer");
        exp.setCompany("Acme");
        exp.setStartDate(LocalDate.of(2021, 1, 1));
        exp.setDescription("Built REST APIs\nScaled services");
        cv.setExperiences(List.of(exp));

        TailoredSkillResponse skill = new TailoredSkillResponse();
        skill.setName("spring boot");
        cv.setSkills(List.of(skill));

        TailoredLanguageResponse lang = new TailoredLanguageResponse();
        lang.setName("Polish");
        lang.setLevel(LanguageLevel.NATIVE);
        cv.setLanguages(List.of(lang));

        return cv;
    }

    /** Читає весь текст параграфів згенерованого DOCX. */
    private String extractText(byte[] bytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    @Test
    @DisplayName("export повертає non-empty byte[], який знову відкривається як XWPFDocument")
    void export_returnsValidReopenableDocx() throws Exception {
        byte[] bytes = exporter.export(fullCv());

        assertThat(bytes).isNotEmpty();
        assertThatCode(() -> {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                assertThat(doc).isNotNull();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("документ містить fullName десь у тексті")
    void document_containsFullName() throws Exception {
        String text = extractText(exporter.export(fullCv()));
        assertThat(text).contains("Paweł Kowalski");
    }

    @Test
    @DisplayName("польські символи (ą, ć, ś) зберігаються коректно")
    void document_preservesPolishCharacters() throws Exception {
        String text = extractText(exporter.export(fullCv()));
        assertThat(text).contains("ą");
        assertThat(text).contains("ć");
        assertThat(text).contains("ś");
        assertThat(text).contains("Paweł");
    }

    @Test
    @DisplayName("порожні секції (null колекції) не падають -> валідний DOCX")
    void export_nullCollections_doesNotCrash() {
        TailoredCvResponse cv = new TailoredCvResponse();
        cv.setFullName("Jane Doe");
        // experiences/skills/... лишаються null

        assertThatCode(() -> {
            byte[] bytes = exporter.export(cv);
            assertThat(bytes).isNotEmpty();
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                assertThat(doc).isNotNull();
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CV лише з hero block (без колекцій) -> валідний DOCX з іменем")
    void export_heroOnly_validDocx() throws Exception {
        TailoredCvResponse cv = new TailoredCvResponse();
        cv.setFullName("Hero Only");
        cv.setHeadline("Developer");
        cv.setEmail("hero@example.com");

        byte[] bytes = exporter.export(cv);
        assertThat(bytes).isNotEmpty();
        String text = extractText(bytes);
        assertThat(text).contains("Hero Only");
    }
}
