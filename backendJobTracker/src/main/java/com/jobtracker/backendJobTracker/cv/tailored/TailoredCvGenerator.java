package com.jobtracker.backendJobTracker.cv.tailored;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.cv.models.Education;
import com.jobtracker.backendJobTracker.cv.models.Experience;
import com.jobtracker.backendJobTracker.cv.models.Language;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;
import com.jobtracker.backendJobTracker.cv.models.Project;
import com.jobtracker.backendJobTracker.cv.repo.EducationRepository;
import com.jobtracker.backendJobTracker.cv.repo.ExperienceRepository;
import com.jobtracker.backendJobTracker.cv.repo.LanguageRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvSkillRepository;
import com.jobtracker.backendJobTracker.cv.repo.ProjectRepository;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredCv;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TailoredCvGenerator {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(TailoredCvGenerator.class);
 
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    // ВИПРАВЛЕНО: master repositories, не tailored
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final MasterCvSkillRepository masterCvSkillRepository;
    private final ProjectRepository projectRepository;
    private final LanguageRepository languageRepository;
 
    public ParsedTailoredCv generate(MasterCv master, Application app, String emphasisInstructions) {
        String jobContext = buildJobContext(app);
        String masterContext = buildMasterContext(master);
 
        String prompt = TailoredCvPrompt.build(jobContext, masterContext, emphasisInstructions);
        String json = aiService.complete(prompt);
 
        if (json == null || json.isBlank()) {
            throw new BusinessRuleException("LLM returned empty response for tailored CV");
        }
 
        return parseJson(json);
    }
 
    
 
    private String buildJobContext(Application app) {
        StringBuilder sb = new StringBuilder();
        sb.append("Position: ").append(safe(app.getName())).append("\n");
        sb.append("Company: ")
                .append(app.getCompany() != null ? safe(app.getCompany().getName()) : "")
                .append("\n");
        sb.append("Seniority: ").append(app.getSeniority()).append("\n");
        sb.append("Location: ").append(safe(app.getLocation())).append("\n");
        sb.append("Work mode: ").append(app.getWorkMode()).append("\n");
        sb.append("\nDescription:\n").append(safe(app.getDescription()));
        return sb.toString();
    }
 
    /**
     * ВИПРАВЛЕНО: повний контекст з MASTER. БЕЗ обмежень — LLM сам вирішує
     * що включити, що відкинути, що переписати. Це і є tailoring.
     */
    private String buildMasterContext(MasterCv cv) {
        UUID cvId = cv.getId();
        StringBuilder sb = new StringBuilder();
 
        sb.append("Name: ").append(safe(cv.getFullName())).append("\n");
        sb.append("Headline: ").append(safe(cv.getHeadline())).append("\n");
        sb.append("Email: ").append(safe(cv.getEmail())).append("\n");
        sb.append("Phone: ").append(safe(cv.getPhone())).append("\n");
        sb.append("LinkedIn: ").append(safe(cv.getLinkedInUrl())).append("\n");
        sb.append("GitHub: ").append(safe(cv.getGithubUrl())).append("\n");
        sb.append("Language: ").append(cv.getLanguage()).append("\n");
 
        if (cv.getSummary() != null && !cv.getSummary().isBlank()) {
            sb.append("\nSummary:\n").append(cv.getSummary()).append("\n");
        }
 
        // ALL experiences — без limit
        List<Experience> experiences = experienceRepository.findByMasterCvIdOrderByStartDateDesc(cvId);
        if (!experiences.isEmpty()) {
            sb.append("\nExperience:\n");
            for (Experience e : experiences) {
                sb.append("- ").append(safe(e.getPosition()))
                        .append(" @ ").append(safe(e.getCompany()))
                        .append(" (").append(e.getStartDate())
                        .append(" – ").append(e.getEndDate() != null ? e.getEndDate() : "present")
                        .append(")");
                if (e.getDescription() != null && !e.getDescription().isBlank()) {
                    sb.append("\n  ").append(e.getDescription().replace("\n", "\n  "));
                }
                sb.append("\n");
            }
        }
 
        // ALL skills
        List<MasterCvSkill> skills = masterCvSkillRepository.findByMasterCvId(cvId);
        if (!skills.isEmpty()) {
            String skillList = skills.stream()
                    .map(mcs -> mcs.getSkill().getName()
                            + (mcs.getLevel() != null ? " (" + mcs.getLevel() + ")" : ""))
                    .collect(Collectors.joining(", "));
            sb.append("\nSkills: ").append(skillList).append("\n");
        }
 
        // ALL projects
        List<Project> projects = projectRepository.findByMasterCvIdOrderByStartDateDesc(cvId);
        if (!projects.isEmpty()) {
            sb.append("\nProjects:\n");
            for (Project p : projects) {
                sb.append("- ").append(safe(p.getName()));
                if (p.getTechnologies() != null && !p.getTechnologies().isBlank()) {
                    sb.append(" [").append(p.getTechnologies()).append("]");
                }
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    sb.append(": ").append(p.getDescription().replace("\n", " "));
                }
                sb.append("\n");
            }
        }
 
        // ALL educations
        List<Education> educations = educationRepository.findByMasterCvIdOrderByStartDateDesc(cvId);
        if (!educations.isEmpty()) {
            sb.append("\nEducation:\n");
            for (Education edu : educations) {
                sb.append("- ").append(safe(edu.getDegree()))
                        .append(" in ").append(safe(edu.getFieldOfStudy()))
                        .append(", ").append(safe(edu.getInstitution()))
                        .append(" (").append(edu.getStartDate())
                        .append(" – ").append(edu.getEndDate() != null ? edu.getEndDate() : "present")
                        .append(")\n");
            }
        }
 
        // Languages
        List<Language> languages = languageRepository.findByMasterCvId(cvId);
        if (!languages.isEmpty()) {
            String langList = languages.stream()
                    .map(l -> l.getName() + " (" + l.getLevel() + ")")
                    .collect(Collectors.joining(", "));
            sb.append("\nLanguages: ").append(langList).append("\n");
        }
 
        return sb.toString();
    }
 
    
 
    private ParsedTailoredCv parseJson(String json) {
        try {
            String cleaned = stripMarkdownFences(json);
            return objectMapper.readValue(cleaned, ParsedTailoredCv.class);
        } catch (Exception e) {
            LOGGER.error("Failed to parse LLM JSON: {} | raw: {}", e.getMessage(), json);
            throw new BusinessRuleException("Could not parse tailored CV response");
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
