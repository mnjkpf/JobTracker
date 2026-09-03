package com.jobtracker.backendJobTracker.cv;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jobtracker.backendJobTracker.cv.dto.ApplicationCvMeta;
import com.jobtracker.backendJobTracker.cv.repo.ApplicationCvRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CvContextResolver {

    private final ApplicationCvRepository appCvRepository;

    /** Текст прикріпленого до заявки CV, якщо є і не порожній. */
    public Optional<String> overrideText(UUID applicationId) {
        return appCvRepository.findMetaByApplicationId(applicationId)
                .map(ApplicationCvMeta::extractedText)
                .filter(t -> t != null && !t.isBlank());
    }
}
