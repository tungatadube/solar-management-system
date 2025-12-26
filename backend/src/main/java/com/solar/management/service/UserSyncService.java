package com.solar.management.service;

import com.solar.management.dto.KeycloakUserSyncDTO;
import com.solar.management.entity.User;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Service for synchronizing users from Keycloak JWT tokens to the application database
 * Ensures that Keycloak is the single source of truth for user data
 */
public interface UserSyncService {

    /**
     * Synchronize user from Keycloak JWT token to database
     * Creates user if doesn't exist, updates if exists
     *
     * @param jwt JWT token from Keycloak containing user claims
     * @return The synchronized User entity
     */
    User syncUserFromJwt(Jwt jwt);

    /**
     * Extract user data from JWT token claims
     *
     * @param jwt JWT token from Keycloak
     * @return KeycloakUserSyncDTO containing extracted user data
     */
    KeycloakUserSyncDTO extractUserFromJwt(Jwt jwt);

    /**
     * Create or update user from sync DTO
     *
     * @param userDTO User data extracted from Keycloak JWT
     * @return The synchronized User entity
     */
    User syncUser(KeycloakUserSyncDTO userDTO);
}
