package com.jobtracker.backendJobTracker.cv.dto.parse;

import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
import com.jobtracker.backendJobTracker.cv.enums.SkillLevel;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ParsedSkill {
    private String name;
    private SkillCategory category;
    private SkillLevel level;
}
