package com.jobtracker.backendJobTracker.interview.notes;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jobtracker.backendJobTracker.interview.notes.dto.InterviewNoteResponse;



@Mapper(componentModel = "spring")
public interface NoteMapper {
    @Mapping(target = "interviewPrepId", source = "interviewPrep.id")
    InterviewNoteResponse toInterviewNoteResponse(InterviewNote note);

}
