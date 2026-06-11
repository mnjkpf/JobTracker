package com.jobtracker.backendJobTracker.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.SkillRepository;

/**
 * Unit-тести для {@link SkillService} з Mockito.
 * <p>
 * Перевіряємо: findOrCreate (lookup перед create, нормалізація до lowercase),
 * генерацію embedding (EmbeddingModel + нативний JdbcTemplate UPDATE),
 * та безпечний skip коли скіл зник між створенням і async-задачею.
 */
@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock private SkillRepository skillRepository;
    @Mock private EmbeddingModel embeddingModel;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private SkillService service;

    // ─── findOrCreateSkill ──────────────────────────────────────────────

    @Test
    @DisplayName("findOrCreateSkill: скіл існує -> повертає існуючий, save НЕ викликається")
    void findOrCreate_existing_returnsExisting_noSave() {
        Skill existing = new Skill();
        existing.setId(UUID.randomUUID());
        existing.setName("java");
        // Lookup за нормалізованим (lowercase) ім'ям.
        when(skillRepository.findByName("java")).thenReturn(Optional.of(existing));

        Skill result = service.findOrCreateSkill("Java", SkillCategory.LANGUAGE);

        assertThat(result).isSameAs(existing);
        verify(skillRepository, never()).save(any());
    }

    @Test
    @DisplayName("findOrCreateSkill: новий скіл -> save викликається, name збережено lowercase")
    void findOrCreate_new_savesWithLowercaseName() {
        when(skillRepository.findByName("java")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        Skill result = service.findOrCreateSkill("Java", SkillCategory.LANGUAGE);

        ArgumentCaptor<Skill> captor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(captor.capture());
        Skill saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("java");                  // нормалізовано
        assertThat(saved.getCategory()).isEqualTo(SkillCategory.LANGUAGE);
        assertThat(result.getName()).isEqualTo("java");

        // Race-safety: спочатку lookup (existsByName/findByName), лише потім create.
        InOrder inOrder = Mockito.inOrder(skillRepository);
        inOrder.verify(skillRepository).findByName("java");
        inOrder.verify(skillRepository).save(any(Skill.class));
    }

    @Test
    @DisplayName("findOrCreateSkill: 'Java' і 'java' нормалізуються до одного lookup-ключа 'java'")
    void findOrCreate_caseInsensitive_sameLookupKey() {
        Skill existing = new Skill();
        existing.setName("java");
        when(skillRepository.findByName("java")).thenReturn(Optional.of(existing));

        service.findOrCreateSkill("Java", SkillCategory.LANGUAGE);
        service.findOrCreateSkill("java", SkillCategory.LANGUAGE);

        // Обидва виклики звертаються до того самого нормалізованого ключа.
        verify(skillRepository, Mockito.times(2)).findByName("java");
        verify(skillRepository, never()).save(any());
    }

    // ─── triggerEmbeddingGeneration ─────────────────────────────────────

    @Test
    @DisplayName("triggerEmbeddingGeneration: EmbeddingModel.embed + JdbcTemplate UPDATE з vector-літералом")
    void triggerEmbedding_callsModelAndJdbcUpdate() {
        UUID skillId = UUID.randomUUID();
        Skill skill = new Skill();
        skill.setId(skillId);
        skill.setName("java");
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));
        when(embeddingModel.embed("java")).thenReturn(new float[] {0.1f, 0.2f});

        service.triggerEmbeddingGeneration(skillId);

        verify(embeddingModel).embed("java");
        // Нативний UPDATE: vector-літерал "[0.1,0.2]" + id у WHERE.
        verify(jdbcTemplate).update(
                eq("UPDATE skills SET embedding = CAST(? AS vector) WHERE id = ?"),
                eq("[0.1,0.2]"),
                eq(skillId));
    }

    @Test
    @DisplayName("triggerEmbeddingGeneration: скіл зник -> WARN та skip, embed/UPDATE НЕ викликаються")
    void triggerEmbedding_skillMissing_skips() {
        UUID skillId = UUID.randomUUID();
        when(skillRepository.findById(skillId)).thenReturn(Optional.empty());

        service.triggerEmbeddingGeneration(skillId);

        verify(embeddingModel, never()).embed(anyString());
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("triggerEmbeddingGeneration: помилка моделі проковтується (fire-and-forget), UPDATE не йде")
    void triggerEmbedding_modelFails_swallowed() {
        UUID skillId = UUID.randomUUID();
        Skill skill = new Skill();
        skill.setId(skillId);
        skill.setName("java");
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));
        when(embeddingModel.embed("java")).thenThrow(new RuntimeException("API down"));

        // Не повинно кидати — помилка одного скіла не валить виклик-ініціатор.
        service.triggerEmbeddingGeneration(skillId);

        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }
}
