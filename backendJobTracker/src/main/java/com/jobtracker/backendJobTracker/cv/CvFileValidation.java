package com.jobtracker.backendJobTracker.cv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CvFileValidation {

    private static final Logger LOGGER = LoggerFactory.getLogger(CvFileValidation.class);

    
    private final Tika tika = new Tika();

    
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // docx
            "application/msword" // doc
    );

    public void validate(MultipartFile file) {
        // Порядок навмисний: від найдешевшої перевірки до найдорожчої.
        validatePresent(file);
        validateSize(file);
        validateExtension(file);
        validateRealType(file);
        LOGGER.debug("CV file passed validation: {}", file == null ? null : file.getOriginalFilename());
    }

    // 1) Файл узагалі є і не порожній.
    private void validatePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CvExtractionException("No file uploaded or file is empty.", null);
        }

    }

    
    private void validateSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new CvExtractionException("File size exceeds the maximum allowed limit of 5 MB.", null);
        }
    }

    private void validateExtension(MultipartFile file) {
        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new CvExtractionException("Unsupported file type.", null);
        }
    }

    // 4) Реальний тип за вмістом (Tika), а не за тим, що сказав клієнт.
    private void validateRealType(MultipartFile file) {
        
        try (InputStream in = file.getInputStream()) {
            String detected = tika.detect(in, file.getOriginalFilename());
            if (!ALLOWED_MIME_TYPES.contains(detected)) {
                throw new CvExtractionException("File content type is not allowed.", null);
            }
        } catch (IOException e) {
            LOGGER.error("Error validating CV file type: {}", e.getMessage());
            throw new CvExtractionException("Failed to validate CV file.", null);
        }
    }
}
