package com.jobtracker.backendJobTracker.cv.tailored;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.cv.tailored.dto.GenerateTailoredCvRequest;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredCv;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredEducation;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredExperience;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredLanguage;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredProject;
import com.jobtracker.backendJobTracker.cv.tailored.dto.ParsedTailoredSkill;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvSummaryResponse;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredCv;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredEducation;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredExperience;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredLanguage;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredProject;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredSkill;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredCvRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredEducationRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredExperienceRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredLanguageRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredProjectRepository;
import com.jobtracker.backendJobTracker.cv.tailored.repos.TailoredSkillRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TailoredCvService {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(TailoredCvService.class);
 
    private final TailoredCvRepository tailoredCvRepository;
    private final TailoredExperienceRepository experienceRepository;
    private final TailoredEducationRepository educationRepository;
    private final TailoredSkillRepository skillRepository;
    private final TailoredProjectRepository projectRepository;
    private final TailoredLanguageRepository languageRepository;
 
    private final ApplicationRepository applicationRepository;
    private final MasterCvRepository masterCvRepository;
 
    private final TailoredCvGenerator generator;
    private final TailoredCvMapper mapper;
 
    
 
    @Transactional
    public TailoredCvResponse generate(UUID userId, UUID applicationId, GenerateTailoredCvRequest request) {
        Application app = fetchOwnedApplication(userId, applicationId);
        ensureJobHasDescription(app);
 
        MasterCv master = masterCvRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Upload your CV first — tailored CV needs context."));
 
        // LLM call: master + job → ParsedTailoredCv
        ParsedTailoredCv parsed = generator.generate(master, app, request.getEmphasisInstructions());
 
        // Створюємо новий TailoredCv (immutable, version+1)
        TailoredCv cv = buildTailoredCv(app, master, parsed);
        cv = tailoredCvRepository.save(cv);
 
        // Attach sub-entities
        attachExperiences(cv, parsed.getExperiences());
        attachEducations(cv, parsed.getEducations());
        attachSkills(cv, parsed.getSkills());
        attachProjects(cv, parsed.getProjects());
        attachLanguages(cv, parsed.getLanguages());
 
        // Повертаємо повний CV з колекціями (як getMy у W4)
        return loadFullResponse(cv);
    }
 
    
 
    public List<TailoredCvSummaryResponse> list(UUID userId, UUID applicationId) {
        fetchOwnedApplication(userId, applicationId);    // ownership check
        return tailoredCvRepository.findByApplication_IdOrderByVersionDesc(applicationId).stream()
                .map(mapper::toSummaryResponse)
                .toList();
    }
 
    public TailoredCvResponse getById(UUID userId, UUID tailoredCvId) {
        TailoredCv cv = tailoredCvRepository.findByIdAndApplication_User_Id(tailoredCvId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tailored CV not found: " + tailoredCvId));
        return loadFullResponse(cv);
    }
 
    @Transactional
    public void delete(UUID userId, UUID tailoredCvId) {
        TailoredCv cv = tailoredCvRepository.findByIdAndApplication_User_Id(tailoredCvId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tailored CV not found: " + tailoredCvId));
        tailoredCvRepository.delete(cv);   // CASCADE забере sub-entities
    }
 
    
 
    private TailoredCv buildTailoredCv(Application app, MasterCv master, ParsedTailoredCv parsed) {
        TailoredCv cv = new TailoredCv();
        cv.setApplication(app);
        cv.setVersion(nextVersion(app.getId()));
 
        // Hero block: LLM output → fallback на master
        cv.setFullName(coalesce(parsed.getFullName(), master.getFullName()));
        cv.setHeadline(coalesce(parsed.getHeadline(), master.getHeadline()));
        cv.setEmail(coalesce(parsed.getEmail(), master.getEmail()));
        cv.setPhone(coalesce(parsed.getPhone(), master.getPhone()));
        cv.setLinkedInUrl(coalesce(parsed.getLinkedInUrl(), master.getLinkedInUrl()));
        cv.setGithubUrl(coalesce(parsed.getGithubUrl(), master.getGithubUrl()));
        cv.setSummary(coalesce(parsed.getSummary(), master.getSummary()));
        cv.setLanguage(parsed.getLanguage() != null ? parsed.getLanguage() : master.getLanguage());
        if (cv.getLanguage() == null) cv.setLanguage(CvLanguage.EN);
 
        cv.setPromptVersion(TailoredCvPrompt.VERSION);
        cv.setCreatedAt(Instant.now());
        return cv;
    }
 
    private void attachExperiences(TailoredCv cv, List<ParsedTailoredExperience> items) {
        if (items == null) return;
        for (ParsedTailoredExperience pe : items) {
            if (isBlank(pe.getPosition()) || isBlank(pe.getCompany())) continue;
            LocalDate start = parseDate(pe.getStartDate());
            if (start == null) continue;             // startDate обов'язковий у БД
 
            TailoredExperience e = new TailoredExperience();
            e.setTailoredCv(cv);
            e.setPosition(pe.getPosition());
            e.setCompany(pe.getCompany());
            e.setLocation(pe.getLocation());
            e.setStartDate(start);
            e.setEndDate(parseDate(pe.getEndDate()));
            e.setDescription(pe.getDescription());
            experienceRepository.save(e);
        }
    }
 
    private void attachEducations(TailoredCv cv, List<ParsedTailoredEducation> items) {
        if (items == null) return;
        for (ParsedTailoredEducation pe : items) {
            if (isBlank(pe.getInstitution())) continue;
            TailoredEducation e = new TailoredEducation();
            e.setTailoredCv(cv);
            e.setInstitution(pe.getInstitution());
            e.setDegree(pe.getDegree());
            e.setFieldOfStudy(pe.getFieldOfStudy());
            e.setLocation(pe.getLocation());
            e.setStartDate(parseDate(pe.getStartDate()));
            e.setEndDate(parseDate(pe.getEndDate()));
            e.setDescription(pe.getDescription());
            educationRepository.save(e);
        }
    }
 
    private void attachSkills(TailoredCv cv, List<ParsedTailoredSkill> items) {
        if (items == null) return;
        for (ParsedTailoredSkill ps : items) {
            if (isBlank(ps.getName())) continue;
            TailoredSkill s = new TailoredSkill();
            s.setTailoredCv(cv);
            s.setName(ps.getName().toLowerCase().trim());
            s.setCategory(ps.getCategory());
            s.setLevel(ps.getLevel());
            skillRepository.save(s);
        }
    }
 
    private void attachProjects(TailoredCv cv, List<ParsedTailoredProject> items) {
        if (items == null) return;
        for (ParsedTailoredProject pp : items) {
            if (isBlank(pp.getName())) continue;
            TailoredProject p = new TailoredProject();
            p.setTailoredCv(cv);
            p.setName(pp.getName());
            p.setDescription(pp.getDescription());
            p.setUrl(pp.getUrl());
            p.setTechnologies(pp.getTechnologies());
            p.setStartDate(parseDate(pp.getStartDate()));
            p.setEndDate(parseDate(pp.getEndDate()));
            projectRepository.save(p);
        }
    }
 
    private void attachLanguages(TailoredCv cv, List<ParsedTailoredLanguage> items) {
        if (items == null) return;
        for (ParsedTailoredLanguage pl : items) {
            if (isBlank(pl.getName()) || pl.getLevel() == null) continue;
            TailoredLanguage l = new TailoredLanguage();
            l.setTailoredCv(cv);
            l.setName(pl.getName());
            l.setLevel(pl.getLevel());
            languageRepository.save(l);
        }
    }
 
   
 
    private TailoredCvResponse loadFullResponse(TailoredCv cv) {
        TailoredCvResponse response = mapper.toResponse(cv);
        UUID cvId = cv.getId();
 
        response.setExperiences(experienceRepository.findByTailoredCv_IdOrderByStartDateDesc(cvId)
                .stream().map(mapper::toExperienceResponse).toList());
        response.setEducations(educationRepository.findByTailoredCv_IdOrderByStartDateDesc(cvId)
                .stream().map(mapper::toEducationResponse).toList());
        response.setSkills(skillRepository.findByTailoredCv_Id(cvId)
                .stream().map(mapper::toSkillResponse).toList());
        response.setProjects(projectRepository.findByTailoredCv_IdOrderByStartDateDesc(cvId)
                .stream().map(mapper::toProjectResponse).toList());
        response.setLanguages(languageRepository.findByTailoredCv_Id(cvId)
                .stream().map(mapper::toLanguageResponse).toList());
 
        return response;
    }
 
    
 
    private int nextVersion(UUID applicationId) {
        return tailoredCvRepository.findTopByApplication_IdOrderByVersionDesc(applicationId)
                .map(cv -> cv.getVersion() + 1)
                .orElse(1);
    }
 
    private Application fetchOwnedApplication(UUID userId, UUID applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found: " + applicationId));
    }
 
    private void ensureJobHasDescription(Application app) {
        if (app.getDescription() == null || app.getDescription().isBlank()) {
            throw new BusinessRuleException(
                    "Application has no description. Add job posting details first.");
        }
    }
 
    /** Як у CvExtractionService: пробуємо різні формати дат. */
    private LocalDate parseDate(String raw) {
        if (isBlank(raw)) return null;
        String s = raw.trim().toLowerCase();
        if (s.equals("present") || s.equals("current") || s.equals("now")) return null;
        try { return LocalDate.parse(raw.trim()); } catch (Exception ignored) {}
        try { return LocalDate.parse(raw.trim() + "-01"); } catch (Exception ignored) {}
        try { return LocalDate.parse(raw.trim() + "-01-01"); } catch (Exception ignored) {}
        LOGGER.debug("Could not parse date: {}", raw);
        return null;
    }
 
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
 
    private String coalesce(String a, String b) { return a != null && !a.isBlank() ? a : b; }
}

