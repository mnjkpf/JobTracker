package com.jobtracker.backendJobTracker.status.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderStatusesRequest {

    @NotEmpty(message = "orderedIds must not be empty")
    private List<UUID> orderedIds;
}
