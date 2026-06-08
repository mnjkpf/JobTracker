package com.jobtracker.backendJobTracker.company;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jobtracker.backendJobTracker.company.dto.CompanyResponse;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    @Mapping(target = "companyName", source = "name")
    CompanyResponse toResponse(Company company);

}
