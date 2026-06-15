package com.jobtracker.backendJobTracker.interview;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.gap.ApplicationSkill;
import com.jobtracker.backendJobTracker.application.gap.ApplicationSkillRepository;
import com.jobtracker.backendJobTracker.cv.models.Experience;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;
import com.jobtracker.backendJobTracker.cv.repo.ExperienceRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvSkillRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.interview.dto.parse.ParsedPrepGuide;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InterviewPrepGenerator {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(InterviewPrepGenerator.class);
    // ↑ ВИПРАВЛЕНО: був TailoredCvGenerator.class
 
    private static final int MAX_EXPERIENCES_IN_CONTEXT = 5;
    private static final int MAX_SKILLS_IN_CONTEXT = 20;
 
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final ApplicationSkillRepository applicationSkillRepository;
    private final ExperienceRepository experienceRepository;
    private final MasterCvSkillRepository masterCvSkillRepository;
 
    public ParsedPrepGuide generate(Application app, MasterCv master) {
        String jobContext = buildJobContext(app);
        String masterContext = buildMasterContext(master);
 
        String prompt = InterviewPrepPrompt.build(jobContext, masterContext);
        String json = aiService.complete(prompt);
 
        if (json == null || json.isBlank()) {
            // ВИПРАВЛЕНО: повідомлення про interview prep, не tailored CV
            throw new BusinessRuleException("LLM returned empty response for interview prep");
        }
 
        // ВИПРАВЛЕНО: раніше parseJson не викликався взагалі — LLM відповідь
        // повністю ігнорувалась і questions завжди порожні.
        return parseJson(json);
    }
 
    private String buildJobContext(Application app) {
        StringBuilder sb = new StringBuilder();
        sb.append("Position: ").append(safe(app.getName())).append("\n");
        sb.append("Company: ")
                .append(app.getCompany() != null ? safe(app.getCompany().getName()) : "")
                .append("\n");
        // ДОДАНО: seniority — впливає на складність питань (junior vs senior)
        sb.append("Seniority: ").append(app.getSeniority()).append("\n");
        sb.append("Work mode: ").append(app.getWorkMode()).append("\n");
        sb.append("Location: ").append(safe(app.getLocation())).append("\n");
 
        // ВИПРАВЛЕНО: skill.getSkill().getName() — реальні назви, не toString() entities.
        // ВИПРАВЛЕНО: joining через ", " — не "[skill1, skill2]" від List.toString()
        List<ApplicationSkill> skills = applicationSkillRepository.findByApplicationId(app.getId());
        if (!skills.isEmpty()) {
            String techList = skills.stream()
                    .map(s -> s.getSkill().getName())
                    .collect(Collectors.joining(", "));
            sb.append("Technologies: ").append(techList).append("\n");
        }
 
        sb.append("\nDescription:\n").append(safe(app.getDescription()));
        return sb.toString();
    }
 
    /**
     * ДОДАНО: повний контекст master CV.
     * Раніше було тільки hero block — LLM не міг оцінити seniority за досвідом.
     */
    private String buildMasterContext(MasterCv cv) {
        UUID cvId = cv.getId();
        StringBuilder sb = new StringBuilder();
 
        sb.append("Name: ").append(safe(cv.getFullName())).append("\n");
        sb.append("Headline: ").append(safe(cv.getHeadline())).append("\n");
        if (cv.getSummary() != null && !cv.getSummary().isBlank()) {
            sb.append("\nSummary:\n").append(cv.getSummary()).append("\n");
        }
 
        // Experiences — top 5 для prep context (не треба ВСІ як у tailored CV)
        List<Experience> experiences = experienceRepository.findByMasterCvIdOrderByStartDateDesc(cvId);
        if (!experiences.isEmpty()) {
            sb.append("\nExperience overview:\n");
            experiences.stream().limit(MAX_EXPERIENCES_IN_CONTEXT).forEach(e -> {
                sb.append("- ").append(safe(e.getPosition()))
                        .append(" @ ").append(safe(e.getCompany()))
                        .append(" (").append(e.getStartDate())
                        .append(" – ").append(e.getEndDate() != null ? e.getEndDate() : "present")
                        .append(")\n");
            });
        }
 
        // Skills — top 20
        List<MasterCvSkill> skills = masterCvSkillRepository.findByMasterCvId(cvId);
        if (!skills.isEmpty()) {
            String skillList = skills.stream()
                    .limit(MAX_SKILLS_IN_CONTEXT)
                    .map(mcs -> mcs.getSkill().getName())
                    .collect(Collectors.joining(", "));
            sb.append("\nSkills: ").append(skillList).append("\n");
        }
 
        return sb.toString();
    }
 
    private ParsedPrepGuide parseJson(String json) {
        try {
            String cleaned = stripMarkdownFences(json);
            return objectMapper.readValue(cleaned, ParsedPrepGuide.class);
        } catch (Exception e) {
            LOGGER.error("Failed to parse LLM JSON: {} | raw: {}", e.getMessage(), json);
            // ВИПРАВЛЕНО: повідомлення про interview prep, не tailored CV
            throw new BusinessRuleException("Could not parse interview prep response");
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
 
    private String safe(String s) {
        return s == null ? "" : s;
    }
}
