package com.jobtracker.backendJobTracker.cv.tailored.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtracker.backendJobTracker.cv.enums.LanguageLevel;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredEducationResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredExperienceResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredLanguageResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredProjectResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredSkillResponse;

/**
 * Unit-тести для {@link CvDocxTemplate} — мокаємо {@link DocxSectionBuilder}
 * і перевіряємо ЯКІ секції і в якому порядку викликаються (без реального POI).
 */
@ExtendWith(MockitoExtension.class)
class CvDocxTemplateTest {

    @Mock private DocxSectionBuilder builder;
    private CvDocxTemplate template;

    private final XWPFDocument doc = mock(XWPFDocument.class);

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        template = new CvDocxTemplate(builder);
    }

    private TailoredExperienceResponse experience(LocalDate start, LocalDate end, String location) {
        TailoredExperienceResponse e = new TailoredExperienceResponse();
        e.setPosition("Engineer");
        e.setCompany("Acme");
        e.setStartDate(start);
        e.setEndDate(end);
        e.setLocation(location);
        return e;
    }

    private TailoredCvResponse fullCv() {
        TailoredCvResponse cv = new TailoredCvResponse();
        cv.setFullName("Jane Dev");
        cv.setHeadline("Engineer");
        cv.setSummary("Summary text");
        cv.setExperiences(List.of(experience(LocalDate.of(2022, 1, 1), null, null)));

        TailoredSkillResponse s = new TailoredSkillResponse();
        s.setName("java");
        cv.setSkills(List.of(s));

        TailoredProjectResponse p = new TailoredProjectResponse();
        p.setName("JobTracker");
        cv.setProjects(List.of(p));

        TailoredEducationResponse edu = new TailoredEducationResponse();
        edu.setDegree("Bachelor");
        edu.setFieldOfStudy("CS");
        edu.setInstitution("WSB");
        cv.setEducations(List.of(edu));

        TailoredLanguageResponse l = new TailoredLanguageResponse();
        l.setName("English");
        l.setLevel(LanguageLevel.C1);
        cv.setLanguages(List.of(l));
        return cv;
    }

    @Test
    @DisplayName("build викликає секції у правильному порядку")
    void build_sectionsInOrder() {
        template.build(doc, fullCv());

        InOrder inOrder = Mockito.inOrder(builder);
        inOrder.verify(builder).sectionHeading(any(), eq("Summary"));
        inOrder.verify(builder).sectionHeading(any(), eq("Experience"));
        inOrder.verify(builder).sectionHeading(any(), eq("Skills"));
        inOrder.verify(builder).sectionHeading(any(), eq("Projects"));
        inOrder.verify(builder).sectionHeading(any(), eq("Education"));
        inOrder.verify(builder).sectionHeading(any(), eq("Languages"));
    }

    @Test
    @DisplayName("порожня секція (empty list) -> sectionHeading НЕ викликається")
    void build_emptySection_noHeading() {
        TailoredCvResponse cv = new TailoredCvResponse();
        cv.setFullName("Jane");
        cv.setSummary("Summary text");
        cv.setExperiences(List.of()); // порожньо -> без Experience heading

        template.build(doc, cv);

        verify(builder).sectionHeading(any(), eq("Summary"));
        verify(builder, never()).sectionHeading(any(), eq("Experience"));
        verify(builder, never()).sectionHeading(any(), eq("Skills"));
    }

    @Test
    @DisplayName("formatPeriod: endDate null -> 'Jan 2022 – Present' у metaLine")
    void formatPeriod_nullEnd_present() {
        TailoredCvResponse cv = new TailoredCvResponse();
        cv.setFullName("Jane");
        cv.setExperiences(List.of(experience(LocalDate.of(2022, 1, 1), null, null)));

        template.build(doc, cv);

        ArgumentCaptor<String> metaCaptor = ArgumentCaptor.forClass(String.class);
        verify(builder).metaLine(any(), metaCaptor.capture());
        // Місяць форматується за дефолтною локаллю системи ("MMM yyyy"), тож не
        // фіксуємо "Jan" — перевіряємо саму логіку: рік + endDate null -> "Present".
        assertThat(metaCaptor.getValue()).contains("2022");
        assertThat(metaCaptor.getValue()).endsWith("Present");
    }

    @Test
    @DisplayName("buildContactLine: лише непорожні частини, через '  |  '")
    void buildContactLine_onlyNonBlankParts() {
        TailoredCvResponse cv = new TailoredCvResponse();
        cv.setFullName("Jane");
        cv.setEmail("jane@example.com");
        cv.setGithubUrl("github.com/jane");
        // phone, linkedInUrl лишаються null

        template.build(doc, cv);

        ArgumentCaptor<String> contactCaptor = ArgumentCaptor.forClass(String.class);
        verify(builder).contactLine(any(), contactCaptor.capture());
        String contact = contactCaptor.getValue();
        assertThat(contact).isEqualTo("jane@example.com  |  github.com/jane");
        assertThat(contact).doesNotContain("null");
    }
}
