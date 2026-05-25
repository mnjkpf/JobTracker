package com.jobtracker.backendJobTracker.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobtracker.backendJobTracker.auth.dto.LoginRequest;
import com.jobtracker.backendJobTracker.auth.dto.LoginResponse;
import com.jobtracker.backendJobTracker.auth.dto.RefreshTokenRequest;
import com.jobtracker.backendJobTracker.auth.dto.RegisterRequest;
import com.jobtracker.backendJobTracker.auth.dto.RequestPasswordResetRequest;
import com.jobtracker.backendJobTracker.auth.dto.RequestVerificationRequest;
import com.jobtracker.backendJobTracker.auth.dto.ResetPasswordRequest;
import com.jobtracker.backendJobTracker.auth.dto.VerifyEmailRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/auth/**")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;


    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public LoginResponse register( @Valid @RequestBody RegisterRequest request ) {
        
        
        return authService.register(request);
    }
    
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest refreshTrawRefreshTokenoken) {
        
        authService.logout(refreshTrawRefreshTokenoken);
    }

    // ─── Email verification ─────────────────────────────────────────
 
    /**
     * Юзер запитує (повторне) надсилання verification email.
     * 202 Accepted — операція асинхронна за змістом, ми приймаємо запит
     * і обіцяємо обробити; конкретний результат буде у вигляді email.
     */
    @PostMapping("/request-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestVerification(@Valid @RequestBody RequestVerificationRequest request) {
        emailVerificationService.requestVerification(request.getEmail());
    }
 
    /**
     * Юзер кликнув link у email і повертається з token.
     */
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.getToken());
    }
 
    // ─── Password reset ─────────────────────────────────────────────
 
    /**
     * Юзер забув пароль і просить link на reset.
     * 202 — як і verification, асинхронно за змістом, відповідь — email.
     * Завжди повертає 202 незалежно від існування юзера (enumeration захист).
     */
    @PostMapping("/request-password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody RequestPasswordResetRequest request) {
        passwordResetService.requestReset(request.getEmail());
    }
 
    /**
     * Юзер прислав token + новий пароль.
     */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    }

    
}
