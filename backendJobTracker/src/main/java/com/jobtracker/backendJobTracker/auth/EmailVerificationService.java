package com.jobtracker.backendJobTracker.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.email.EmailService;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;
import com.jobtracker.backendJobTracker.util.HashUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {
 
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
 
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
 
    /**
     * Юзер запитує verification email. Якщо юзер не існує — повертаємо тихо
     * без помилки (захист від enumeration attacks: атакуючий не дізнається
     * які emails зареєстровані за відповіддю API).
     */
    @Transactional
    public void requestVerification(String email) {
        String normalizedEmail = email.toLowerCase().trim();
 
        // Тихо повертаємось якщо юзера немає — щоб не leak'ати які emails зареєстровані
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return;
        }
 
        if (user.isEmailVerified()) {
            throw new BusinessRuleException("Email is already verified");
        }
 
        // Інвалідуємо всі попередні active verification токени цього юзера.
        // Якщо запитав двічі — діє лише останній.
        tokenRepository.invalidateAllForUser(
                user.getId(),
                VerificationTokenType.EMAIL_VERIFICATION,
                Instant.now()
        );
 
        // Створюємо новий opaque token
        String rawToken = generateOpaqueToken();
 
        VerificationToken vt = new VerificationToken();
        vt.setTokenHash(HashUtil.sha256(rawToken));
        vt.setTokenType(VerificationTokenType.EMAIL_VERIFICATION);
        vt.setExpiresAt(Instant.now().plus(VERIFICATION_TTL));
        vt.setUser(user);
        tokenRepository.save(vt);
 
        // Надсилаємо RAW token у email — клієнт повертатиметься з ним до /verify
        sendVerificationEmail(normalizedEmail, rawToken);
    }
 
    /**
     * Юзер кликнув link у email і повернувся з raw token.
     * Перевіряємо, маркуємо used, активуємо emailVerified.
     */
    @Transactional
    public void verify(String rawToken) {
        String hash = HashUtil.sha256(rawToken);
 
        VerificationToken token = tokenRepository
                .findByTokenHashAndTokenType(hash, VerificationTokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new ResourceNotFoundException("Verification token not found"));
 
        if (!token.isUsable()) {
            throw new BusinessRuleException("Verification token expired or already used");
        }
 
        // Single-use: маркуємо як використаний
        token.markUsed();
        tokenRepository.save(token);
 
        // Активуємо emailVerified
        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
    }
 
    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
 
    private void sendVerificationEmail(String to, String rawToken) {
        // У реальному UI tu було б URL з token як query параметром:
        // https://jobtracker.local/verify-email?token=abc123
        // Frontend парсить token з URL і викликає POST /api/v1/auth/verify-email
        String body = """
                Hi,
 
                Please verify your email by using this token:
 
                %s
 
                This token is valid for 24 hours.
 
                If you didn't register, ignore this email.
                """.formatted(rawToken);
 
        emailService.sendEmail(to, "Verify your email — Job Tracker", body);
    }
}

