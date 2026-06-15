package com.jobtracker.backendJobTracker.interview.dto;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.interview.enums.InterviewPrepStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewPrepSummaryResponse {
        private UUID id;
    // ВИПРАВЛЕНО: applicationId, не full Application entity (та сама причина як у Response)
    private UUID applicationId;
    private InterviewPrepStatus status;
    private Integer questionCount;     // загалом по всіх категоріях
    private Instant createdAt;

}
