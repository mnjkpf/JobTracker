package com.jobtracker.backendJobTracker.coverletter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jobtracker.backendJobTracker.coverletter.dto.CoverLetterResponse;
import com.jobtracker.backendJobTracker.coverletter.dto.CoverLetterSummaryResponse;

@Mapper(componentModel = "spring")
public interface CoverLetterMapper {
 
    CoverLetterResponse toResponse(CoverLetter coverLetter);
 
    // contentPreview мапиться через експресію — беремо перші ~100 символів з content.
    // Якщо контент null — null повертається (на випадок BD з legacy даними).
    @Mapping(target = "contentPreview",
             expression = "java(coverLetter.getContent() == null ? null : "
                     + "coverLetter.getContent().length() > 100 "
                     + "? coverLetter.getContent().substring(0, 100) + \"...\" "
                     + ": coverLetter.getContent())")
    CoverLetterSummaryResponse toSummaryResponse(CoverLetter coverLetter);
}

