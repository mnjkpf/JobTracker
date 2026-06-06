package com.jobtracker.backendJobTracker.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.jobtracker.backendJobTracker.config.JobTrackerProperties;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class EmbeddingService {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingService.class);
 
    private final AiClient aiClient;
    private final EmbeddingCache cache;                          
    private final JobTrackerProperties.Ai aiProperties;
 
    public float[] getEmbedding(String text) {
        // 1. Спершу кеш — той самий текст ніколи не пораховуємо двічі.
        return cache.get(text).orElseGet(() -> {
            LOGGER.debug("Embedding cache miss, calling LLM (text length: {})", text.length());
 
            // 2. Cache miss → реальний виклик OpenAI.
            float[] vector = aiClient.getEmbedding(
                    text,
                    aiProperties.embeddingModel(),
                    aiProperties.embeddingDimension());
 
            // 3. Записуємо у кеш для майбутніх викликів.
            if (vector != null && vector.length > 0) {
                cache.put(text, vector);
            }
            return vector;
        });
    }
}

