package com.jobtracker.backendJobTracker.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobtracker.backendJobTracker.ai.AiQuotaService;
import com.jobtracker.backendJobTracker.auth.AuthService;
import com.jobtracker.backendJobTracker.auth.CustomUserDetails;
import com.jobtracker.backendJobTracker.auth.dto.LoginResponse;
import com.jobtracker.backendJobTracker.auth.dto.RegisterRequest;
import com.jobtracker.backendJobTracker.user.dto.AccountResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Операції над власним акаунтом, що вимагають авторизації.
 * <p>
 * Навмисно НЕ під {@code /api/v1/auth/**}: цей шлях виключений із
 * {@code JwtAuthenticationFilter.shouldNotFilter}, тому там
 * {@code @AuthenticationPrincipal} завжди був би null — а апгрейд гостя
 * мусить знати, який саме гість його викликав.
 */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;
    private final AiQuotaService aiQuotaService;

    /** Стан акаунта: гість чи ні, залишок AI-квоти, коли згорає сесія. */
    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.user();

        AccountResponse response = new AccountResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setGuest(user.isGuest());
        response.setGuestExpiresAt(user.getGuestExpiresAt());
        response.setAiGenerationsRemaining(aiQuotaService.remainingGenerations(user.getId()));
        return response;
    }

    /**
     * Реєстрація з гостьової сесії: той самий акаунт отримує email і пароль,
     * усі вже створені заявки та CV залишаються. Повертає нову пару токенів.
     */
    @PostMapping("/upgrade")
    public LoginResponse upgrade(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody RegisterRequest request) {
        return authService.upgradeGuest(principal.user().getId(), request);
    }
}
