package com.jobtracker.backendJobTracker.coverletter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.coverletter.enums.CoverLetterTone;
import com.jobtracker.backendJobTracker.cv.models.Education;
import com.jobtracker.backendJobTracker.cv.models.Experience;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;
import com.jobtracker.backendJobTracker.cv.repo.EducationRepository;
import com.jobtracker.backendJobTracker.cv.repo.ExperienceRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvSkillRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CoverLetterGenerator {
 
    private static final int MAX_EXPERIENCES_IN_CONTEXT = 5;
    private static final int MAX_SKILLS_IN_CONTEXT = 20;
 
    private final AiService aiService;
    // ВИПРАВЛЕНО: CvService не потрібен — використовуємо репозиторії напряму
    // для збору колекцій (MasterCv не має @OneToMany).
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final MasterCvSkillRepository masterCvSkillRepository;
 
    /**
     * ВИПРАВЛЕНО: tone — CoverLetterTone enum, не String. Prompt отримує .name().
     */
    public String generate(MasterCv cv, Application app, CoverLetterTone tone, String extraInstructions) {
        String jobContext = buildJobContext(app);
        String cvContext = buildCvContext(cv);
        String prompt = CoverLetterPrompt.build(jobContext, cvContext, tone.name(), extraInstructions);
        return aiService.complete(prompt);
    }
 
    public String refine(String currentLetter, String instruction) {
        String prompt = CoverLetterRefinePrompt.build(currentLetter, instruction);
        return aiService.complete(prompt);
    }
 
    
    // CONTEXT BUILDERS
    
 
    
    private String buildJobContext(Application app) {
        StringBuilder sb = new StringBuilder();
        sb.append("Position: ").append(safe(app.getName())).append("\n");
        // ВИПРАВЛЕНО: .getName() — без нього String.format друкував "Company@a3b4..."
        sb.append("Company: ").append(app.getCompany() != null ? safe(app.getCompany().getName()) : "")
                .append("\n");
        sb.append("Seniority: ").append(app.getSeniority()).append("\n");
        sb.append("Location: ").append(safe(app.getLocation())).append("\n");
        sb.append("Work mode: ").append(app.getWorkMode()).append("\n");
        sb.append("\nDescription:\n").append(safe(app.getDescription()));
        return sb.toString();
    }
 
    
    private String buildCvContext(MasterCv cv) {
        UUID cvId = cv.getId();
        StringBuilder sb = new StringBuilder();
 
        sb.append("Name: ").append(safe(cv.getFullName())).append("\n");
        sb.append("Headline: ").append(safe(cv.getHeadline())).append("\n");
        if (cv.getSummary() != null && !cv.getSummary().isBlank()) {
            sb.append("\nSummary:\n").append(cv.getSummary()).append("\n");
        }
 
        // Experience — top-N від найновішої
        List<Experience> experiences = experienceRepository.findByMasterCvIdOrderByStartDateDesc(cvId);
        if (!experiences.isEmpty()) {
            sb.append("\nExperience:\n");
            experiences.stream().limit(MAX_EXPERIENCES_IN_CONTEXT).forEach(e -> {
                sb.append("- ").append(safe(e.getPosition()))
                        .append(" @ ").append(safe(e.getCompany()))
                        .append(" (").append(e.getStartDate())
                        .append(" – ").append(e.getEndDate() != null ? e.getEndDate() : "present")
                        .append(")");
                if (e.getDescription() != null && !e.getDescription().isBlank()) {
                    sb.append(": ").append(e.getDescription().replace("\n", " "));
                }
                sb.append("\n");
            });
        }
 
        // Skills — список через кому, top-N
        List<MasterCvSkill> skills = masterCvSkillRepository.findByMasterCvId(cvId);
        if (!skills.isEmpty()) {
            String skillList = skills.stream()
                    .limit(MAX_SKILLS_IN_CONTEXT)
                    .map(mcs -> mcs.getSkill().getName())
                    .collect(Collectors.joining(", "));
            sb.append("\nSkills: ").append(skillList).append("\n");
        }
 
        // Education — лише найвищий рівень (latest) для компактності
        List<Education> educations = educationRepository.findByMasterCvIdOrderByStartDateDesc(cvId);
        if (!educations.isEmpty()) {
            Education edu = educations.get(0);
            sb.append("\nEducation: ").append(safe(edu.getDegree()))
                    .append(" in ").append(safe(edu.getFieldOfStudy()))
                    .append(", ").append(safe(edu.getInstitution()));
        }
 
        return sb.toString();
    }
 
    /** null → "". Уникає "null" у promp'і. */
    private String safe(String s) {
        return s == null ? "" : s;
    }
}

