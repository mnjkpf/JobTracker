package com.jobtracker.backendJobTracker.cv.tailored.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class GenerateTailoredCvRequest {
 
    /**
     * Опціональні підказки: "акцент на Spring", "коротше", "не згадуй cashier job".
     */
    @Size(max = 1000)
    private String emphasisInstructions;
}
