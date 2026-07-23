package com.jobtracker.backendJobTracker.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingService.class);
 
    private final EmbeddingModel embeddingModel;
    private final EmbeddingCache cache;
 
    public float[] getEmbedding(String text) {
        // 1. Кеш — той самий текст ніколи не пораховуємо двічі.
        return cache.get(text).orElseGet(() -> {
            LOGGER.debug("Embedding cache miss, calling LLM (text length: {})", text.length());
 
            // 2. Spring AI: embeddingModel.embed(text) → float[]
            //    Модель і dimensions беруться з application.properties.
            float[] vector = embeddingModel.embed(text);
 
            // 3. Зберігаємо в кеш на 30 днів.
            if (vector != null && vector.length > 0) {
                cache.put(text, vector);
            }
            return vector;
        });
    }
}
