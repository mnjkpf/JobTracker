package com.jobtracker.backendJobTracker.cv.tailored.ats;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredCv;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredEducation;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredExperience;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredProject;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredSkill;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredCvRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredEducationRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredExperienceRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredProjectRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredSkillRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtsScoringService {
 
    private final AtsKeywordExtractor atsKeywordExtractor;
    private final TailoredCvRepository tailoredCvRepository;
    private final TailoredExperienceRepository experienceRepository;
    private final TailoredEducationRepository educationRepository;
    private final TailoredSkillRepository skillRepository;
    private final TailoredProjectRepository projectRepository;
 
    public AtsScoreResponse score(UUID userId, UUID tailoredCvId) {
        // 1. Tenant-safe fetch tailored CV
        TailoredCv cv = tailoredCvRepository
                .findByIdAndApplication_User_Id(tailoredCvId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tailored CV not found: " + tailoredCvId));
 
        // 2. Application через FK — без окремого repository fetch
        Application application = cv.getApplication();
        if (application.getDescription() == null || application.getDescription().isBlank()) {
            throw new BusinessRuleException(
                    "Application has no description — cannot extract ATS keywords");
        }
 
        // 3. Завантажуємо всі sub-entities tailored CV
        UUID cvId = cv.getId();
        List<TailoredExperience> experiences = experienceRepository
                .findByTailoredCv_IdOrderByStartDateDesc(cvId);
        List<TailoredEducation> educations = educationRepository
                .findByTailoredCv_IdOrderByStartDateDesc(cvId);
        List<TailoredSkill> skills = skillRepository.findByTailoredCv_Id(cvId);
        List<TailoredProject> projects = projectRepository
                .findByTailoredCv_IdOrderByStartDateDesc(cvId);
 
        // 4. Будуємо великий рядок з усього тексту CV
        String cvText = buildCvText(cv, experiences, educations, skills, projects);
 
        // 5. ОДИН LLM call — витягуємо keywords з job description
        List<String> keywords = atsKeywordExtractor.extract(application.getDescription());
 
        // 6. Matching: для кожного keyword шукаємо у cvText
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        String cvTextLower = cvText.toLowerCase();
        for (String keyword : keywords) {
            // keywords уже нормалізовані (lowercase trim) у AtsKeywordExtractor
            if (cvTextLower.contains(keyword)) {
                matched.add(keyword);
            } else {
                missing.add(keyword);
            }
        }
 
        // 7. Score
        int total = keywords.size();
        int matchedCount = matched.size();
        // Якщо keywords порожні — score 0 (нема чого міряти).
        int score = total == 0 ? 0 : Math.round((float) matchedCount / total * 100);
 
        AtsScoreResponse response = new AtsScoreResponse();
        response.setScore(score);
        response.setTotalKeywords(total);
        response.setMatchedCount(matchedCount);
        response.setMatched(matched);
        response.setMissing(missing);
        response.setAnalyzedAt(Instant.now());
        return response;
    }
 
    
    private String buildCvText(TailoredCv cv,
                                List<TailoredExperience> experiences,
                                List<TailoredEducation> educations,
                                List<TailoredSkill> skills,
                                List<TailoredProject> projects) {
        StringBuilder sb = new StringBuilder();
 
        appendIfNotBlank(sb, cv.getSummary());
 
        for (TailoredExperience e : experiences) {
            appendIfNotBlank(sb, e.getPosition());
            appendIfNotBlank(sb, e.getCompany());
            appendIfNotBlank(sb, e.getDescription());
        }
 
        for (TailoredEducation e : educations) {
            appendIfNotBlank(sb, e.getDegree());
            appendIfNotBlank(sb, e.getFieldOfStudy());
            appendIfNotBlank(sb, e.getInstitution());
            appendIfNotBlank(sb, e.getDescription());
        }
 
        for (TailoredSkill s : skills) {
            appendIfNotBlank(sb, s.getName());
        }
 
        for (TailoredProject p : projects) {
            appendIfNotBlank(sb, p.getName());
            appendIfNotBlank(sb, p.getDescription());
            appendIfNotBlank(sb, p.getTechnologies());
        }
 
        return sb.toString();
    }
 
    private void appendIfNotBlank(StringBuilder sb, String s) {
        if (s != null && !s.isBlank()) {
            sb.append(s).append('\n');
        }
    }
}

