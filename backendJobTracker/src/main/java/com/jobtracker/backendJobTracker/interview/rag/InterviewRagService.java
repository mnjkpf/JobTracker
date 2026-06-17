package com.jobtracker.backendJobTracker.interview.rag;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.gap.ApplicationSkill;
import com.jobtracker.backendJobTracker.application.gap.ApplicationSkillRepository;
import com.jobtracker.backendJobTracker.interview.notes.InterviewNoteSimilaritySearch;
import com.jobtracker.backendJobTracker.interview.notes.dto.SimilarInterviewNote;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewRagService {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(InterviewRagService.class);
 
    
    private static final int MAX_RESULTS = 10;
 
    
    private static final int MAX_DESCRIPTION_CHARS = 800;
 
   
    private static final double SIMILARITY_THRESHOLD = 0.70;
 
    private final InterviewNoteSimilaritySearch noteSimilaritySearch;
    private final ApplicationSkillRepository applicationSkillRepository;
 
    
    public List<SimilarInterviewNote> findRelevantPastNotes(UUID userId, Application app) {
        if (userId == null || app == null) {
            return List.of();
        }
 
        
        String queryText = buildQueryText(app);
        if (queryText.isBlank()) {
            LOGGER.debug("Empty query text for application {}, no RAG retrieval", app.getId());
            return List.of();
        }
 
        
        List<SimilarInterviewNote> rawResults = noteSimilaritySearch.findSimilar(
                userId, queryText, MAX_RESULTS);
 
        if (rawResults.isEmpty()) {
            LOGGER.debug("No past notes found for user {} (likely first interview)", userId);
            return List.of();
        }
 
        
        UUID currentAppId = app.getId();
        List<SimilarInterviewNote> filtered = rawResults.stream()
                .filter(n -> n.getSimilarity() >= SIMILARITY_THRESHOLD)
                .filter(n -> !currentAppId.equals(n.getApplicationId()))
                .collect(Collectors.toList());
 
        LOGGER.debug("RAG retrieval for app {}: {} raw → {} after filter (top similarity: {})",
                currentAppId,
                rawResults.size(),
                filtered.size(),
                filtered.isEmpty() ? "n/a" : String.format("%.2f", filtered.get(0).getSimilarity()));
 
        return filtered;
    }
 
   
    private String buildQueryText(Application app) {
        StringBuilder sb = new StringBuilder();
 
        if (notBlank(app.getName())) {
            sb.append("Position: ").append(app.getName()).append("\n");
        }
        if (app.getSeniority() != null) {
            sb.append("Seniority: ").append(app.getSeniority()).append("\n");
        }
 
        // Технології — з ApplicationSkill (зі стадії gap analysis).
        // Якщо gap analysis ще не запускалась — пропускаємо, fallback на description.
        List<ApplicationSkill> skills = applicationSkillRepository.findByApplicationId(app.getId());
        if (!skills.isEmpty()) {
            String techList = skills.stream()
                    .map(s -> s.getSkill().getName())
                    .collect(Collectors.joining(", "));
            sb.append("Technologies: ").append(techList).append("\n");
        }
 
        // Description — обмежений
        if (notBlank(app.getDescription())) {
            String desc = app.getDescription();
            if (desc.length() > MAX_DESCRIPTION_CHARS) {
                desc = desc.substring(0, MAX_DESCRIPTION_CHARS);
            }
            sb.append("Description: ").append(desc);
        }
 
        return sb.toString();
    }
 
    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
