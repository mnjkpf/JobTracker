package com.jobtracker.backendJobTracker.application.ai;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
 
import lombok.RequiredArgsConstructor;

/**
 * Обгортка над Spring AI ChatClient. Єдина точка LLM-викликів: кеш, логування,
 * (у майбутньому) circuit breaker і cost tracking. Інші сервіси ходять до LLM
 * тільки через цей клас.
 */
@Service
@RequiredArgsConstructor
public class AiService {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(AiService.class);
 
    private final ChatClient.Builder chatClientBuilder;
    private final AiResponseCache cache;
 
    /**
     * Викликає LLM з промптом. Кешує за SHA-256 промпту — однаковий промпт = одна оплата.
     */
    public String complete(String prompt) {
        return cache.get(prompt).orElseGet(() -> {
            LOGGER.debug("AI cache miss, calling LLM (prompt length: {})", prompt.length());
 
            ChatClient client = chatClientBuilder.build();
            String response = client.prompt()
                    .user(prompt)
                    .call()
                    .content();
 
            if (response != null && !response.isBlank()) {
                cache.put(prompt, response);
            }
 
            return response;
        });
    }
}
