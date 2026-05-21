package com.jobtracker.backendJobTracker.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private String displayName;
    private String refreshToken;
    
}
