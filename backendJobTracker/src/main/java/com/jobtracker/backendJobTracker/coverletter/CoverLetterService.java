package com.jobtracker.backendJobTracker.coverletter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.coverletter.dto.CoverLetterResponse;
import com.jobtracker.backendJobTracker.coverletter.dto.CoverLetterSummaryResponse;
import com.jobtracker.backendJobTracker.coverletter.dto.GenerateCoverLetterRequest;
import com.jobtracker.backendJobTracker.coverletter.dto.RefineCoverLetterRequest;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoverLetterService {
 
    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterGenerator generator;
    private final ApplicationRepository applicationRepository;
    private final MasterCvRepository masterCvRepository;
    private final CoverLetterMapper mapper;
 
    
 
    @Transactional
    public CoverLetterResponse generate(UUID userId, UUID applicationId, GenerateCoverLetterRequest request) {
        Application application = fetchOwnedApplication(userId, applicationId);
        ensureJobHasDescription(application);
 
        MasterCv cv = masterCvRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Upload your CV first — cover letter needs context."));
 
        String content = generator.generate(cv, application, request.getTone(),
                request.getAdditionalInstructions());           // ВИПРАВЛЕНО: getAdditionalInstructions (не getExtraInstructions)
 
        CoverLetter coverLetter = new CoverLetter();
        coverLetter.setApplication(application);
        coverLetter.setContent(content);
        coverLetter.setTone(request.getTone());                  // ДОДАНО: tone обов'язковий у БД
        coverLetter.setVersion(nextVersion(applicationId));      // ВИПРАВЛЕНО: не завжди 1
        return mapper.toResponse(coverLetterRepository.save(coverLetter));
    }
 
    
    // REFINE нова версія на основі існуючої
 
    @Transactional
    public CoverLetterResponse refine(UUID userId, UUID applicationId,
                                       UUID coverLetterId, RefineCoverLetterRequest request) {
        // Tenant check + знайти існуючий letter
        CoverLetter existing = coverLetterRepository
                .findByIdAndApplication_User_Id(coverLetterId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cover letter not found: " + coverLetterId));
 
        String refinedContent = generator.refine(existing.getContent(), request.getInstruction());
 
        // ВИПРАВЛЕНО: створюємо НОВИЙ запис, не модифікуємо існуючий.
        // Immutable модель — історія всіх версій зберігається.
        CoverLetter newVersion = new CoverLetter();
        newVersion.setApplication(existing.getApplication());
        newVersion.setContent(refinedContent);
        newVersion.setTone(existing.getTone());                  // tone успадковується
        newVersion.setVersion(nextVersion(existing.getApplication().getId()));
        return mapper.toResponse(coverLetterRepository.save(newVersion));
    }
 
    
    
 
    public List<CoverLetterSummaryResponse> list(UUID userId, UUID applicationId) {
        // ВИПРАВЛЕНО: ownership check заявки. Раніше будь-хто з валідним JWT міг
        // отримати cover letters чужої заявки знаючи applicationId.
        fetchOwnedApplication(userId, applicationId);
 
        return coverLetterRepository.findByApplicationIdOrderByVersionDesc(applicationId).stream()
                .map(mapper::toSummaryResponse)
                .toList();
    }
 
    // ДОДАНО: бракувало getById
    public CoverLetterResponse getById(UUID userId, UUID coverLetterId) {
        CoverLetter cl = coverLetterRepository
                .findByIdAndApplication_User_Id(coverLetterId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cover letter not found: " + coverLetterId));
        return mapper.toResponse(cl);
    }
 
    // ДОДАНО: бракувало delete
    @Transactional
    public void delete(UUID userId, UUID coverLetterId) {
        CoverLetter cl = coverLetterRepository
                .findByIdAndApplication_User_Id(coverLetterId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cover letter not found: " + coverLetterId));
        coverLetterRepository.delete(cl);
    }
 
    
 
    /** version наступної генерації: latest+1, або 1 якщо ще нема. */
    private int nextVersion(UUID applicationId) {
        return coverLetterRepository.findTopByApplicationIdOrderByVersionDesc(applicationId)
                .map(cl -> cl.getVersion() + 1)
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
}

