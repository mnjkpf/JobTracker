package com.jobtracker.backendJobTracker.cv.tailored;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.company.Company;
import com.jobtracker.backendJobTracker.cv.models.Experience;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.repo.EducationRepository;
import com.jobtracker.backendJobTracker.cv.repo.ExperienceRepository;
import com.jobtracker.backendJobTracker.cv.repo.LanguageRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvSkillRepository;
import com.jobtracker.backendJobTracker.cv.repo.ProjectRepository;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredCv;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;

/**
 * Unit-тести для {@link TailoredCvGenerator}.
 * <p>
 * Перевіряємо: приймає {@link MasterCv} (не TailoredCv), master-контекст містить
 * УСІ experiences (без limit), повертає {@link ParsedTailoredCv} (не String),
 * company через {@code getName()}, помилка парсингу -> BusinessRuleException.
 * ObjectMapper — реальний (як у Spring Boot).
 */
@ExtendWith(MockitoExtension.class)
class TailoredCvGeneratorTest {

    @Mock private AiService aiService;
    @Mock private ExperienceRepository experienceRepository;
    @Mock private EducationRepository educationRepository;
    @Mock private MasterCvSkillRepository masterCvSkillRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private LanguageRepository languageRepository;

    private TailoredCvGenerator generator;

    private final UUID cvId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        generator = new TailoredCvGenerator(aiService, objectMapper, experienceRepository,
                educationRepository, masterCvSkillRepository, projectRepository, languageRepository);
    }

    private MasterCv master() {
        MasterCv cv = new MasterCv();
        cv.setId(cvId);
        cv.setFullName("Jane Dev");
        return cv;
    }

    private Application app() {
        Company company = new Company();
        company.setName("Acme Corp");
        Application app = new Application();
        app.setName("Backend Engineer");
        app.setCompany(company);
        app.setDescription("We use Spring Boot");
        return app;
    }

    @Test
    @DisplayName("buildMasterContext містить УСІ experiences (без limit) + повертає ParsedTailoredCv")
    void includesAllExperiences_returnsParsedTailoredCv() {
        // 7 досвідів — більше за ліміт CoverLetterGenerator (5). Tailored не лімітує.
        List<Experience> experiences = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            Experience e = new Experience();
            e.setPosition("Position" + i);
            e.setCompany("Company" + i);
            e.setStartDate(LocalDate.of(2010 + i, 1, 1));
            experiences.add(e);
        }
        when(experienceRepository.findByMasterCvIdOrderByStartDateDesc(cvId)).thenReturn(experiences);
        when(aiService.complete(anyString()))
                .thenReturn("{\"fullName\":\"Jane Dev\",\"headline\":\"Engineer\"}");

        ParsedTailoredCv result = generator.generate(master(), app(), "emphasis");

        // Повертає ParsedTailoredCv (не String).
        assertThat(result).isInstanceOf(ParsedTailoredCv.class);
        assertThat(result.getFullName()).isEqualTo("Jane Dev");
        assertThat(result.getHeadline()).isEqualTo("Engineer");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).complete(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        // Усі 7 позицій присутні — жодна не відрізана лімітом.
        for (int i = 1; i <= 7; i++) {
            assertThat(prompt).contains("Position" + i);
        }
    }

    @Test
    @DisplayName("company.getName() використовується у job-контексті (не Object.toString())")
    void usesCompanyGetName() {
        when(aiService.complete(anyString())).thenReturn("{}");

        generator.generate(master(), app(), null);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).complete(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("Company: Acme Corp");
        assertThat(prompt).doesNotContain("Company@");
    }

    @Test
    @DisplayName("невалідний JSON -> BusinessRuleException")
    void invalidJson_throws() {
        when(aiService.complete(anyString())).thenReturn("not a valid json {{{");

        assertThatThrownBy(() -> generator.generate(master(), app(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("порожня відповідь LLM -> BusinessRuleException")
    void emptyResponse_throws() {
        when(aiService.complete(anyString())).thenReturn("   ");

        assertThatThrownBy(() -> generator.generate(master(), app(), null))
                .isInstanceOf(BusinessRuleException.class);
    }
}
