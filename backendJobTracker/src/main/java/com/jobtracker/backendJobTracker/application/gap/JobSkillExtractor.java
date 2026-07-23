package com.jobtracker.backendJobTracker.application.gap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.cv.SkillService;
import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.SkillRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobSkillExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobSkillExtractor.class);

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final SkillService skillService;
    private final SkillRepository skillRepository;
    private final ApplicationSkillRepository applicationSkillRepository;

    @Transactional
    public List<ApplicationSkill> extractAndSaveSkills(Application application) {
        if (application == null) throw new BusinessRuleException("Application is required");
        return extractAndSaveSkills(application, application.getDescription());
    }

    @Transactional
    public List<ApplicationSkill> extractAndSaveSkills(Application application, String jobText) {
        if (application == null) throw new BusinessRuleException("Application is required");
        if (jobText == null || jobText.isBlank()) {
            LOGGER.debug("Empty job text for application {}, skipping", application.getId());
            return List.of();
        }

        
        String prompt = JobSkillExtractionPrompt.build(jobText);
        String json = aiService.complete(prompt);
        if (json == null || json.isBlank()) {
            throw new BusinessRuleException("LLM returned empty response");
        }

        
        ParsedJobSkills parsed = parseJson(json);

        
        applicationSkillRepository.deleteByApplicationId(application.getId());
        applicationSkillRepository.flush();

        Set<String> processed = new HashSet<>();
        List<ApplicationSkill> result = new ArrayList<>();
        result.addAll(saveSkills(application, parsed.required, true, processed));
        result.addAll(saveSkills(application, parsed.niceToHave, false, processed));

        
        return result;
    }

    private List<ApplicationSkill> saveSkills(
            Application application,
            List<ParsedSkillItem> items,        
            boolean required,
            Set<String> processed) {

        if (items == null) return List.of();

        List<ApplicationSkill> saved = new ArrayList<>();

        // Обходимо КОЖЕН ParsedSkillItem — це один елемент з JSON-масиву,
        // наприклад {"name": "java", "category": "LANGUAGE"}.
        for (ParsedSkillItem item : items) {

            // item.name і item.category — доступ до полів parsed-об'єкта.
            // item.name = "java", item.category = "LANGUAGE" (з JSON).
            if (item == null || item.name == null || item.name.isBlank()) continue;

            String normalized = item.name.toLowerCase().trim();
            if (!processed.add(normalized)) continue;

            // Тут ми "перекладаємо" ParsedSkillItem (тимчасовий DTO) у Skill (entity з БД).
            // ParsedSkillItem.name → Skill.name
            // ParsedSkillItem.category → Skill.category
            boolean isNewSkill = !skillRepository.existsByName(normalized);
            SkillCategory category = parseCategory(item.category);
            Skill skill = skillService.findOrCreateSkill(item.name, category);

            // Створюємо junction ApplicationSkill — це і є реальна "корисна" entity,
            // ParsedSkillItem був лише носієм даних, а ApplicationSkill зв'язує
            // заявку з catalog-скілом і зберігається у БД.
            ApplicationSkill as = new ApplicationSkill();
            as.setApplication(application);
            as.setSkill(skill);
            as.setRequired(required);
            applicationSkillRepository.save(as);
            saved.add(as);

            if (isNewSkill) {
                UUID skillId = skill.getId();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        skillService.triggerEmbeddingGeneration(skillId);
                    }
                });
            }
        }
        return saved;
    }


    private ParsedJobSkills parseJson(String json) {
        try {
            String cleaned = stripMarkdownFences(json);
            return objectMapper.readValue(cleaned, ParsedJobSkills.class);
        } catch (Exception e) {
            LOGGER.error("Failed to parse LLM JSON: {} | raw: {}", e.getMessage(), json);
            throw new BusinessRuleException("Could not parse job skills response");
        }
    }

    private String stripMarkdownFences(String s) {
        String trimmed = s.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private SkillCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return SkillCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    

    /**
     * Корінь JSON-структури від LLM. Мапиться на:
     * <pre>
     * {
     *   "required":   [...],   ← список required скілів
     *   "niceToHave": [...]    ← список бонусних скілів
     * }
     * </pre>
     * Імена полів ОБОВ'ЯЗКОВО мають збігатись з ключами JSON.
     */
    private static class ParsedJobSkills {
        public List<ParsedSkillItem> required;
        public List<ParsedSkillItem> niceToHave;
    }

    /**
     * Один елемент масиву всередині JSON. Мапиться на:
     * <pre>
     * { "name": "java", "category": "LANGUAGE" }
     * </pre>
     * category — String (не SkillCategory enum), бо LLM може повернути щось
     * неочікуване ("language", "lang", "PROG"). У parseCategory() ми безпечно
     * конвертуємо String → SkillCategory з fallback на null.
     */
    private static class ParsedSkillItem {
        public String name;
        public String category;
    }
}