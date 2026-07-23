package com.jobtracker.backendJobTracker.auth.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {

    private UUID id;
    private String email;
    private String displayName;
    private String role;
    private boolean isActive;
    private boolean isEmailVerified;

    
}
