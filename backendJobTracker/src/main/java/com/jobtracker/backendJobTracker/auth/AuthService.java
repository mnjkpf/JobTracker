package com.jobtracker.backendJobTracker.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.auth.dto.LoginRequest;
import com.jobtracker.backendJobTracker.auth.dto.LoginResponse;
import com.jobtracker.backendJobTracker.auth.dto.RefreshTokenRequest;
import com.jobtracker.backendJobTracker.auth.dto.RegisterRequest;
import com.jobtracker.backendJobTracker.config.JobTrackerProperties;
import com.jobtracker.backendJobTracker.exception.ConflictException;
import com.jobtracker.backendJobTracker.exception.UnauthorizedException;
import com.jobtracker.backendJobTracker.user.Role;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;
import com.jobtracker.backendJobTracker.status.StatusService;
import com.jobtracker.backendJobTracker.util.HashUtil;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final StatusService statusService;
    private final JobTrackerProperties properties;

    @Transactional
    public LoginResponse register( RegisterRequest request) {

        
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("User with email " + email + " already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setRole(Role.USER);  // нові юзери завжди USER; адміни ставляться вручну в БД
        user.setActive(true);
        user.setEmailVerified(false);  // підтвердиться через email verification flow (W2-B)
 
        User saved = userRepository.save(user);

        statusService.seedDefaults(saved);

        return jwtService.generateTokens(saved);
    }

    /**
     * Створює анонімний (гостьовий) акаунт і одразу видає токени — юзер може
     * користуватись трекером, не реєструючись.
     * <p>
     * Акаунт отримує повний набір дефолтних статусів, як звичайний, і згорає
     * через {@code jobtracker.guest.ttl}, якщо не буде апгрейднутий реєстрацією.
     */
    @Transactional
    public LoginResponse createGuestSession() {
        User guest = new User();
        // email і passwordHash лишаються null — CHECK-constraint у БД це допускає
        // саме для гостя (див. V22__add_guest_users.sql).
        guest.setGuest(true);
        guest.setGuestExpiresAt(Instant.now().plus(properties.guest().ttl()));
        guest.setRole(Role.USER);
        guest.setActive(true);
        guest.setEmailVerified(false);

        User saved = userRepository.save(guest);
        statusService.seedDefaults(saved);

        return jwtService.generateTokens(saved);
    }

    /**
     * Перетворює гостьовий акаунт у повноцінний: той самий рядок {@code users}
     * отримує email і пароль, тому всі заявки, CV та згенерований контент
     * залишаються на місці — ніякої міграції даних не відбувається.
     */
    @Transactional
    public LoginResponse upgradeGuest(UUID userId, RegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Account not found"));

        if (!user.isGuest()) {
            throw new ConflictException("This account is already registered");
        }

        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("User with email " + email + " already exists");
        }

        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setGuest(false);
        user.setGuestExpiresAt(null);
        userRepository.save(user);

        // Нова пара токенів: у subject тепер email замість "guest:<uuid>".
        return jwtService.generateTokens(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        
        Authentication  auth;
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is disabled");
        }
        // Пароль зберігається через passwordEncoder (BCrypt) у register(), тому й
        // перевіряти треба тим самим encoder'ом. HashUtil (SHA-256) тут НЕ підходить —
        // він для refresh-токенів; BCrypt-хеш ($2a$...) ніколи не збігся б із SHA-256.
        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()) ) {
            throw new UnauthorizedException("Invalid email or password");
        }
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Generic повідомлення — не зливаємо чи email існує, чи пароль не той.
            // Це best practice для login forms (захист від enumeration attacks).
            throw new UnauthorizedException("Invalid email or password");
        } catch (DisabledException e) {
            throw new UnauthorizedException("Account is disabled");
        }

        // principal = CustomUserDetails (наша імплементація UserDetails).
        // Через record accessor .user() дістаємо реального User entity.
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        User userAuth = principal.user();

        return jwtService.generateTokens(userAuth);

    }

    /**
     * Logout одного device — revoke refresh token який клієнт надсилає.
     * <p>
     * Access token при цьому НЕ revoke'ається (він stateless, помре через 15 хв
     * сам). Якщо потрібно instant invalidation access tokens — це окрема справа
     * (token blacklist у Redis), не для MVP.
     */

    @Transactional
    public void logout(RefreshTokenRequest refreshTrawRefreshTokenoken) {
        String hash = HashUtil.sha256(refreshTrawRefreshTokenoken.getRefreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });

        // Якщо token не знайдено — мовчки повертаємо. Не блокуємо logout
        // лише через те, що клієнт надіслав уже невідомий токен.

    }



    
}
