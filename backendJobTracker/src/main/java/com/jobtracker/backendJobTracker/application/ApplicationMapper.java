package com.jobtracker.backendJobTracker.application;

import org.mapstruct.Mapper;

import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import com.jobtracker.backendJobTracker.application.dto.ApplicationStatusHistoryResponse;
import com.jobtracker.backendJobTracker.application.dto.ApplicationSummaryResponse;
import com.jobtracker.backendJobTracker.application.dto.CreateApplicationRequest;



@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    ApplicationResponse toResponse(Application application);

    ApplicationSummaryResponse toApplicationSummaryResponse(Application application);

    Application toEntity(CreateApplicationRequest request);

    ApplicationStatusHistoryResponse toStatusHistoryResponse(ApplicationStatusHistory history);

}
