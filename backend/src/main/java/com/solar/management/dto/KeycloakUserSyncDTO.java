package com.solar.management.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for encapsulating user data extracted from Keycloak JWT token
 * Used for synchronizing users from Keycloak to the application database
 */
@Data
@Builder
public class KeycloakUserSyncDTO {

    /**
     * Keycloak user UUID from JWT 'sub' claim
     * This is the immutable identifier for the user in Keycloak
     */
    private String keycloakId;

    /**
     * Username from JWT 'preferred_username' claim
     */
    private String username;

    /**
     * Email from JWT 'email' claim
     */
    private String email;

    /**
     * First name from JWT 'given_name' claim
     */
    private String firstName;

    /**
     * Last name from JWT 'family_name' claim
     */
    private String lastName;

    /**
     * Roles from JWT 'realm_access.roles' claim
     */
    private List<String> roles;
}
