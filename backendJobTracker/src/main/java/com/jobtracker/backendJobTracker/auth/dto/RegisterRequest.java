package com.jobtracker.backendJobTracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Password cannot be blank")
    private String password;
    @NotBlank(message = "Display name cannot be blank")
    private String displayName;
}
