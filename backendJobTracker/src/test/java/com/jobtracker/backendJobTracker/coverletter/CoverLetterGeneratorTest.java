package com.jobtracker.backendJobTracker.coverletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.company.Company;
import com.jobtracker.backendJobTracker.coverletter.enums.CoverLetterTone;
import com.jobtracker.backendJobTracker.cv.models.Education;
import com.jobtracker.backendJobTracker.cv.models.Experience;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.EducationRepository;
import com.jobtracker.backendJobTracker.cv.repo.ExperienceRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvSkillRepository;

/**
 * Unit-тести для {@link CoverLetterGenerator}.
 * <p>
 * Перехоплюємо prompt, переданий у {@link AiService#complete}, і перевіряємо
 * що CV-контекст містить experiences/skills/education (не лише ім'я+summary),
 * tone передається як {@code enum.name()}, а company — через {@code getName()}.
 */
@ExtendWith(MockitoExtension.class)
class CoverLetterGeneratorTest {

    @Mock private AiService aiService;
    @Mock private ExperienceRepository experienceRepository;
    @Mock private EducationRepository educationRepository;
    @Mock private MasterCvSkillRepository masterCvSkillRepository;

    @InjectMocks private CoverLetterGenerator generator;

    private final UUID cvId = UUID.randomUUID();

    private MasterCv cv() {
        MasterCv cv = new MasterCv();
        cv.setId(cvId);
        cv.setFullName("Jane Dev");
        cv.setHeadline("Backend Engineer");
        cv.setSummary("Experienced backend developer");
        return cv;
    }

    private Application app() {
        Company company = new Company();
        company.setName("Acme Corp");
        Application app = new Application();
        app.setName("Senior Backend Engineer");
        app.setCompany(company);
        app.setSeniority(Seniority.MID);
        app.setWorkMode(WorkMode.REMOTE);
        app.setLocation("Warsaw");
        app.setDescription("We use Spring Boot and Postgres");
        return app;
    }

    @Test
    @DisplayName("generate: prompt містить experiences/skills/education, tone як name(), company.getName()")
    void generate_buildsRichContext() {
        Experience exp = new Experience();
        exp.setPosition("Java Developer");
        exp.setCompany("PrevCorp");
        exp.setStartDate(LocalDate.of(2020, 1, 1));
        exp.setDescription("Built REST APIs");

        Skill skill = new Skill();
        skill.setName("spring boot");
        MasterCvSkill mcs = new MasterCvSkill();
        mcs.setSkill(skill);

        Education edu = new Education();
        edu.setDegree("Bachelor");
        edu.setFieldOfStudy("Computer Science");
        edu.setInstitution("WSB University");

        when(experienceRepository.findByMasterCvIdOrderByStartDateDesc(cvId)).thenReturn(List.of(exp));
        when(masterCvSkillRepository.findByMasterCvId(cvId)).thenReturn(List.of(mcs));
        when(educationRepository.findByMasterCvIdOrderByStartDateDesc(cvId)).thenReturn(List.of(edu));
        when(aiService.complete(anyString())).thenReturn("LETTER");

        String result = generator.generate(cv(), app(), CoverLetterTone.FORMAL, "Mention remote");

        assertThat(result).isEqualTo("LETTER");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).complete(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        // Experience — не лише name+summary, а реальні позиції/компанії/описи.
        assertThat(prompt).contains("Java Developer");
        assertThat(prompt).contains("PrevCorp");
        assertThat(prompt).contains("Built REST APIs");
        // Skills.
        assertThat(prompt).contains("spring boot");
        // Education.
        assertThat(prompt).contains("Bachelor");
        assertThat(prompt).contains("Computer Science");
        assertThat(prompt).contains("WSB University");
        // tone як enum.name().
        assertThat(prompt).contains("TONE: FORMAL");
        // company через getName(), а не Object.toString().
        assertThat(prompt).contains("Company: Acme Corp");
        assertThat(prompt).doesNotContain("Company@");
        // extra instructions проброшені.
        assertThat(prompt).contains("Mention remote");
    }

    @Test
    @DisplayName("refine: RefinePrompt отримує існуючий текст + instruction")
    void refine_passesCurrentLetterAndInstruction() {
        when(aiService.complete(anyString())).thenReturn("REFINED");

        String result = generator.refine("My current cover letter body", "make it more enthusiastic");

        assertThat(result).isEqualTo("REFINED");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).complete(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("My current cover letter body");
        assertThat(prompt).contains("make it more enthusiastic");
    }
}
