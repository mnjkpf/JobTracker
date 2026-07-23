package com.jobtracker.backendJobTracker.cv;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jobtracker.backendJobTracker.cv.dto.EducationResponse;
import com.jobtracker.backendJobTracker.cv.dto.ExperienceResponse;
import com.jobtracker.backendJobTracker.cv.dto.LanguageResponse;
import com.jobtracker.backendJobTracker.cv.dto.MasterCvResponse;
import com.jobtracker.backendJobTracker.cv.dto.MasterCvSummaryResponse;
import com.jobtracker.backendJobTracker.cv.dto.ProjectResponse;
import com.jobtracker.backendJobTracker.cv.dto.SkillResponse;
import com.jobtracker.backendJobTracker.cv.models.Education;
import com.jobtracker.backendJobTracker.cv.models.Experience;
import com.jobtracker.backendJobTracker.cv.models.Language;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.models.MasterCvSkill;
import com.jobtracker.backendJobTracker.cv.models.Project;


@Mapper(componentModel = "spring")
public interface CvMapper {

    @Mapping(target = "experiences", ignore = true)
    @Mapping(target = "educations", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "languages", ignore = true)
    MasterCvResponse toMasterCvResponse(MasterCv masterCv);

    MasterCvSummaryResponse toMasterCvSummaryResponse(MasterCv masterCv);

    ExperienceResponse toExperienceResponse(Experience experience);

    EducationResponse toEducationResponse(Education education);

    ProjectResponse toProjectResponse(Project project);

    LanguageResponse toLanguageResponse(Language language);

   
    @Mapping(target = "id", source = "id")              // id з junction
    @Mapping(target = "skillId", source = "skill.id")   // id з shared catalog
    @Mapping(target = "name", source = "skill.name")
    @Mapping(target = "category", source = "skill.category")
    @Mapping(target = "level", source = "level")
    SkillResponse toSkillResponse(MasterCvSkill masterCvSkill);
}