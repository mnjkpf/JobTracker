package com.jobtracker.backendJobTracker.application.parsing.dto;

import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedJobPosting {
 
    private String position;
    private String companyName;
    private String description;
 
    private Seniority seniority;
    private ContractType contractType;
    private WorkMode workMode;
 
    private String location;
 
    private Integer salaryMin;
    private Integer salaryMax;
    private String salaryCurrency;
 
    private boolean parsedByLlm;
 
    /**
     * Чи витягнуто мінімум (position + company). Якщо ні — orchestrator піде
     * на LLM fallback або попросить manual paste.
     */
    public boolean hasMinimumData() {
        return position != null && !position.isBlank()
                && companyName != null && !companyName.isBlank();
    }
}

