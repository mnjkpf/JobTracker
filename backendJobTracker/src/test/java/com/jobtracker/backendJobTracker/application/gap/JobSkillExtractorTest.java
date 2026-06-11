package com.jobtracker.backendJobTracker.application.gap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.cv.SkillService;
import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.SkillRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;

/**
 * Unit-тести для {@link JobSkillExtractor}.
 * <p>
 * ObjectMapper — реальний (як у Spring Boot), щоб справді парсити LLM-JSON у
 * приватні {@code ParsedJobSkills}/{@code ParsedSkillItem}. Перевіряємо парсинг,
 * дедуплікацію required↔niceToHave, replace-pattern (delete+flush перед insert),
 * race-aware embedding trigger та error-paths.
 */
@ExtendWith(MockitoExtension.class)
class JobSkillExtractorTest {

    @Mock private AiService aiService;
    @Mock private SkillService skillService;
    @Mock private SkillRepository skillRepository;
    @Mock private ApplicationSkillRepository applicationSkillRepository;

    private JobSkillExtractor extractor;

    private final UUID appId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        extractor = new JobSkillExtractor(aiService, objectMapper, skillService,
                skillRepository, applicationSkillRepository);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Application appWithDescription(String description) {
        Application app = new Application();
        app.setId(appId);
        app.setDescription(description);
        return app;
    }

    private Skill skill(String name) {
        Skill s = new Skill();
        s.setId(UUID.randomUUID());
        s.setName(name);
        return s;
    }

    // ─── parsing ────────────────────────────────────────────────────────

