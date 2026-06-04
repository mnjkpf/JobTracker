package com.jobtracker.backendJobTracker.cv;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.jobtracker.backendJobTracker.cv.dto.AddSkillRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateEducationRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateExperienceRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateLanguageRequest;
import com.jobtracker.backendJobTracker.cv.dto.CreateProjectRequest;
import com.jobtracker.backendJobTracker.cv.dto.EducationResponse;
import com.jobtracker.backendJobTracker.cv.dto.ExperienceResponse;
import com.jobtracker.backendJobTracker.cv.dto.LanguageResponse;
import com.jobtracker.backendJobTracker.cv.dto.MasterCvResponse;
import com.jobtracker.backendJobTracker.cv.dto.ProjectResponse;
import com.jobtracker.backendJobTracker.cv.dto.SkillResponse;
import com.jobtracker.backendJobTracker.cv.dto.UpdateEducationRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateExperienceRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateLanguageRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateMasterCvRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateProjectRequest;
import com.jobtracker.backendJobTracker.cv.dto.UpdateSkillRequest;
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
import com.jobtracker.backendJobTracker.exception.ConflictException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Бізнес-логіка MasterCv та всіх sub-entities.
 * <p>
 * ІНВАРІАНТИ:
 *  1. userId — перший аргумент кожного публічного методу (multi-tenancy).
 *  2. Read: класовий {@code @Transactional(readOnly=true)}. Write: явний {@code @Transactional}.
 *  3. Tenant-safe fetch sub-entities через graph: {@code findByIdAndMasterCv_User_Id}.
 *  4. PATCH: null-поля НЕ перезаписують entity. Тільки явно надіслані змінюються.
 *  5. Add* викликає {@code fetchOrCreateMasterCv} — новий юзер може одразу додавати
 *     experience без попереднього "create CV" кроку.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CvService {

    private final MasterCvRepository masterCvRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final MasterCvSkillRepository masterCvSkillRepository;
    private final ProjectRepository projectRepository;
    private final LanguageRepository languageRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final SkillService skillService;
    private final CvMapper cvMapper;

    
    public MasterCvResponse getMy(UUID userId) {
        MasterCv masterCv = masterCvRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CV not found. Upload a resume or fill manually first."));

        MasterCvResponse response = cvMapper.toMasterCvResponse(masterCv);
        UUID cvId = masterCv.getId();

        response.setExperiences(experienceRepository.findByMasterCvIdOrderByStartDateDesc(cvId)
                .stream().map(cvMapper::toExperienceResponse).toList());
        response.setEducations(educationRepository.findByMasterCvIdOrderByStartDateDesc(cvId)
                .stream().map(cvMapper::toEducationResponse).toList());
        response.setSkills(masterCvSkillRepository.findByMasterCvId(cvId)
                .stream().map(cvMapper::toSkillResponse).toList());
        response.setProjects(projectRepository.findByMasterCvIdOrderByStartDateDesc(cvId)
                .stream().map(cvMapper::toProjectResponse).toList());
        response.setLanguages(languageRepository.findByMasterCvId(cvId)
                .stream().map(cvMapper::toLanguageResponse).toList());

        return response;
    }

    

    @Transactional
    public MasterCvResponse updateMetadata(UUID userId, UpdateMasterCvRequest r) {
        
        MasterCv cv = fetchOrCreateMasterCv(userId);

        
        if (r.getFullName() != null) cv.setFullName(r.getFullName());
        if (r.getHeadline() != null) cv.setHeadline(r.getHeadline());
        if (r.getEmail() != null) cv.setEmail(r.getEmail());
        if (r.getPhone() != null) cv.setPhone(r.getPhone());
        if (r.getLinkedInUrl() != null) cv.setLinkedInUrl(r.getLinkedInUrl());
        if (r.getGithubUrl() != null) cv.setGithubUrl(r.getGithubUrl());
        if (r.getSummary() != null) cv.setSummary(r.getSummary());
        if (r.getLanguage() != null) cv.setLanguage(r.getLanguage());

        return cvMapper.toMasterCvResponse(masterCvRepository.save(cv));
    }

    

    @Transactional
    public ExperienceResponse addExperience(UUID userId, CreateExperienceRequest r) {
        MasterCv cv = fetchOrCreateMasterCv(userId);

        Experience e = new Experience();
        e.setMasterCv(cv);
        e.setPosition(r.getPosition());
        e.setCompany(r.getCompany());
        e.setLocation(r.getLocation());
        e.setStartDate(r.getStartDate());
        e.setEndDate(r.getEndDate());
        e.setDescription(r.getDescription());

        return cvMapper.toExperienceResponse(experienceRepository.save(e));
    }

    @Transactional
    public ExperienceResponse updateExperience(UUID userId, UUID id, UpdateExperienceRequest r) {
        Experience e = experienceRepository.findByIdAndMasterCv_User_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found: " + id));

        if (r.getPosition() != null) e.setPosition(r.getPosition());
        if (r.getCompany() != null) e.setCompany(r.getCompany());
        if (r.getLocation() != null) e.setLocation(r.getLocation());
        if (r.getStartDate() != null) e.setStartDate(r.getStartDate());
        if (r.getEndDate() != null) e.setEndDate(r.getEndDate());
        if (r.getDescription() != null) e.setDescription(r.getDescription());

        return cvMapper.toExperienceResponse(experienceRepository.save(e));
    }

    @Transactional
    public void deleteExperience(UUID userId, UUID id) {
        Experience e = experienceRepository.findByIdAndMasterCv_User_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found: " + id));
        experienceRepository.delete(e);
    }

    

    @Transactional
    public EducationResponse addEducation(UUID userId, CreateEducationRequest r) {
        MasterCv cv = fetchOrCreateMasterCv(userId);

        Education e = new Education();
        e.setMasterCv(cv);
        e.setInstitution(r.getInstitution());
        e.setDegree(r.getDegree());
        e.setFieldOfStudy(r.getFieldOfStudy());
        e.setLocation(r.getLocation());
        e.setStartDate(r.getStartDate());
        e.setEndDate(r.getEndDate());
        e.setDescription(r.getDescription());

        return cvMapper.toEducationResponse(educationRepository.save(e));
    }

    @Transactional
    public EducationResponse updateEducation(UUID userId, UUID id, UpdateEducationRequest r) {
        Education e = educationRepository.findByIdAndMasterCv_User_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Education not found: " + id));

        if (r.getInstitution() != null) e.setInstitution(r.getInstitution());
        if (r.getDegree() != null) e.setDegree(r.getDegree());
        if (r.getFieldOfStudy() != null) e.setFieldOfStudy(r.getFieldOfStudy());
        if (r.getLocation() != null) e.setLocation(r.getLocation());
        if (r.getStartDate() != null) e.setStartDate(r.getStartDate());
        if (r.getEndDate() != null) e.setEndDate(r.getEndDate());
        if (r.getDescription() != null) e.setDescription(r.getDescription());

        return cvMapper.toEducationResponse(educationRepository.save(e));
    }

    @Transactional
    public void deleteEducation(UUID userId, UUID id) {
        Education e = educationRepository.findByIdAndMasterCv_User_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Education not found: " + id));
        educationRepository.delete(e);
    }

    

    @Transactional
    public SkillResponse addSkill(UUID userId, AddSkillRequest r) {
        MasterCv cv = fetchOrCreateMasterCv(userId);

        // Перевірка чи скіл новий у каталозі — для одноразової генерації embedding.
        // Race: два юзери одночасно додають "java" — обидва бачать isNewSkill=true,
        // findOrCreate створить лише один (через UNIQUE), embedding згенерується двічі
        // (overwrite). Прийнятний tradeoff — explicit lock був би overkill.
        boolean isNewSkill = !skillRepository.existsByName(r.getName().toLowerCase().trim());
        Skill skill = skillService.findOrCreateSkill(r.getName(), r.getCategory());

        // Defensive: один скіл двічі додати не можна (UK у БД, але читабельніше тут).
        masterCvSkillRepository.findByMasterCvIdAndSkillId(cv.getId(), skill.getId())
                .ifPresent(existing -> {
                    throw new ConflictException("Skill already in CV: " + skill.getName());
                });

        MasterCvSkill mcs = new MasterCvSkill();
        mcs.setMasterCv(cv);
        mcs.setSkill(skill);
        mcs.setLevel(r.getLevel());
        MasterCvSkill saved = masterCvSkillRepository.save(mcs);

        // Embedding generation ПІСЛЯ commit основної транзакції. Уникаємо race,
        // де async-метод читає скіл, який ще не закомічений у БД.
        if (isNewSkill) {
            UUID skillId = skill.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    skillService.triggerEmbeddingGeneration(skillId);
                }
            });
        }

        return cvMapper.toSkillResponse(saved);
    }

    @Transactional
    public SkillResponse updateSkillLevel(UUID userId, UUID masterCvSkillId, UpdateSkillRequest r) {
        MasterCvSkill mcs = masterCvSkillRepository.findByIdAndMasterCv_User_Id(masterCvSkillId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not in CV: " + masterCvSkillId));

        // У UpdateSkillRequest лише level. Якщо null — нічого не міняємо.
        if (r.getLevel() != null) mcs.setLevel(r.getLevel());

        return cvMapper.toSkillResponse(masterCvSkillRepository.save(mcs));
    }

    @Transactional
    public void removeSkill(UUID userId, UUID masterCvSkillId) {
        MasterCvSkill mcs = masterCvSkillRepository.findByIdAndMasterCv_User_Id(masterCvSkillId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not in CV: " + masterCvSkillId));
        masterCvSkillRepository.delete(mcs);
        // Сам Skill у shared catalog не чіпається — інші юзери можуть його юзати.
    }

    
    @Transactional
    public ProjectResponse addProject(UUID userId, CreateProjectRequest r) {
        MasterCv cv = fetchOrCreateMasterCv(userId);

        Project p = new Project();
        p.setMasterCv(cv);
        p.setName(r.getName());
        p.setDescription(r.getDescription());
        p.setUrl(r.getUrl());
        p.setTechnologies(r.getTechnologies());
        p.setStartDate(r.getStartDate());
        p.setEndDate(r.getEndDate());

        return cvMapper.toProjectResponse(projectRepository.save(p));
    }

    @Transactional
    public ProjectResponse updateProject(UUID userId, UUID id, UpdateProjectRequest r) {
        Project p = projectRepository.findByIdAndMasterCv_User_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        if (r.getName() != null) p.setName(r.getName());
        if (r.getDescription() != null) p.setDescription(r.getDescription());
        if (r.getUrl() != null) p.setUrl(r.getUrl());
        if (r.getTechnologies() != null) p.setTechnologies(r.getTechnologies());
        if (r.getStartDate() != null) p.setStartDate(r.getStartDate());
        if (r.getEndDate() != null) p.setEndDate(r.getEndDate());

        return cvMapper.toProjectResponse(projectRepository.save(p));
    }

    @Transactional
    public void deleteProject(UUID userId, UUID id) {
        Project p = projectRepository.findByIdAndMasterCv_User_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        projectRepository.delete(p);
    }

    
    @Transactional
    public LanguageResponse addLanguage(UUID userId, CreateLanguageRequest r) {
        MasterCv cv = fetchOrCreateMasterCv(userId);

        Language l = new Language();
        l.setMasterCv(cv);
        l.setName(r.getName());
        l.setLevel(r.getLevel());

        return cvMapper.toLanguageResponse(languageRepository.save(l));
    }

    @Transactional
    public LanguageResponse updateLanguage(UUID userId, UUID id, UpdateLanguageRequest r) {
        Language l = languageRepository.findByIdAndMasterCv_User_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Language not found: " + id));

        if (r.getName() != null) l.setName(r.getName());
        if (r.getLevel() != null) l.setLevel(r.getLevel());

        return cvMapper.toLanguageResponse(languageRepository.save(l));
    }

    @Transactional
    public void deleteLanguage(UUID userId, UUID id) {
        Language l = languageRepository.findByIdAndMasterCv_User_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Language not found: " + id));
        languageRepository.delete(l);
    }

    
    private MasterCv fetchOrCreateMasterCv(UUID userId) {
        return masterCvRepository.findByUserId(userId)
                .orElseGet(() -> {
                    MasterCv cv = new MasterCv();
                    cv.setUser(userRepository.getReferenceById(userId));
                    cv.setLanguage(CvLanguage.EN);
                    return masterCvRepository.save(cv);
                });
    }
}