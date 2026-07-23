package com.jobtracker.backendJobTracker.cv.tailored;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvSummaryResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredEducationResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredExperienceResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredLanguageResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredProjectResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredSkillResponse;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredCv;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredEducation;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredExperience;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredLanguage;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredProject;
import com.jobtracker.backendJobTracker.cv.tailored.models.TailoredSkill;

@Mapper(componentModel = "spring")
public interface TailoredCvMapper {
 
    @Mapping(target = "applicationId", source = "application.id")
    @Mapping(target = "experiences", ignore = true)
    @Mapping(target = "educations", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "languages", ignore = true)
    TailoredCvResponse toResponse(TailoredCv cv);
 
    TailoredCvSummaryResponse toSummaryResponse(TailoredCv cv);
 
    TailoredExperienceResponse toExperienceResponse(TailoredExperience e);
    TailoredEducationResponse toEducationResponse(TailoredEducation e);
    TailoredSkillResponse toSkillResponse(TailoredSkill s);
    TailoredProjectResponse toProjectResponse(TailoredProject p);
    TailoredLanguageResponse toLanguageResponse(TailoredLanguage l);
}

