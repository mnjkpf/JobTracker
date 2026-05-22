package com.jobtracker.backendJobTracker.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
 
    /** Short-lived JWT for API requests (15 min). Sent as {@code Authorization: Bearer ...}. */
    private String accessToken;
 
    /** Long-lived opaque token (30 days). Sent on {@code POST /auth/refresh} to get a new access token. */
    private String refreshToken;
 
    /** Constant "Bearer" — token type hint per OAuth 2.0 convention. */
    private String tokenType = "Bearer";
 
    /** Access token lifetime in seconds. */
    private long expiresIn;
 
    /** Optional user info — convenience so client doesn't need another round-trip for /me after login. */
    private UserResponse user;
}

