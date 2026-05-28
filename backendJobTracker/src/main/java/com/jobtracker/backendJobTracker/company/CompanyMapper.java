package com.jobtracker.backendJobTracker.company;

import org.mapstruct.Mapper;

import com.jobtracker.backendJobTracker.company.dto.CompanyResponse;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    CompanyResponse toResponse(Company company);
}
