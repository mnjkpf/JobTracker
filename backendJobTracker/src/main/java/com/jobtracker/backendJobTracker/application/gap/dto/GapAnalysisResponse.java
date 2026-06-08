package com.jobtracker.backendJobTracker.application.gap.dto;

import java.time.Instant;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GapAnalysisResponse {

    private double score;
    private int totalRequiredSkills;
    private int matchedRequiredSkills;
    private int missedRequiredSkills;
    private Instant analyzeAt;

    private List<SkillMatchResponse> matchedSkills;
    private List<SkillMatchResponse> missingSkills;
}
