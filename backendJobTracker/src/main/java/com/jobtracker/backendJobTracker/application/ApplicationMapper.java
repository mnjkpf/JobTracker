package com.jobtracker.backendJobTracker.application;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import com.jobtracker.backendJobTracker.application.dto.ApplicationStatusHistoryResponse;
import com.jobtracker.backendJobTracker.application.dto.ApplicationSummaryResponse;
import com.jobtracker.backendJobTracker.application.dto.StatusSummary;
import com.jobtracker.backendJobTracker.status.StatusCategory;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "companyName", source = "company.name")
    ApplicationResponse toResponse(Application application);

    @Mapping(target = "companyName", source = "company.name")
    ApplicationSummaryResponse toApplicationSummaryResponse(Application application);

    ApplicationStatusHistoryResponse toStatusHistoryResponse(ApplicationStatusHistory history);

    // Used implicitly by MapStruct for the nested `status` property.
    StatusSummary toStatusSummary(StatusCategory status);
}
