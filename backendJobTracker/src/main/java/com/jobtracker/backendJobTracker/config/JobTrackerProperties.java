package com.jobtracker.backendJobTracker.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Strongly-typed bindings for the {@code jobtracker.*} namespace in
 * application.properties. Bound and validated at startup — invalid or
 * missing required values cause fail-fast startup with a clear error.
 * <p>
 * This is intentional: missing {@code JWT_SECRET} in production should
 * never silently fall through to a weak default.
 */
@Validated
@ConfigurationProperties(prefix = "jobtracker")
public record JobTrackerProperties(

        @Valid @NotNull Auth auth,
        @Valid @NotNull RateLimit rateLimit,
        @Valid @NotNull Ai ai,
        @Valid Cors cors
) {

    public record Auth(
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl,
            @NotBlank String issuer,
            @NotBlank String jwtSecret
    ) {}

    public record RateLimit(
            @Positive int defaultCapacity,
            @NotNull Duration defaultRefillPeriod,
            @Positive int authEndpointsCapacity,
            @NotNull Duration authEndpointsRefillPeriod
    ) {}

    public record Ai(
            @NotNull Duration cacheTtl,
            @NotBlank String embeddingModel,
            @Positive int embeddingDimension
    ) {}

    public record Cors(
            List<String> allowedOrigins
    ) {}
}