package com.jobtracker.backendJobTracker.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jobtracker.backendJobTracker.email.EmailService;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;
import com.jobtracker.backendJobTracker.util.HashUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
 
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration RESET_TTL = Duration.ofHours(1);
 
    private final VerificationTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
 
    /**
     * Юзер запросив reset. Відповідаємо однаково незалежно від того, є юзер чи ні
     * (захист від email enumeration: атакуючий не дізнається які emails зареєстровані).
     */
    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
 
        if (user == null) {
            // Тихо повертаємось — клієнту виглядає так само як успіх
            return;
        }
 
        // Інвалідуємо попередні active reset токени
        tokenRepository.invalidateAllForUser(
                user.getId(),
                VerificationTokenType.PASSWORD_RESET,
                Instant.now()
        );
 
        String rawToken = generateOpaqueToken();
 
        VerificationToken vt = new VerificationToken();
        vt.setTokenHash(HashUtil.sha256(rawToken));
        vt.setTokenType(VerificationTokenType.PASSWORD_RESET);
        vt.setExpiresAt(Instant.now().plus(RESET_TTL));
        vt.setUser(user);
        tokenRepository.save(vt);
 
        sendResetEmail(normalizedEmail, rawToken);
    }
 
    /**
     * Юзер прислав raw token + новий пароль. Перевіряємо, оновлюємо.
     * Critical: після успішного reset revoke'аємо ВСІ refresh tokens юзера,
     * щоб старі сесії стали невалідні (атакуючий міг мати active session).
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = HashUtil.sha256(rawToken);
 
        VerificationToken token = tokenRepository
                .findByTokenHashAndTokenType(hash, VerificationTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new ResourceNotFoundException("Reset token not found"));
 
        if (!token.isUsable()) {
            throw new BusinessRuleException("Reset token expired or already used");
        }
 
        // Маркуємо token як використаний
        token.markUsed();
        tokenRepository.save(token);
 
        // Оновлюємо пароль
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
 
        // SECURITY: revoke всі refresh tokens — змушуємо relogin усюди.
        // Якщо хтось ще мав активну сесію — після цього вона безкорисна.
        refreshTokenRepository.revokeAllForUser(user.getId());
    }
 
    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
 
    private void sendResetEmail(String to, String rawToken) {
        String body = """
                Hi,
 
                A password reset was requested for your account.
 
                Use this token to set a new password:
 
                %s
 
                This token is valid for 1 hour. If you didn't request a reset,
                ignore this email — your password remains unchanged.
                """.formatted(rawToken);
 
        emailService.sendEmail(to, "Password reset — Job Tracker", body);
    }
}
