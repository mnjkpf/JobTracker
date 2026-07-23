package com.jobtracker.backendJobTracker.cv.dto;

import com.jobtracker.backendJobTracker.cv.enums.SkillLevel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSkillRequest {
 
    private SkillLevel level;
}
