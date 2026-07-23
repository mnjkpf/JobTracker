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
import com.jobtracker.backendJobTracker.exception.ConflictException;
import com.jobtracker.backendJobTracker.exception.UnauthorizedException;
import com.jobtracker.backendJobTracker.user.Role;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;
import com.jobtracker.backendJobTracker.util.HashUtil;

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

        return jwtService.generateTokens(saved);
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
