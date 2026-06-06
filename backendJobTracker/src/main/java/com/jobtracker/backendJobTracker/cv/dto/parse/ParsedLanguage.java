package com.jobtracker.backendJobTracker.cv.dto.parse;
import com.jobtracker.backendJobTracker.cv.enums.LanguageLevel;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ParsedLanguage {
    private String name;
    private LanguageLevel level;
}
