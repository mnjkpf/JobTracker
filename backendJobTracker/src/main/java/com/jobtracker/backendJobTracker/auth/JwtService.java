package com.jobtracker.backendJobTracker.auth;

// ─── SecureRandom ────────────────────────────────────────────────────────────
// SecureRandom — cryptographically strong PRNG. Не плутати з java.util.Random,
// який детермінований при відомому seed (можна "вгадати" наступні значення).
// SecureRandom використовує OS entropy source (/dev/urandom на Linux, CryptGenRandom на Windows)
// і призначений саме для security-критичних випадків: nonces, tokens, salt, IVs.
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.auth.dto.LoginResponse;
import com.jobtracker.backendJobTracker.config.JobTrackerProperties;
import com.jobtracker.backendJobTracker.exception.UnauthorizedException;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.util.HashUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);

    // SecureRandom потокобезпечний (за документацією), тому одну instance на клас вистачає.
    // Створення SecureRandom має деякі startup costs (seed від OS), тому переюзаємо.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    
    private final JobTrackerProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResponse generateTokens(User user) {
        // 1. Access token — підписаний JWT з email у claims (subject).
        String accessToken = createAccessToken(user.getEmail());

        // 2. Refresh token — opaque random 256-bit string.
        //    Це НЕ JWT. Просто URL-safe Base64 від 32 random bytes.
        //    "Opaque" означає: токен сам по собі не містить даних, він лише ключ
        //    до запису в БД (як session ID, по суті).
        String rawRefreshToken = generateOpaqueToken();

        // 3. У БД зберігаємо SHA-256 ХЕШ raw токену.
        //    Якщо хтось зчитає БД — він матиме хеши, з яких не можна відтворити raw token.
        //    При наступному refresh клієнт надішле raw, ми зхешуємо і порівняємо.
        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(HashUtil.sha256(rawRefreshToken));
        entity.setUser(user);
        // Instant.plus(Duration) — додавання тривалості до моменту часу.
        // properties.auth().refreshTokenTtl() = Duration з P30D (30 днів).
        entity.setExpiresAt(Instant.now().plus(properties.auth().refreshTokenTtl()));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);

        // 4. Формуємо відповідь.
        //    expiresIn в секундах — стандарт OAuth 2.0 для клієнтів.
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(rawRefreshToken);  // RAW токен — тільки клієнту, не в БД
        response.setTokenType("Bearer");
        response.setExpiresIn(properties.auth().accessTokenTtl().toSeconds());
        return response;
    }

    /**
     * Refresh flow з ротацією: видаємо нову пару (access + refresh), старий refresh revoke'аний.
     * <p>
     * <b>Чому rotation</b> (а не "просто новий access на старому refresh"): якщо вкрали
     * refresh token, атакуючий може ним вічно генерувати access tokens. З rotation
     * перший хто скористається — інвалідує токен для другого. Це не повний захист, але
     * сильно піднімає bar для атак.
     * <p>
     * <b>Чому setRevoked(true) а не delete()</b>: audit trail. У БД залишається запис,
     * коли був виданий, коли використаний, коли revoke'аний. Корисно для security
     * incident investigation і для метрик.
     */
    @Transactional
    public LoginResponse refreshTokens(String rawRefreshToken) {
        // 1. Хешуємо incoming token і шукаємо в БД.
        //    Pattern: client sends raw, we hash incoming, compare to stored hash.
        String incomingHash = HashUtil.sha256(rawRefreshToken);

        // .orElseThrow — якщо Optional порожній, кидаємо UnauthorizedException.
        // GlobalExceptionHandler переведе це у 401 з ProblemDetail body.
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // 2. Перевірка статусу: чи не revoked, чи не expired.
        //    isActive() = !revoked && Instant.now().isBefore(expiresAt) (метод у RefreshToken entity).
        if (!oldToken.isActive()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        // 3. Revoke старого токена (НЕ delete — зберігаємо аудит).
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // 4. Видаємо нову пару (access + refresh).
        //    Рекурсивний виклик generateTokens створить новий refresh у БД та поверне його клієнту.
        //    Користувач отримує абсолютно нову пару — старий refresh більше не діє.
        return generateTokens(oldToken.getUser());
    }

    /**
     * Валідація access token: перевіряє signature, expiration, format.
     * <p>
     * Повертає {@code boolean} (не throws) — викликається з {@link JwtAuthenticationFilter},
     * де exceptions не хочуть зупиняти filter chain. Якщо токен невалідний — false,
     * filter залишає SecurityContext порожнім, далі SecurityConfig вирішує
     * "401" чи "permit" відповідно до правил.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException — все що JJWT може кинути: expired, malformed, bad signature, etc.
            // IllegalArgumentException — null/empty input.
            // На рівні warn (не error), бо невалідний токен у запиті — це не баг сервера,
            // а очікувана ситуація (просрочений токен, атака, помилка клієнта).
            LOGGER.warn("Token validation failed ({}): {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * Витягає email (з {@code subject} claim) з валідованого access token.
     * <p>
     * <b>Передумова</b>: токен має бути попередньо валідований через {@link #validateToken}.
     * Цей метод може кинути JwtException якщо викликати з невалідним токеном.
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Створює signed JWT access token.
     * <p>
     * Claims:
     * <ul>
     *   <li>{@code sub} (subject) — email юзера</li>
     *   <li>{@code iss} (issuer) — наш application (з properties)</li>
     *   <li>{@code iat} (issued at) — момент створення</li>
     *   <li>{@code exp} (expiration) — iat + access TTL</li>
     * </ul>
     * <p>
     * Sign алгоритм визначається довжиною ключа: 256 bits → HS256, 384 → HS384, 512 → HS512.
     * Key.hmacShaKeyFor() автоматично обирає правильний.
     */
    private String createAccessToken(String email) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.auth().accessTokenTtl());

        // JJWT 0.12+ API: методи без префіксу set... (deprecated setSubject/setIssuer/...)
        return Jwts.builder()
                .subject(email)
                .issuer(properties.auth().issuer())
                .issuedAt(Date.from(now))        // JJWT API приймає java.util.Date, конвертуємо з Instant
                .expiration(Date.from(expiry))
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * Генерує opaque (непрозорий) refresh token: 256 bits cryptographically random,
     * URL-safe Base64 encoded, без padding.
     * <p>
     * <b>Чому 256 bits</b> — баланс довжини і entropy. 256 bits = 2^256 можливих токенів,
     * це 1.16 × 10^77 — більше ніж атомів у спостережуваному Всесвіті. Брутфорс неможливий.
     * <p>
     * <b>Чому URL-safe Base64 без padding</b> — токен буде у URL/header/JSON.
     * Стандартний Base64 містить {@code +} і {@code /}, які треба URL-encoding.
     * URL-safe Base64 використовує {@code -} і {@code _} замість них. Без padding ({@code =})
     * — коротше і красивіше.
     * <p>
     * Довжина результату ~43 символи: ceil(32 bytes × 4/3) - padding.
     */
    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];          // 32 bytes = 256 bits
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    /**
     * Розшифровує Base64-encoded JWT secret з properties у HMAC SecretKey.
     * <p>
     * Secret має бути мінімум 256 бітів (32 байти) для HS256. Якщо менше — JJWT кине
     * виняток при першому використанні. У {@link JobTrackerProperties} він валідується
     * через {@code @NotBlank}, але довжину варто перевіряти окремо у CI/CD або тестах.
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(properties.auth().jwtSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}