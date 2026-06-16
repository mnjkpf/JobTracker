package com.jobtracker.backendJobTracker.interview.notes;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.jobtracker.backendJobTracker.ai.EmbeddingService;
import com.jobtracker.backendJobTracker.interview.notes.dto.SimilarInterviewNote;
import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;

import lombok.RequiredArgsConstructor;

/**
 * Native pgvector similarity search для нотаток. Інфраструктура для RAG (7C).
 * <p>
 * Не використовується у 7B — пишемо заздалегідь щоб InterviewRagService у 7C
 * просто інжектив і викликав.
 */
@Service
@RequiredArgsConstructor
public class InterviewNoteSimilaritySearch {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterviewNoteSimilaritySearch.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    
    public List<SimilarInterviewNote> findSimilar(UUID userId, String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        float[] queryVector = embeddingService.getEmbedding(query);
        if (queryVector == null || queryVector.length == 0) {
            LOGGER.warn("Could not generate embedding for query: {}", query);
            return List.of();
        }

        String vectorLiteral = toVectorLiteral(queryVector);

        
        String sql = """
                SELECT n.id, n.content, n.note_type, ip.application_id,
                       (n.embedding <=> CAST(? AS vector)) AS distance
                FROM interview_notes n
                JOIN interview_preps ip ON ip.id = n.interview_prep_id
                JOIN applications a ON a.id = ip.application_id
                WHERE a.user_id = ?
                  AND n.embedding IS NOT NULL
                ORDER BY distance ASC
                LIMIT ?
                """;

        
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> mapRow(rs),
                vectorLiteral,
                userId,           
                limit);
    }

    
    private SimilarInterviewNote mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        SimilarInterviewNote r = new SimilarInterviewNote();
        r.setId(rs.getObject("id", UUID.class));
        r.setContent(rs.getString("content"));                       // не "name"
        r.setApplicationId(rs.getObject("application_id", UUID.class));

        String noteTypeStr = rs.getString("note_type");              // не "NoteType"
        r.setNoteType(noteTypeStr == null ? null : NoteType.valueOf(noteTypeStr));

        // cosine distance 0..2 → similarity = 1 - distance, clamp у [0, 1]
        double distance = rs.getDouble("distance");
        double similarity = Math.max(0.0, Math.min(1.0, 1.0 - distance));
        r.setSimilarity(similarity);

        return r;
    }

    
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