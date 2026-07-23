package com.jobtracker.backendJobTracker.cv;

import java.util.List;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.cv.dto.SimilarSkillResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillsController {
 
    private final SkillSimilaritySearch skillSimilaritySearch;
 
    /**
     * Semantic search: знаходить скіли близькі за змістом до query.
     * "java" → Java, Kotlin, Spring, Hibernate
     * "frontend" → React, Vue, Angular, TypeScript
     */
    @GetMapping("/search")
    public List<SimilarSkillResponse> search(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return skillSimilaritySearch.findSimilar(q, limit);
    }
}

