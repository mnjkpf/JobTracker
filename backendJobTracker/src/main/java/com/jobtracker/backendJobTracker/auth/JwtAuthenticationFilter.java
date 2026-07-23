package com.jobtracker.backendJobTracker.auth;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractTokenFromRequest(request);

        if(token != null) {
            setAuthentication(token);
        }

        filterChain.doFilter(request, response);
    }

    

    private String extractTokenFromRequest(HttpServletRequest request){
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }

    private void setAuthentication(String token) {
        String email = jwtService.getEmailFromToken(token);
        var userDetails = userDetailsService.loadUserByUsername(email);
        var authToken = new UsernamePasswordAuthenticationToken( userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    

        @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/actuator/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

}