    @Test
    @DisplayName("LLM JSON -> required + niceToHave з правильними прапорцями required")
    void parsesRequiredAndNiceToHave() {
        Application app = appWithDescription("We need a backend engineer");
        String json = """
                {
                  "required":   [{"name":"Java","category":"LANGUAGE"}],
                  "niceToHave": [{"name":"Docker","category":"TOOL"}]
                }
                """;
        when(aiService.complete(anyString())).thenReturn(json);
        when(skillRepository.existsByName("java")).thenReturn(true);
        when(skillRepository.existsByName("docker")).thenReturn(true);
        when(skillService.findOrCreateSkill("Java", SkillCategory.LANGUAGE)).thenReturn(skill("java"));
        when(skillService.findOrCreateSkill("Docker", SkillCategory.TOOL)).thenReturn(skill("docker"));
        when(applicationSkillRepository.save(any(ApplicationSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ApplicationSkill> result = extractor.extractAndSaveSkills(app);

        assertThat(result).hasSize(2);
        ApplicationSkill java = result.stream()
                .filter(s -> s.getSkill().getName().equals("java")).findFirst().orElseThrow();
        ApplicationSkill docker = result.stream()
                .filter(s -> s.getSkill().getName().equals("docker")).findFirst().orElseThrow();
        assertThat(java.isRequired()).isTrue();
        assertThat(docker.isRequired()).isFalse();
    }

    @Test
    @DisplayName("дедуплікація: скіл у обох списках -> береться лише required")
    void deduplicates_requiredWins() {
        Application app = appWithDescription("backend");
        String json = """
                {
                  "required":   [{"name":"Java","category":"LANGUAGE"}],
                  "niceToHave": [{"name":"java","category":"LANGUAGE"}]
                }
                """;
        when(aiService.complete(anyString())).thenReturn(json);
        when(skillRepository.existsByName("java")).thenReturn(true);
        when(skillService.findOrCreateSkill("Java", SkillCategory.LANGUAGE)).thenReturn(skill("java"));
        when(applicationSkillRepository.save(any(ApplicationSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ApplicationSkill> result = extractor.extractAndSaveSkills(app);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isRequired()).isTrue();
        // findOrCreate лише раз — другий (nice) пропущено через processed-set.
        verify(skillService, Mockito.times(1)).findOrCreateSkill("Java", SkillCategory.LANGUAGE);
    }

    // ─── replace pattern ────────────────────────────────────────────────

    @Test
    @DisplayName("replace: deleteByApplicationId + flush ВИКОНУЮТЬСЯ перед save")
    void replacePattern_deleteFlushBeforeInsert() {
        Application app = appWithDescription("backend");
        when(aiService.complete(anyString()))
                .thenReturn("{\"required\":[{\"name\":\"Java\",\"category\":\"LANGUAGE\"}]}");
        when(skillRepository.existsByName("java")).thenReturn(true);
        when(skillService.findOrCreateSkill("Java", SkillCategory.LANGUAGE)).thenReturn(skill("java"));
        when(applicationSkillRepository.save(any(ApplicationSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        extractor.extractAndSaveSkills(app);

        InOrder inOrder = Mockito.inOrder(applicationSkillRepository);
        inOrder.verify(applicationSkillRepository).deleteByApplicationId(appId);
        inOrder.verify(applicationSkillRepository).flush();
        inOrder.verify(applicationSkillRepository).save(any(ApplicationSkill.class));
    }

    // ─── embedding trigger (race-aware) ─────────────────────────────────

    @Test
    @DisplayName("race-aware: existsByName(normalized) перевіряється; новий скіл -> afterCommit зареєстровано")
    void newSkill_registersEmbeddingAfterCommit() {
        Application app = appWithDescription("backend");
        when(aiService.complete(anyString()))
                .thenReturn("{\"required\":[{\"name\":\"Rust\",\"category\":\"LANGUAGE\"}]}");
        when(skillRepository.existsByName("rust")).thenReturn(false); // новий
        when(skillService.findOrCreateSkill("Rust", SkillCategory.LANGUAGE)).thenReturn(skill("rust"));
        when(applicationSkillRepository.save(any(ApplicationSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        extractor.extractAndSaveSkills(app);

        verify(skillRepository).existsByName("rust");
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    }

    @Test
    @DisplayName("існуючий скіл -> embedding НЕ реєструється (synchronization порожній)")
    void existingSkill_noEmbeddingRegistration() {
        Application app = appWithDescription("backend");
        when(aiService.complete(anyString()))
                .thenReturn("{\"required\":[{\"name\":\"Java\",\"category\":\"LANGUAGE\"}]}");
        when(skillRepository.existsByName("java")).thenReturn(true); // вже у каталозі
        when(skillService.findOrCreateSkill("Java", SkillCategory.LANGUAGE)).thenReturn(skill("java"));
        when(applicationSkillRepository.save(any(ApplicationSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        extractor.extractAndSaveSkills(app);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    // ─── error / edge paths ─────────────────────────────────────────────

    @Test
    @DisplayName("невалідний JSON -> BusinessRuleException, delete/save НЕ виконуються")
    void invalidJson_throws() {
        Application app = appWithDescription("backend");
        when(aiService.complete(anyString())).thenReturn("not a json {{{");

        assertThatThrownBy(() -> extractor.extractAndSaveSkills(app))
                .isInstanceOf(BusinessRuleException.class);

        verify(applicationSkillRepository, never()).deleteByApplicationId(any());
        verify(applicationSkillRepository, never()).save(any());
    }

    @Test
    @DisplayName("порожній jobText -> empty List, LLM НЕ викликається")
    void emptyJobText_returnsEmpty_noLlm() {
        Application app = appWithDescription(null);

        List<ApplicationSkill> result = extractor.extractAndSaveSkills(app);

        assertThat(result).isEmpty();
        verify(aiService, never()).complete(anyString());
        verify(applicationSkillRepository, never()).deleteByApplicationId(any());
    }

    @Test
    @DisplayName("порожня відповідь LLM -> BusinessRuleException")
    void emptyLlmResponse_throws() {
        Application app = appWithDescription("backend");
        when(aiService.complete(anyString())).thenReturn("   ");

        assertThatThrownBy(() -> extractor.extractAndSaveSkills(app))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("existsByName викликається з нормалізованим lowercase ключем")
    void existsByName_normalizedKey() {
        Application app = appWithDescription("backend");
        when(aiService.complete(anyString()))
                .thenReturn("{\"required\":[{\"name\":\"  JaVa  \",\"category\":\"LANGUAGE\"}]}");
        when(skillRepository.existsByName("java")).thenReturn(true);
        when(skillService.findOrCreateSkill(any(), any())).thenReturn(skill("java"));
        when(applicationSkillRepository.save(any(ApplicationSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        extractor.extractAndSaveSkills(app);

        verify(skillRepository).existsByName(eq("java"));
    }
}
