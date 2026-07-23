package com.jobtracker.backendJobTracker.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.application.gap.ApplicationSkillRepository;
import com.jobtracker.backendJobTracker.company.Company;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.ExperienceRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvSkillRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.interview.dto.parse.ParsedPrepGuide;
import com.jobtracker.backendJobTracker.interview.notes.dto.SimilarInterviewNote;
import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;
import com.jobtracker.backendJobTracker.interview.rag.InterviewRagService;

/**
 * Unit-тести {@link InterviewPrepGenerator} (7A + інтеграція RAG у 7C).
 * <p>
 * Реальний {@link ObjectMapper} (легко створити, тестуємо справжній parse),
 * решта колабораторів — моки.
 */
@ExtendWith(MockitoExtension.class)
class InterviewPrepGeneratorTest {

    @Mock private AiService aiService;
    @Mock private ApplicationSkillRepository applicationSkillRepository;
    @Mock private ExperienceRepository experienceRepository;
    @Mock private MasterCvSkillRepository masterCvSkillRepository;
    @Mock private InterviewRagService ragService;

    private InterviewPrepGenerator generator;

    private final UUID userId = UUID.randomUUID();
    private final UUID appId = UUID.randomUUID();
    private final UUID masterId = UUID.randomUUID();

    private static final String VALID_JSON = """
            {"technical":[{"question":"Explain N+1","suggestedAnswer":"use JOIN FETCH"},
                          {"question":"REST design","suggestedAnswer":"resources, verbs"}],
             "behavioral":[{"question":"Teamwork","suggestedAnswer":null}],
             "questionsToAsk":[{"question":"Onboarding?","suggestedAnswer":null}]}
            """;

    @BeforeEach
    void setUp() {
        generator = new InterviewPrepGenerator(
                aiService, new ObjectMapper(),
                applicationSkillRepository, experienceRepository,
                masterCvSkillRepository, ragService);
    }

    private Application app() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Allegro");
        Application a = new Application();
        a.setId(appId);
        a.setName("Junior Java Dev");
        a.setCompany(company);
        a.setSeniority(Seniority.JUNIOR);
        a.setWorkMode(WorkMode.HYBRID);
        a.setLocation("Warsaw");
        a.setDescription("Java Spring PostgreSQL role");
        return a;
    }

    private MasterCv master() {
        MasterCv cv = new MasterCv();
        cv.setId(masterId);
        cv.setFullName("Jan Kowalski");
        cv.setHeadline("Java Developer");
        cv.setSummary("2 years Spring Boot");
        return cv;
    }

    private MasterCvSkill mcs(String name) {
        Skill s = new Skill();
        s.setId(UUID.randomUUID());
        s.setName(name);
        MasterCvSkill m = new MasterCvSkill();
        m.setSkill(s);
        return m;
    }

    private SimilarInterviewNote simNote(String content) {
        SimilarInterviewNote n = new SimilarInterviewNote();
        n.setId(UUID.randomUUID());
        n.setApplicationId(UUID.randomUUID());
        n.setNoteType(NoteType.POST_INTERVIEW);
        n.setContent(content);
        n.setSimilarity(0.6);
        return n;
    }

    private String captureLlmPrompt() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(aiService).complete(captor.capture());
        return captor.getValue();
    }

    // ─── RAG integration ────────────────────────────────────────────────

    @Test
    @DisplayName("generate: викликає ragService.findRelevantPastNotes рівно один раз")
    void generate_callsRagOnce() {
        Application app = app();
        when(aiService.complete(anyString())).thenReturn(VALID_JSON);

        generator.generate(userId, app, master());

        verify(ragService).findRelevantPastNotes(userId, app);
    }

    @Test
    @DisplayName("RAG порожній -> prompt БЕЗ секції PAST NOTES (pastNotesContext=null)")
    void generate_ragEmpty_noPastNotesSection() {
        when(aiService.complete(anyString())).thenReturn(VALID_JSON);
        // ragService не застабано -> Mockito повертає empty list -> formatPastNotes=null

        generator.generate(userId, app(), master());

        assertThat(captureLlmPrompt()).doesNotContain("=== RELEVANT PAST INTERVIEW NOTES ===");
    }

    @Test
    @DisplayName("RAG повертає нотатки -> prompt МІСТИТЬ секцію PAST NOTES з контентом")
    void generate_ragNotes_pastNotesIncluded() {
        Application app = app();
        when(ragService.findRelevantPastNotes(userId, app))
                .thenReturn(List.of(simNote("Failed N+1, confused REQUIRES_NEW")));
        when(aiService.complete(anyString())).thenReturn(VALID_JSON);

        generator.generate(userId, app, master());

        assertThat(captureLlmPrompt())
                .contains("=== RELEVANT PAST INTERVIEW NOTES ===")
                .contains("Failed N+1, confused REQUIRES_NEW");
    }

    // ─── JSON parsing ───────────────────────────────────────────────────

    @Test
    @DisplayName("валідний JSON -> ParsedPrepGuide з правильними списками")
    void generate_validJson() {
        when(aiService.complete(anyString())).thenReturn(VALID_JSON);

        ParsedPrepGuide guide = generator.generate(userId, app(), master());

        assertThat(guide.getTechnical()).hasSize(2);
        assertThat(guide.getBehavioral()).hasSize(1);
        assertThat(guide.getQuestionsToAsk()).hasSize(1);
        assertThat(guide.getTechnical().get(0).getQuestion()).isEqualTo("Explain N+1");
        assertThat(guide.getBehavioral().get(0).getSuggestedAnswer()).isNull();
    }

    @Test
    @DisplayName("порожня LLM-відповідь -> BusinessRuleException")
    void generate_emptyResponse() {
        when(aiService.complete(anyString())).thenReturn("   ");
        assertThatThrownBy(() -> generator.generate(userId, app(), master()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("невалідний JSON -> BusinessRuleException")
    void generate_invalidJson() {
        when(aiService.complete(anyString())).thenReturn("this is not json at all");
        assertThatThrownBy(() -> generator.generate(userId, app(), master()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("markdown ```json ... ``` fences зрізаються перед parse")
    void generate_markdownFencesStripped() {
        when(aiService.complete(anyString())).thenReturn("```json\n" + VALID_JSON + "\n```");

        ParsedPrepGuide guide = generator.generate(userId, app(), master());

        assertThat(guide.getTechnical()).hasSize(2);
    }

    // ─── context building ───────────────────────────────────────────────

    @Test
    @DisplayName("buildJobContext: company.getName() (не toString), у prompt 'Company: Allegro'")
    void generate_jobContextCompanyName() {
        when(aiService.complete(anyString())).thenReturn(VALID_JSON);

        generator.generate(userId, app(), master());

        assertThat(captureLlmPrompt()).contains("Company: Allegro");
    }

    @Test
    @DisplayName("buildMasterContext: skills join через ', ' (не List.toString())")
    void generate_masterContextSkillsJoin() {
        when(masterCvSkillRepository.findByMasterCvId(masterId))
                .thenReturn(List.of(mcs("java"), mcs("spring")));
        when(aiService.complete(anyString())).thenReturn(VALID_JSON);

        generator.generate(userId, app(), master());

        assertThat(captureLlmPrompt()).contains("Skills: java, spring");
    }
}
