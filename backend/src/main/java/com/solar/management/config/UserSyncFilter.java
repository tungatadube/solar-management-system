package com.solar.management.config;

import com.solar.management.service.UserSyncService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Servlet filter that automatically syncs users from Keycloak JWT to database
 * Runs after JWT authentication, on every authenticated request
 * Ensures database user record is in sync with Keycloak
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserSyncFilter extends OncePerRequestFilter {

    private final UserSyncService userSyncService;

    // Public endpoints that don't require user sync
    private static final List<String> SKIP_SYNC_PATHS = Arrays.asList(
            "/actuator",
            "/health",
            "/api/public",
            "/error",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip sync for public endpoints
        if (shouldSkipSync(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Get authentication from SecurityContext (set by Spring Security)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // If authenticated with JWT, sync user from token
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Jwt) {

                Jwt jwt = (Jwt) authentication.getPrincipal();

                // Best-effort sync: don't block request if sync fails
                try {
                    userSyncService.syncUserFromJwt(jwt);
                } catch (Exception e) {
                    // Log error but don't block the request
                    log.error("Failed to sync user from JWT: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in UserSyncFilter: {}", e.getMessage(), e);
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request path should skip user sync
     */
    private boolean shouldSkipSync(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_SYNC_PATHS.stream().anyMatch(path::startsWith);
    }
}
