package com.jobtracker.backendJobTracker.ai;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.jobtracker.backendJobTracker.util.HashUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmbeddingCache {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingCache.class);
    private static final String KEY_PREFIX = "ai:embedding:";
    private static final Duration TTL = Duration.ofDays(30);
 
    private final StringRedisTemplate redisTemplate;
 
    public Optional<float[]> get(String text) {
        String key = buildKey(text);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                LOGGER.debug("Embedding cache HIT for key {}", key);
                return Optional.of(deserialize(cached));
            }
        } catch (Exception e) {
            // Redis down / network glitch / парсинг впав — log і повертаємо empty,
            // EmbeddingService піде в LLM.
            LOGGER.warn("Embedding cache read failed: {}", e.getMessage());
        }
        return Optional.empty();
    }
 
    public void put(String text, float[] vector) {
        String key = buildKey(text);
        try {
            redisTemplate.opsForValue().set(key, serialize(vector), TTL);
            LOGGER.debug("Embedding cache PUT for key {} (dims={})", key, vector.length);
        } catch (Exception e) {
            LOGGER.warn("Embedding cache write failed: {}", e.getMessage());
        }
    }
 
    private String buildKey(String text) {
        return KEY_PREFIX + HashUtil.sha256(text);
    }
 
    /** float[] → "0.1234,0.5678,..." */
    private String serialize(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8);
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.toString();
    }
 
    /** "0.1234,0.5678,..." → float[] */
    private float[] deserialize(String csv) {
        String[] parts = csv.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i]);
        }
        return result;
    }
}

