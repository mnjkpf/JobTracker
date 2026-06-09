package com.jobtracker.backendJobTracker.application.gap;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.application.gap.dto.GapAnalysisResponse;
import com.jobtracker.backendJobTracker.application.gap.dto.SkillMatchResponse;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
 
import lombok.RequiredArgsConstructor;
 
/**
 * Orchestrator gap analysis.
 * <p>
 * <b>Розподіл праці:</b>
 * <ul>
 * <li>POST /gap-analysis → {@link #runAnalysis} — LLM extraction + matching. Повільно (~5 сек).</li>
 * <li>GET  /gap-analysis → {@link #getAnalysis} — лише matching проти збережених ApplicationSkills. Швидко.</li>
 * </ul>
 * <p>
 * <b>Matching через pgvector:</b> для кожного required skill знаходимо у CV
 * юзера найближчий за cosine distance. Якщо similarity ≥ THRESHOLD → match.
 * Завдяки embeddings "Spring" у вакансії matchиться з "Spring Boot" у CV.
 */
@Service
@RequiredArgsConstructor
public class GapAnalysisService {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(GapAnalysisService.class);
 
    /**
     * Поріг для "матчу". Емпіричний. 0.75 → треба досить сильна схожість.
     * Винести у application.properties якщо буде потреба тюнити.
     */
    private static final double MATCH_THRESHOLD = 0.75;
 
    private final ApplicationRepository applicationRepository;
    private final ApplicationSkillRepository applicationSkillRepository;
    private final MasterCvRepository masterCvRepository;
    private final JobSkillExtractor jobSkillExtractor;
    private final JdbcTemplate jdbcTemplate;
 
    
 
    /**
     * Full pipeline: LLM витягує required skills з опису вакансії,
     * зберігає у application_skills, рахує matching, повертає response.
     */
    @Transactional
    public GapAnalysisResponse runAnalysis(UUID userId, UUID applicationId) {
        Application app = fetchOwned(userId, applicationId);
        ensureUserHasCv(userId);
 
        // 1. LLM call + save ApplicationSkills (delete-then-insert pattern всередині).
        jobSkillExtractor.extractAndSaveSkills(app);
 
        // 2. Matching проти CV
        return computeAnalysis(userId, app);
    }
 
    /**
     * Лише matching — без LLM. Швидко. Працює якщо ApplicationSkills уже існують
     * (тобто runAnalysis викликали раніше).
     */
    public GapAnalysisResponse getAnalysis(UUID userId, UUID applicationId) {
        Application app = fetchOwned(userId, applicationId);
        ensureUserHasCv(userId);
        return computeAnalysis(userId, app);
    }
 
    
    // MATCHING LOGIC
    
 
    private GapAnalysisResponse computeAnalysis(UUID userId, Application app) {
        List<ApplicationSkill> jobSkills = applicationSkillRepository.findByApplicationId(app.getId());
 
        List<SkillMatchResponse> matched = new ArrayList<>();
        List<SkillMatchResponse> missing = new ArrayList<>();
 
        for (ApplicationSkill js : jobSkills) {
            SkillMatchResponse match = findBestCvMatch(userId, js);
            if (match.isMatched()) {
                matched.add(match);
            } else {
                missing.add(match);
            }
        }
 
        // Score рахуємо ЛИШЕ по required (must-have) — nice-to-have бонус, не критичні.
        // Якщо у вакансії нема required → score 1.0 (vacuously perfect).
        long totalRequired = jobSkills.stream().filter(ApplicationSkill::isRequired).count();
        long matchedRequired = matched.stream().filter(SkillMatchResponse::isRequired).count();
        double score = totalRequired == 0 ? 1.0 : (double) matchedRequired / totalRequired;
 
        GapAnalysisResponse response = new GapAnalysisResponse();
        response.setScore(score);
        response.setTotalRequiredSkills((int) totalRequired);
        response.setMatchedRequiredSkills((int) matchedRequired);
        response.setMissedRequiredSkills((int) (totalRequired - matchedRequired));
        response.setMatchedSkills(matched);
        response.setMissingSkills(missing);
        response.setAnalyzeAt(Instant.now());
        return response;
    }
 
    /**
     * Знаходить найближчий CV-скіл для одного job skill.
     * <p>
     * SQL: WITH req — витягуємо embedding required-скіла один раз. Потім cosine
     * distance до кожного CV-скіла юзера. ORDER BY distance + LIMIT 1 → найкращий.
     * HNSW index на skills.embedding робить це швидко.
     */
    private SkillMatchResponse findBestCvMatch(UUID userId, ApplicationSkill jobSkill) {
        UUID jobSkillId = jobSkill.getSkill().getId();
 
        String sql = """
                WITH req AS (
                    SELECT embedding FROM skills WHERE id = ? AND embedding IS NOT NULL
                )
                SELECT cv_s.name AS cv_skill_name,
                       req.embedding <=> cv_s.embedding AS distance
                FROM req, master_cv_skills mcs
                JOIN master_cvs mc ON mc.id = mcs.master_cv_id
                JOIN skills cv_s ON cv_s.id = mcs.skill_id
                WHERE mc.user_id = ?
                  AND cv_s.embedding IS NOT NULL
                ORDER BY distance ASC
                LIMIT 1
                """;
 
        List<MatchRow> rows = jdbcTemplate.query(sql,
                (rs, rowNum) -> new MatchRow(
                        rs.getString("cv_skill_name"),
                        rs.getDouble("distance")),
                jobSkillId, userId);
 
        SkillMatchResponse response = new SkillMatchResponse();
        response.setRequiredSkillId(jobSkillId);
        response.setRequiredSkillName(jobSkill.getSkill().getName());
        response.setRequired(jobSkill.isRequired());
 
        if (rows.isEmpty()) {
            // Або у юзера немає CV skills з embedding, або в required-скіла нема embedding
            // (можливо async ще не згенерувало). Маркуємо як miss.
            response.setMatched(false);
            return response;
        }
 
        MatchRow best = rows.get(0);
        // cosine distance 0..2 → similarity = 1 - distance, clamp у [0, 1].
        double similarity = Math.max(0.0, Math.min(1.0, 1.0 - best.distance));
 
        response.setSimilarity(similarity);
        response.setMatchedSkillName(best.cvSkillName);  // показуємо навіть якщо нижче порогу
        response.setMatched(similarity >= MATCH_THRESHOLD);
        return response;
    }
 
    

 
    private Application fetchOwned(UUID userId, UUID applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }
 
    private void ensureUserHasCv(UUID userId) {
        if (!masterCvRepository.existsByUserId(userId)) {
            throw new BusinessRuleException(
                    "Cannot run gap analysis: no CV found. Upload your CV or add skills first.");
        }
    }
 
    /** Internal row mapping helper. */
    private record MatchRow(String cvSkillName, double distance) {
    }
}
