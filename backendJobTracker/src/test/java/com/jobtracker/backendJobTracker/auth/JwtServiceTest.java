package com.jobtracker.backendJobTracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jobtracker.backendJobTracker.auth.dto.LoginResponse;
import com.jobtracker.backendJobTracker.config.JobTrackerProperties;
import com.jobtracker.backendJobTracker.exception.UnauthorizedException;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.util.HashUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Unit-тести для {@link JwtService}. RefreshTokenRepository мокається,
 * SecretKey будуємо тим самим способом що SUT, щоб незалежно генерувати/перевіряти токени.
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    // Base64-encoded секрет, що декодується у >= 32 байти (вимога HS256).
    private static final String BASE64_SECRET = Base64.getEncoder()
            .encodeToString("unit-test-secret-key-with-at-least-32-bytes-long!!".getBytes(StandardCharsets.UTF_8));

    private static final Duration ACCESS_TTL = Duration.ofMinutes(5);

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private JwtService jwtService;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        JobTrackerProperties.Auth auth =
                new JobTrackerProperties.Auth(ACCESS_TTL, Duration.ofHours(1), "jobtracker", BASE64_SECRET);
        JobTrackerProperties props = new JobTrackerProperties(
                auth,
                new JobTrackerProperties.RateLimit(100, Duration.ofHours(1), 10, Duration.ofMinutes(15)),
                new JobTrackerProperties.Ai(Duration.ofDays(7), "text-embedding-3-small", 1536),
                null);

        jwtService = new JwtService(props, refreshTokenRepository);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_SECRET));
    }

    private String buildToken(String subject, Instant expiry) {
        return Jwts.builder()
                .subject(subject)
                .issuer("jobtracker")
                .issuedAt(Date.from(Instant.now().minusSeconds(1)))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("Валідний свіжий токен -> true")
        void validToken() {
            String token = buildToken("user@example.com", Instant.now().plusSeconds(300));
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Прострочений токен -> false")
        void expiredToken() {
            String token = buildToken("user@example.com", Instant.now().minusSeconds(60));
            assertThat(jwtService.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("Підроблений підпис (tampered) -> false")
        void tamperedToken() {
            String token = buildToken("user@example.com", Instant.now().plusSeconds(300));
            String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "B" : "A");
            assertThat(jwtService.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("Сміття / null / empty -> false (без винятків)")
        void malformedToken() {
            assertThat(jwtService.validateToken("not.a.jwt")).isFalse();
            assertThat(jwtService.validateToken("")).isFalse();
            assertThat(jwtService.validateToken(null)).isFalse();
        }
    }

    @Test
    @DisplayName("getEmailFromToken повертає subject (email)")
    void getEmailFromToken() {
        String token = buildToken("alice@example.com", Instant.now().plusSeconds(300));
        assertThat(jwtService.getEmailFromToken(token)).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("generateTokens: персистить хеш refresh-токена, повертає raw + Bearer + expiresIn")
    void generateTokens_persistsHashedRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        User user = new User();
        user.setEmail("bob@example.com");

        LoginResponse response = jwtService.generateTokens(user);

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(ACCESS_TTL.toSeconds());
        assertThat(response.getRefreshToken()).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        // У БД лежить ХЕШ, не raw токен — і він збігається з sha256(raw).
        assertThat(saved.getTokenHash()).isEqualTo(HashUtil.sha256(response.getRefreshToken()));
        assertThat(saved.getTokenHash()).isNotEqualTo(response.getRefreshToken());
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        assertThat(saved.getUser()).isSameAs(user);
    }

    @Nested
    @DisplayName("refreshTokens (rotation)")
    class RefreshRotation {

        @Test
        @DisplayName("Активний токен -> revoke старого + видача нової пари")
        void rotatesActiveToken() {
            User user = new User();
            user.setEmail("carol@example.com");
            RefreshToken active = new RefreshToken();
            active.setUser(user);
            active.setRevoked(false);
            active.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));
            active.setTokenHash(HashUtil.sha256("old-raw"));

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(java.util.Optional.of(active));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse response = jwtService.refreshTokens("old-raw");

            assertThat(active.isRevoked()).as("старий токен має бути revoked").isTrue();
            assertThat(response.getRefreshToken()).isNotBlank().isNotEqualTo("old-raw");
            // save двічі: revoke старого + новий токен.
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Невідомий токен -> UnauthorizedException")
        void unknownTokenRejected() {
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> jwtService.refreshTokens("nope"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        @DisplayName("Revoked токен -> UnauthorizedException, нова пара не видається")
        void revokedTokenRejected() {
            RefreshToken revoked = new RefreshToken();
            revoked.setRevoked(true);
            revoked.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(java.util.Optional.of(revoked));

            assertThatThrownBy(() -> jwtService.refreshTokens("used"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expired or revoked");

            verify(refreshTokenRepository, org.mockito.Mockito.never()).save(any(RefreshToken.class));
        }
    }
}
