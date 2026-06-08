package com.jobtracker.backendJobTracker.cv;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.backendJobTracker.ai.AiService;
import com.jobtracker.backendJobTracker.cv.dto.parse.ParsedCv;
import com.jobtracker.backendJobTracker.cv.dto.parse.ParsedEducation;
import com.jobtracker.backendJobTracker.cv.dto.parse.ParsedExperience;
import com.jobtracker.backendJobTracker.cv.dto.parse.ParsedLanguage;
import com.jobtracker.backendJobTracker.cv.dto.parse.ParsedProject;
import com.jobtracker.backendJobTracker.cv.dto.parse.ParsedSkill;
import com.jobtracker.backendJobTracker.cv.enums.CvLanguage;
import com.jobtracker.backendJobTracker.cv.models.Education;
import com.jobtracker.backendJobTracker.cv.models.Experience;
import com.jobtracker.backendJobTracker.cv.models.Language;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;
import com.jobtracker.backendJobTracker.cv.models.Project;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.EducationRepository;
import com.jobtracker.backendJobTracker.cv.repo.ExperienceRepository;
import com.jobtracker.backendJobTracker.cv.repo.LanguageRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvSkillRepository;
import com.jobtracker.backendJobTracker.cv.repo.ProjectRepository;
import com.jobtracker.backendJobTracker.cv.repo.SkillRepository;
import com.jobtracker.backendJobTracker.user.UserRepository;
import com.jobtracker.backendJobTracker.util.HashUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CvExtractionService {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(CvExtractionService.class);
 
    private final CvFileValidation fileValidation;
    private final CvFileParser fileParser;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    
    private final MasterCvRepository masterCvRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final MasterCvSkillRepository masterCvSkillRepository;
    private final ProjectRepository projectRepository;
    private final LanguageRepository languageRepository;
    private final SkillRepository skillRepository;
 
    private final SkillService skillService;
    private final UserRepository userRepository;
 
    /** Preview: file → LLM → ParsedCv. БД не чіпаємо. */
    public ParsedCv preview(MultipartFile file) {
        fileValidation.validate(file);
        String text = fileParser.extractText(file);
        return callLlmAndParse(text);
    }
 
    /** REPLACE flow: видаляємо старий MasterCv юзера, створюємо новий з parsed даних. */
    @Transactional
    public MasterCv extractAndReplace(UUID userId, MultipartFile file) {
        fileValidation.validate(file);
        String text = fileParser.extractText(file);
        String hash = HashUtil.sha256(text);
        ParsedCv parsed = callLlmAndParse(text);
 
        // CASCADE забирає всі sub-entities. Сам Skill каталог не зачіпається.
        masterCvRepository.findByUserId(userId).ifPresent(masterCvRepository::delete);
        // flush щоб DELETE виконався ДО INSERT — інакше UNIQUE(user_id) constraint впаде
        masterCvRepository.flush();
 
        MasterCv cv = buildMasterCv(userId, parsed, file.getOriginalFilename(), hash);
        cv = masterCvRepository.save(cv);
 
        attachExperiences(cv, parsed.getExperiences());
        attachEducations(cv, parsed.getEducations());
        attachProjects(cv, parsed.getProjects());
        attachLanguages(cv, parsed.getLanguages());
        attachSkills(cv, parsed.getSkills());
 
        LOGGER.info("CV replaced for user {} from file {}", userId, file.getOriginalFilename());
        return cv;
    }
 
    // ─── LLM + JSON ─────────────────────────────────────────────────
 
    private ParsedCv callLlmAndParse(String text) {
        String prompt = CvExtractionPrompt.build(text);
        String json;
        try {
            json = aiService.complete(prompt);
        } catch (Exception e) {
            LOGGER.error("LLM extraction failed: {}", e.getMessage(), e);
            throw new CvExtractionException("AI extraction failed. Try again or fill manually.", e);
        }
        if (json == null || json.isBlank()) {
            throw new CvExtractionException("AI returned empty response.", null);
        }
 
        try {
            return objectMapper.readValue(stripMarkdownFences(json), ParsedCv.class);
        } catch (Exception e) {
            LOGGER.error("Failed to parse LLM JSON: {} | raw: {}", e.getMessage(), json);
            throw new CvExtractionException("AI response could not be parsed. Try again.", e);
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
 
    
 
    private MasterCv buildMasterCv(UUID userId, ParsedCv p, String filename, String hash) {
        MasterCv cv = new MasterCv();
        cv.setUser(userRepository.getReferenceById(userId));
        cv.setLanguage(p.getLanguage() != null ? p.getLanguage() : CvLanguage.EN);
        cv.setFullName(p.getFullName());
        cv.setHeadline(p.getHeadline());
        cv.setEmail(p.getEmail());
        cv.setPhone(p.getPhone());
        cv.setLinkedInUrl(p.getLinkedInUrl());
        cv.setGithubUrl(p.getGithubUrl());
        cv.setSummary(p.getSummary());
        cv.setSourceFileName(filename);
        cv.setSourceFileHash(hash);
        cv.setExtractionPromptVersion(CvExtractionPrompt.VERSION);
        cv.setExtractedAt(Instant.now());
        return cv;
    }
 
    private void attachExperiences(MasterCv cv, List<ParsedExperience> list) {
        if (list == null) return;
        for (ParsedExperience p : list) {
            if (p.getCompany() == null || p.getPosition() == null) continue;
            // Required field у Experience — startDate. Skip якщо немає.
            LocalDate start = parseDate(p.getStartDate());
            if (start == null) continue;
 
            Experience e = new Experience();
            e.setMasterCv(cv);
            e.setPosition(p.getPosition());
            e.setCompany(p.getCompany());
            e.setLocation(p.getLocation());
            e.setStartDate(start);
            e.setEndDate(parseDate(p.getEndDate()));
            e.setDescription(p.getDescription());
            experienceRepository.save(e);
        }
    }
 
    private void attachEducations(MasterCv cv, List<ParsedEducation> list) {
        if (list == null) return;
        for (ParsedEducation p : list) {
            if (p.getInstitution() == null) continue;
            Education e = new Education();
            e.setMasterCv(cv);
            e.setInstitution(p.getInstitution());
            e.setDegree(p.getDegree());
            e.setFieldOfStudy(p.getFieldOfStudy());
            e.setStartDate(parseDate(p.getStartDate()));
            e.setEndDate(parseDate(p.getEndDate()));
            e.setDescription(p.getDescription());
            educationRepository.save(e);
        }
    }
 
    private void attachProjects(MasterCv cv, List<ParsedProject> list) {
        if (list == null) return;
        for (ParsedProject p : list) {
            if (p.getName() == null) continue;
            Project pr = new Project();
            pr.setMasterCv(cv);
            pr.setName(p.getName());
            pr.setDescription(p.getDescription());
            pr.setUrl(p.getUrl());
            pr.setTechnologies(p.getTechnologies());
            pr.setStartDate(parseDate(p.getStartDate()));
            pr.setEndDate(parseDate(p.getEndDate()));
            projectRepository.save(pr);
        }
    }
 
    private void attachLanguages(MasterCv cv, List<ParsedLanguage> list) {
        if (list == null) return;
        for (ParsedLanguage p : list) {
            if (p.getName() == null || p.getLevel() == null) continue;
            Language l = new Language();
            l.setMasterCv(cv);
            l.setName(p.getName());
            l.setLevel(p.getLevel());
            languageRepository.save(l);
        }
    }
 
    private void attachSkills(MasterCv cv, List<ParsedSkill> list) {
        if (list == null) return;
        for (ParsedSkill p : list) {
            if (p.getName() == null || p.getName().isBlank()) continue;
 
            // findOrCreate у shared catalog. Race: дві extraction одночасно для нового скіла —
            // UNIQUE constraint спіймає, скіл буде один.
            boolean isNewSkill = !skillRepository.existsByName(p.getName().toLowerCase().trim());
            Skill skill = skillService.findOrCreateSkill(p.getName(), p.getCategory());
 
            MasterCvSkill mcs = new MasterCvSkill();
            mcs.setMasterCv(cv);
            mcs.setSkill(skill);
            mcs.setLevel(p.getLevel());
            masterCvSkillRepository.save(mcs);
 
            // Embedding після commit, як у CvService.addSkill — той самий patern.
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
    }
 
    
    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim().toLowerCase();
        if ("present".equals(trimmed) || "current".equals(trimmed) || "now".equals(trimmed)) return null;
 
        try { return LocalDate.parse(raw); } catch (Exception ignored) {}
        try { return LocalDate.parse(raw + "-01"); } catch (Exception ignored) {}
        try { return LocalDate.parse(raw + "-01-01"); } catch (Exception ignored) {}
 
        LOGGER.debug("Could not parse date: {}", raw);
        return null;
    }
}

