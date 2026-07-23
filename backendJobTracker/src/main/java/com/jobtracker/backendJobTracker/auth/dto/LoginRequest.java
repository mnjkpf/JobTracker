package com.jobtracker.backendJobTracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class LoginRequest {
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Password cannot be blank")
    private String password;
    private String displayName;
}
