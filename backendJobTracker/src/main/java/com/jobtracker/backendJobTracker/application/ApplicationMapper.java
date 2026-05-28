package com.jobtracker.backendJobTracker.application;

import org.mapstruct.Mapper;

import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import com.jobtracker.backendJobTracker.application.dto.ApplicationStatusHistoryResponse;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    ApplicationResponse toResponse(Application application);

    ApplicationStatusHistoryResponse toApplicationSummaryResponse(Application application);
}
