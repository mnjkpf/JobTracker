package com.jobtracker.backendJobTracker.application.parsing.dto;

import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParseUrlPreviewResponse {

    private String url;
    private String position;
    private String companyName;
    private String description;
    private String location;
    private Integer salaryMin;
    private Integer salaryMax;
    private String salaryCurrency;
    private ContractType contractType;
    private Seniority seniority;
    private WorkMode workMode;
    private SourceBoard sourceBoard;
 
    // Метадані для UX
    /** true якщо дані з LLM (AI може помилитись — більш ретельно перевір). */
    private boolean parsedByLlm;
 
    /** Підказка для юзера на формі — короткий human-readable hint. */
    private String hint;


}
