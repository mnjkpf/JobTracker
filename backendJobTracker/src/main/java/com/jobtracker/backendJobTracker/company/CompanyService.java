package com.jobtracker.backendJobTracker.company;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.company.dto.CompanyResponse;
import com.jobtracker.backendJobTracker.company.dto.CreateCompanyRequest;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Transactional
    public final boolean createCompany(CreateCompanyRequest request) {
        if (companyRepository.findByName(request.getName()).isPresent()) {
            return false; // Company with the same name already exists
        }

        Company company = new Company();
        company.setName(request.getName());
        company.setWebsite(request.getWebsite());
        company.setIndustry(request.getIndustry());
        company.setDescription(request.getDescription());
        company.setSize(request.getSize());

        companyRepository.save(company);
        return true;
    }

    @Transactional
    public final boolean deleteCompany(String name) {
        return companyRepository.findByName(name)
                .map(company -> {
                    companyRepository.delete(company);
                    return true;
                })
                .orElse(false); // Company not found
    }

    @Transactional
    public final boolean updateCompany(String name, CreateCompanyRequest request) {
        return companyRepository.findByName(name)
                .map(company -> {
                    company.setWebsite(request.getWebsite());
                    company.setIndustry(request.getIndustry());
                    company.setDescription(request.getDescription());
                    company.setSize(request.getSize());
                    companyRepository.save(company);
                    return true;
                })
                .orElse(false); // Company not found
    }

    @Transactional
    public final CompanyResponse getCompanyByName(String name){
        return companyRepository.findByName(name)
                .map(company -> {
                    CompanyResponse response = new CompanyResponse();
                    response.setId(company.getId().toString());
                    response.setName(company.getName());
                    response.setWebsite(company.getWebsite());
                    response.setIndustry(company.getIndustry());
                    response.setDescription(company.getDescription());
                    response.setSize(company.getSize());
                    return response;
                })
                .orElse(null); // Company not found
    }

    @Transactional
    public final List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(company -> {
                    CompanyResponse response = new CompanyResponse();
                    response.setId(company.getId().toString());
                    response.setName(company.getName());
                    response.setWebsite(company.getWebsite());
                    response.setIndustry(company.getIndustry());
                    response.setDescription(company.getDescription());
                    response.setSize(company.getSize());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CompanyResponse> getCompaniesByIndustry(String industry) {
        return companyRepository.findByIndustry(industry).stream()
                .map(company -> {
                    CompanyResponse response = new CompanyResponse();
                    response.setId(company.getId().toString());
                    response.setName(company.getName());
                    response.setWebsite(company.getWebsite());
                    response.setIndustry(company.getIndustry());
                    response.setDescription(company.getDescription());
                    response.setSize(company.getSize());
                    return response;
                })
                .collect(Collectors.toList());
    }
    @Transactional
    public List<CompanyResponse> getCompaniesBySize(CampanySize size) {
        return companyRepository.findByCompanySize(size).stream()
                .map(company -> {
                    CompanyResponse response = new CompanyResponse();
                    response.setId(company.getId().toString());
                    response.setName(company.getName());
                    response.setWebsite(company.getWebsite());
                    response.setIndustry(company.getIndustry());
                    response.setDescription(company.getDescription());
                    response.setSize(company.getSize());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CompanyResponse> getCompaniesByUser(UUID id) {
        User user = userRepository.findById(id).orElse(null);
        return companyRepository.findByUser(user).stream()
                .map(company -> {
                    CompanyResponse response = new CompanyResponse();
                    response.setId(company.getId().toString());
                    response.setName(company.getName());
                    response.setWebsite(company.getWebsite());
                    response.setIndustry(company.getIndustry());
                    response.setDescription(company.getDescription());
                    response.setSize(company.getSize());
                    return response;
                })
                .collect(Collectors.toList());
    }



    



}
