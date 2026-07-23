package com.jobtracker.backendJobTracker.interview.dto.parse;

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
public class ParsedQuestion {

    
    private String question;

    
    private String suggestedAnswer;
}