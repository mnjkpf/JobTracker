package com.jobtracker.backendJobTracker.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
import com.jobtracker.backendJobTracker.cv.models.Experience;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.EducationRepository;
import com.jobtracker.backendJobTracker.cv.repo.ExperienceRepository;
import com.jobtracker.backendJobTracker.cv.repo.LanguageRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvSkillRepository;
import com.jobtracker.backendJobTracker.cv.repo.ProjectRepository;
import com.jobtracker.backendJobTracker.cv.repo.SkillRepository;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

/**
 * Unit-тести для {@link CvExtractionService}.
 * <p>
 * ObjectMapper — реальний (зібраний як у Spring Boot через
 * {@link Jackson2ObjectMapperBuilder}), щоб справді парсити JSON від LLM,
 * включно з вкладеними {@code @Builder} DTO. Решта залежностей — моки.
 * <p>
 * Для перевірки {@code afterCommit}-реєстрації вручну активуємо
 * transaction-synchronization (у unit-тесті реальної транзакції немає).
 */
@ExtendWith(MockitoExtension.class)
class CvExtractionServiceTest {

    @Mock private CvFileValidation fileValidation;
    @Mock private CvFileParser fileParser;
    @Mock private AiService aiService;
    @Mock private MasterCvRepository masterCvRepository;
    @Mock private ExperienceRepository experienceRepository;
    @Mock private EducationRepository educationRepository;
    @Mock private MasterCvSkillRepository masterCvSkillRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private LanguageRepository languageRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private SkillService skillService;
    @Mock private UserRepository userRepository;
    @Mock private MultipartFile file;

    private CvExtractionService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // ObjectMapper з усіма well-known модулями (parameter-names, jsr310) — як у Spring Boot.
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        service = new CvExtractionService(
                fileValidation, fileParser, aiService, objectMapper,
                masterCvRepository, experienceRepository, educationRepository,
                masterCvSkillRepository, projectRepository, languageRepository,
                skillRepository, skillService, userRepository);
    }

    @AfterEach
    void tearDown() {
        // Прибираємо synchronization, якщо тест його активував.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ─── happy path: file -> text -> LLM -> ParsedCv -> entity graph ────

    @Test
    @DisplayName("extractAndReplace: file -> text -> LLM JSON -> зберігає Experience+Skill, реєструє afterCommit")
    void extractAndReplace_buildsEntityGraph_andRegistersEmbedding() {
        String json = """
                {
                  "language": "EN",
                  "fullName": "Jane Dev",
                  "email": "jane@example.com",
                  "summary": "Backend developer",
                  "experiences": [
                    {"company":"Acme","position":"Backend Engineer",
                     "startDate":"2021-01-01","endDate":"2023-06-01",
                     "location":"Remote","description":"Built APIs"}
                  ],
                  "skills": [
                    {"name":"Java","category":"LANGUAGE","level":"ADVANCED"}
                  ]
                }
                """;
        when(fileParser.extractText(file)).thenReturn("raw cv text");
        when(aiService.complete(anyString())).thenReturn(json);
        when(masterCvRepository.findByUserId(userId)).thenReturn(java.util.Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(new User());
        when(masterCvRepository.save(any(MasterCv.class))).thenAnswer(inv -> {
            MasterCv cv = inv.getArgument(0);
            cv.setId(UUID.randomUUID());
            return cv;
        });
        lenient().when(file.getOriginalFilename()).thenReturn("cv.pdf");

        Skill javaSkill = new Skill();
        javaSkill.setId(UUID.randomUUID());
        javaSkill.setName("java");
        when(skillRepository.existsByName("java")).thenReturn(false); // новий -> afterCommit
        when(skillService.findOrCreateSkill("Java", SkillCategory.LANGUAGE)).thenReturn(javaSkill);
        when(experienceRepository.save(any(Experience.class))).thenAnswer(inv -> inv.getArgument(0));
        when(masterCvSkillRepository.save(any(MasterCvSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        // Активуємо synchronization, щоб registerSynchronization не кинув IllegalStateException.
        TransactionSynchronizationManager.initSynchronization();

        MasterCv result = service.extractAndReplace(userId, file);

        assertThat(result.getFullName()).isEqualTo("Jane Dev");

        ArgumentCaptor<Experience> expCaptor = ArgumentCaptor.forClass(Experience.class);
        verify(experienceRepository).save(expCaptor.capture());
        Experience exp = expCaptor.getValue();
        assertThat(exp.getPosition()).isEqualTo("Backend Engineer");
        assertThat(exp.getCompany()).isEqualTo("Acme");
        assertThat(exp.getStartDate()).isEqualTo(LocalDate.of(2021, 1, 1));
        assertThat(exp.getMasterCv()).isSameAs(result);

        verify(masterCvSkillRepository).save(any(MasterCvSkill.class));

        // afterCommit для embedding зареєстровано (новий скіл).
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
    }

    @Test
    @DisplayName("extractAndReplace: REPLACE — старий CV видаляється і flush ДО save нового")
    void extractAndReplace_deletesOldBeforeInsert() {
        when(fileParser.extractText(file)).thenReturn("raw cv text");
        when(aiService.complete(anyString())).thenReturn("{\"language\":\"EN\",\"fullName\":\"Jane\"}");
        MasterCv oldCv = new MasterCv();
        oldCv.setId(UUID.randomUUID());
        when(masterCvRepository.findByUserId(userId)).thenReturn(java.util.Optional.of(oldCv));
        when(userRepository.getReferenceById(userId)).thenReturn(new User());
        when(masterCvRepository.save(any(MasterCv.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(file.getOriginalFilename()).thenReturn("cv.pdf");

        service.extractAndReplace(userId, file);

        // Порядок: delete(old) -> flush() -> save(new). Інакше UNIQUE(user_id) впаде.
        InOrder inOrder = Mockito.inOrder(masterCvRepository);
        inOrder.verify(masterCvRepository).delete(oldCv);
        inOrder.verify(masterCvRepository).flush();
        inOrder.verify(masterCvRepository).save(any(MasterCv.class));
    }

    // ─── error paths ────────────────────────────────────────────────────

    @Test
    @DisplayName("preview: невалідний JSON від LLM -> CvExtractionException")
    void preview_invalidJson_throws() {
        when(fileParser.extractText(file)).thenReturn("raw cv text");
        when(aiService.complete(anyString())).thenReturn("this is not json {{{");

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(CvExtractionException.class);
    }

    @Test
    @DisplayName("preview: порожня відповідь LLM -> CvExtractionException")
    void preview_emptyLlmResponse_throws() {
        when(fileParser.extractText(file)).thenReturn("raw cv text");
        when(aiService.complete(anyString())).thenReturn("   ");

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(CvExtractionException.class);
    }

    @Test
    @DisplayName("preview: невалідний/порожній файл (validation throws) -> exception, LLM не викликається")
    void preview_invalidFile_throwsBeforeLlm() {
        doThrow(new CvExtractionException("No file uploaded or file is empty.", null))
                .when(fileValidation).validate(file);

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(CvExtractionException.class);

        verify(fileParser, never()).extractText(any());
        verify(aiService, never()).complete(anyString());
    }
}
