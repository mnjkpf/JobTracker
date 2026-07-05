package com.jobtracker.backendJobTracker.application;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jobtracker.backendJobTracker.application.dto.ApplicationResponse;
import com.jobtracker.backendJobTracker.application.dto.ApplicationStatusHistoryResponse;
import com.jobtracker.backendJobTracker.application.dto.ApplicationSummaryResponse;
import com.jobtracker.backendJobTracker.application.dto.CreateApplicationRequest;



@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    @Mapping(target = "companyName", source = "company.name")
    ApplicationResponse toResponse(Application application);

    // companyName беремо з company.name (LAZY, але list() читає у read-only транзакції).
    @Mapping(target = "companyName", source = "company.name")
    ApplicationSummaryResponse toApplicationSummaryResponse(Application application);

    Application toEntity(CreateApplicationRequest request);

    ApplicationStatusHistoryResponse toStatusHistoryResponse(ApplicationStatusHistory history);

}
