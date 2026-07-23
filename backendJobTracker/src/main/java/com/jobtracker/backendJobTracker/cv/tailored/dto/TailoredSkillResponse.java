package com.jobtracker.backendJobTracker.cv.tailored.dto;

import java.util.UUID;
 
import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
import com.jobtracker.backendJobTracker.cv.enums.SkillLevel;
 
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class TailoredSkillResponse {
    private UUID id;
    private String name;
    private SkillCategory category;
    private SkillLevel level;
}
