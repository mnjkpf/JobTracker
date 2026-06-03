package com.jobtracker.backendJobTracker.cv.dto;
import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
import com.jobtracker.backendJobTracker.cv.enums.SkillLevel;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddSkillRequest {
 
    @NotBlank
    @Size(max = 100)
    private String name;
 
    private SkillCategory category;   // опціонально — якщо не задано, юзер пізніше виправить
    private SkillLevel level;
}

