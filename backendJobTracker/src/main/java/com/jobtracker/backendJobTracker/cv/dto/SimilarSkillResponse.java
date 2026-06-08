package com.jobtracker.backendJobTracker.cv.dto;

import java.util.UUID;

import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimilarSkillResponse {
 
    private UUID id;
    private String name;
    private SkillCategory category;
    private Double similarity;
}
