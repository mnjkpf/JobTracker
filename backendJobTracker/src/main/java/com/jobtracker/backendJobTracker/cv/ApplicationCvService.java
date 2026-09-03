package com.jobtracker.backendJobTracker.cv;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.application.dto.ApplicationCvMetaResponse;
import com.jobtracker.backendJobTracker.cv.models.ApplicationCv;
import com.jobtracker.backendJobTracker.cv.repo.ApplicationCvRepository;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationCvService {

    private final ApplicationCvRepository applicationCvRepository;
    private final ApplicationRepository applicationRepository;
    private final CvFileParser cvFileParser;
    private final CvFileValidation validation;

    @Transactional
    public ApplicationCvMetaResponse upload(UUID userId, UUID applicationId, MultipartFile file) {
        Application app = fetchOwned(userId, applicationId);
        validation.validate(file);

        String text;
        try {
            text = cvFileParser.extractText(file);
        } catch (Exception e) {
            throw new CvExtractionException("Failed to extract text from CV file.", e);
        }

        ApplicationCv cv = applicationCvRepository.findByApplicationId(applicationId)
                .orElseGet(ApplicationCv::new);
        cv.setApplicationId(app.getId());
        cv.setFileName(file.getOriginalFilename());
        cv.setContentType(file.getContentType());
        cv.setFileSize(file.getSize());
        try {
            cv.setFileBytes(file.getBytes());
        } catch (Exception e) {
            throw new CvExtractionException("Failed to read CV file bytes.", e);
        }
        cv.setExtractedText(text);

        return toMeta(applicationCvRepository.save(cv));
    }

    public ApplicationCvMetaResponse getMeta(UUID userId, UUID applicationId) {
        fetchOwned(userId, applicationId);
        return applicationCvRepository.findByApplicationId(applicationId).map(this::toMeta)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No CV attached to application: " + applicationId));
    }

    public ApplicationCv getDownload(UUID userId, UUID applicationId) {
        fetchOwned(userId, applicationId);
        return applicationCvRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No CV attached to application: " + applicationId));
    }

    @Transactional
    public void delete(UUID userId, UUID applicationId) {
        fetchOwned(userId, applicationId);
        if (!applicationCvRepository.existsByApplicationId(applicationId)) {
            throw new ResourceNotFoundException("No CV attached to application: " + applicationId);
        }
        applicationCvRepository.deleteByApplicationId(applicationId);
    }

    private Application fetchOwned(UUID userId, UUID applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found: " + applicationId));
    }

    private ApplicationCvMetaResponse toMeta(ApplicationCv cv) {
        ApplicationCvMetaResponse r = new ApplicationCvMetaResponse();
        r.setId(cv.getId());
        r.setFileName(cv.getFileName());
        r.setContentType(cv.getContentType());
        r.setFileSize(cv.getFileSize());
        r.setCreatedAt(cv.getCreatedAt());
        return r;
    }
}
