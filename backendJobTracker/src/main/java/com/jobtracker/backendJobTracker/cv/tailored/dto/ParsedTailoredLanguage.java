package com.jobtracker.backendJobTracker.cv.tailored.dto;
import com.jobtracker.backendJobTracker.cv.enums.LanguageLevel;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedTailoredLanguage {
    private String name;
    private LanguageLevel level;
}

