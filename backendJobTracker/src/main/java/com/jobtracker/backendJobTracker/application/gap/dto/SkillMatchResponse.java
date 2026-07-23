package com.jobtracker.backendJobTracker.application.gap.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillMatchResponse {
    private String requiredSkillName;
    private UUID requiredSkillId;
    private boolean matched;
    private String matchedSkillName; // null if not matched
    private double similarity; // 0..1, how closely the matched skill corresponds to the required one. 1 = exact match.
    private boolean required; // true = must-have, false = nice-to-have. Copied from ApplicationSkill.required for convenience.
    
}
