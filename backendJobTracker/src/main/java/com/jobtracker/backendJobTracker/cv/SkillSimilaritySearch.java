package com.jobtracker.backendJobTracker.cv;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jobtracker.backendJobTracker.ai.EmbeddingService;
import com.jobtracker.backendJobTracker.cv.dto.SimilarSkillResponse;
import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SkillSimilaritySearch {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(SkillSimilaritySearch.class);
 
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
 
    public List<SimilarSkillResponse> findSimilar(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
 
        // 1. Embed query text. EmbeddingService сам кешує — повторні query безкоштовні.
        float[] queryVector = embeddingService.getEmbedding(query);
        if (queryVector == null || queryVector.length == 0) {
            LOGGER.warn("Could not generate embedding for query: {}", query);
            return List.of();
        }
 
        // 2. pgvector очікує літерал виду '[0.1,0.2,...]', CAST AS vector.
        //    Параметр передається ДВІЧІ (у SELECT і ORDER BY) — Postgres
        //    оптимізує і виконує вираз один раз.
        String vectorLiteral = toVectorLiteral(queryVector);
 
        String sql = """
                SELECT id, name, category,
                       embedding <=> CAST(? AS vector) AS distance
                FROM skills
                WHERE embedding IS NOT NULL
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;
 
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> mapRow(rs.getObject("id", UUID.class),
                                       rs.getString("name"),
                                       rs.getString("category"),
                                       rs.getDouble("distance")),
                vectorLiteral, vectorLiteral, limit);
    }
 
    private SimilarSkillResponse mapRow(UUID id, String name, String categoryStr, double distance) {
        SimilarSkillResponse r = new SimilarSkillResponse();
        r.setId(id);
        r.setName(name);
        // category може бути null у БД (CHECK дозволяє).
        r.setCategory(categoryStr == null ? null : SkillCategory.valueOf(categoryStr));
        // similarity = 1 - distance, clamp у [0, 1]. Cosine distance > 1 рідкісне
        // для нормалізованих embeddings, але захищаємось.
        double similarity = Math.max(0.0, Math.min(1.0, 1.0 - distance));
        r.setSimilarity(similarity);
        return r;
    }
 
    /** float[] → "[0.1234,0.5678,...]" pgvector літерал. */
    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
