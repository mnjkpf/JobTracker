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
public class AiResponseCache {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(AiResponseCache.class);
    private static final String KEY_PREFIX = "ai:extract:";
    private static final Duration TTL = Duration.ofDays(7);
 
    private final StringRedisTemplate redisTemplate;
 
    public Optional<String> get(String input) {
        String key = buildKey(input);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                LOGGER.debug("AI cache HIT for key {}", key);
                return Optional.of(cached);
            }
        } catch (Exception e) {
            LOGGER.warn("AI cache read failed: {}", e.getMessage());
        }
        return Optional.empty();
    }
 
    public void put(String input, String response) {
        String key = buildKey(input);
        try {
            redisTemplate.opsForValue().set(key, response, TTL);
            LOGGER.debug("AI cache PUT for key {}", key);
        } catch (Exception e) {
            LOGGER.warn("AI cache write failed: {}", e.getMessage());
        }
    }
 
    private String buildKey(String input) {
        return KEY_PREFIX + HashUtil.sha256(input);
    }
}
 

