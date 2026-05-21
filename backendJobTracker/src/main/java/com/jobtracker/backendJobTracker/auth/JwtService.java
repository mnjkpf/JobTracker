package com.jobtracker.backendJobTracker.auth;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jobtracker.backendJobTracker.auth.dto.LoginResponse;

import com.jobtracker.backendJobTracker.user.User;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secretKey;
    private final RefreshTokenRepository refreshTokenRepository;
    

    public LoginResponse generateToken(User user){
        String token = createToken(user.getEmail());
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);

        String refreshToken = createRefreshToken(user.getEmail());

        RefreshToken refreshTokenEntity = new RefreshToken();

        refreshTokenEntity.setTokenHash(refreshToken);
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant());
        refreshTokenEntity.setRevoked(false);
        refreshTokenRepository.save(refreshTokenEntity);
        loginResponse.setRefreshToken(refreshToken);
        return loginResponse;
    }

    public LoginResponse refreshBaseToken(String refreshToken) {
        RefreshToken tokenEntity = refreshTokenRepository.findByTokenHash(refreshToken);
        if (tokenEntity == null || !tokenEntity.isActive()) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        User user = tokenEntity.getUser();
        String newToken = createToken(user.getEmail());

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(newToken);
        loginResponse.setRefreshToken(refreshToken);
        loginResponse.setDisplayName(user.getDisplayName());
        refreshTokenRepository.delete(tokenEntity);
        tokenEntity = new RefreshToken();
        tokenEntity.setTokenHash(refreshToken);
        tokenEntity.setUser(user);
        tokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant());
        tokenEntity.setRevoked(false);
        refreshTokenRepository.save(tokenEntity);
        return loginResponse;
    }

    private String createRefreshToken(String email) {
        Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24); // 24 hours
        return Jwts.builder()
                .setSubject(email)
                .issuedAt(new Date())
                .setExpiration(expiration)
                .signWith(getSecretKey())
                .compact();
    }

    private String createToken(String email){
        Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24); // 24 hours
        return Jwts.builder()
                .setSubject(email)
                .issuedAt(new Date())
                .setExpiration(expiration)
                .signWith(getSecretKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            LOGGER.error("Token expired", e);
        } catch (UnsupportedJwtException e) {
            LOGGER.error("Unsupported token", e);
        } catch (MalformedJwtException e) {
            LOGGER.error("Malformed token", e);
        } catch (SignatureException e) {
            LOGGER.error("Signature validation failed", e);
        } catch (Exception e) {
            LOGGER.error("Invalid token", e);
        }
        return false;
    }


    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            LOGGER.error("Refresh token expired", e);
        } catch (UnsupportedJwtException e) {
            LOGGER.error("Unsupported refresh token", e);
        } catch (MalformedJwtException e) {
            LOGGER.error("Malformed refresh token", e);
        } catch (SignatureException e) {
            LOGGER.error("Refresh token signature validation failed", e);
        } catch (Exception e) {
            LOGGER.error("Invalid refresh token", e);
        }
        return false;
    }
    

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getPayload();

        return claims.getSubject();
    }


    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
