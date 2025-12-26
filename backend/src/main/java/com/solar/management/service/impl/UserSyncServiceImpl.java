package com.solar.management.service.impl;

import com.solar.management.dto.KeycloakUserSyncDTO;
import com.solar.management.entity.User;
import com.solar.management.repository.UserRepository;
import com.solar.management.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of UserSyncService
 * Handles automatic synchronization of users from Keycloak JWT tokens to the database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncServiceImpl implements UserSyncService {

    private final UserRepository userRepository;

    // Only sync if last sync was more than 5 minutes ago (throttling)
    private static final long SYNC_THROTTLE_MINUTES = 5;

    @Override
    @Transactional
    public User syncUserFromJwt(Jwt jwt) {
        KeycloakUserSyncDTO userDTO = extractUserFromJwt(jwt);
        return syncUser(userDTO);
    }

    @Override
    public KeycloakUserSyncDTO extractUserFromJwt(Jwt jwt) {
        // Extract keycloak ID from 'sub' claim (user UUID in Keycloak)
        String keycloakId = jwt.getSubject();

        // Extract username from 'preferred_username' claim
        String username = jwt.getClaimAsString("preferred_username");

        // Extract email from 'email' claim
        String email = jwt.getClaimAsString("email");

        // Extract first name from 'given_name' claim
        String firstName = jwt.getClaimAsString("given_name");
        if (firstName == null || firstName.isBlank()) {
            // Fallback: derive from username
            firstName = username != null && username.contains(".")
                ? username.split("\\.")[0]
                : username;
            if (firstName != null) {
                firstName = capitalize(firstName);
            }
        }

        // Extract last name from 'family_name' claim
        String lastName = jwt.getClaimAsString("family_name");
        if (lastName == null || lastName.isBlank()) {
            // Fallback: derive from username or use default
            lastName = username != null && username.contains(".") && username.split("\\.").length > 1
                ? capitalize(username.split("\\.")[1])
                : "User";
        }

        // Extract roles from 'realm_access.roles' claim
        List<String> roles = extractRoles(jwt);

        return KeycloakUserSyncDTO.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public User syncUser(KeycloakUserSyncDTO userDTO) {
        Optional<User> existingUser = userRepository.findByKeycloakId(userDTO.getKeycloakId());

        if (existingUser.isPresent()) {
            return updateExistingUser(existingUser.get(), userDTO);
        } else {
            return createNewUser(userDTO);
        }
    }

    /**
     * Create new user from Keycloak data
     */
    private User createNewUser(KeycloakUserSyncDTO userDTO) {
        User user = User.builder()
                .keycloakId(userDTO.getKeycloakId())
                .username(userDTO.getUsername())
                .email(userDTO.getEmail())
                .password(UUID.randomUUID().toString())  // Random, unused password
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .role(determineHighestRole(userDTO.getRoles()))
                .active(true)
                .syncSource(User.SyncSource.KEYCLOAK_AUTO)
                .lastKeycloakSync(LocalDateTime.now())
                .build();

        try {
            User savedUser = userRepository.save(user);
            log.info("Created new user from Keycloak: username={}, keycloakId={}, role={}",
                    userDTO.getUsername(), userDTO.getKeycloakId(), user.getRole());
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            // Handle duplicate username/email conflict
            if (e.getMessage().contains("username")) {
                log.warn("Username conflict for {}, appending keycloak ID suffix", userDTO.getUsername());
                user.setUsername(userDTO.getUsername() + "_" + userDTO.getKeycloakId().substring(0, 8));
                return userRepository.save(user);
            }
            throw e;
        }
    }

    /**
     * Update existing user from Keycloak data
     */
    private User updateExistingUser(User user, KeycloakUserSyncDTO userDTO) {
        LocalDateTime lastSync = user.getLastKeycloakSync();

        // Throttling: only sync if > 5 minutes since last sync
        if (lastSync != null &&
            lastSync.isAfter(LocalDateTime.now().minusMinutes(SYNC_THROTTLE_MINUTES))) {
            log.debug("Skipping sync - recently synced: username={}", user.getUsername());
            return user;
        }

        // Update fields from Keycloak (always take Keycloak as source of truth)
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setRole(determineHighestRole(userDTO.getRoles()));
        user.setUsername(userDTO.getUsername());
        user.setLastKeycloakSync(LocalDateTime.now());

        // Preserve database-managed fields: phoneNumber, abn, address, account, jobs, etc.

        User savedUser = userRepository.save(user);
        log.debug("Updated user from Keycloak: username={}, keycloakId={}, role={}",
                userDTO.getUsername(), userDTO.getKeycloakId(), user.getRole());
        return savedUser;
    }

    /**
     * Determine highest privilege role from Keycloak roles
     * Priority: ADMIN > MANAGER > TECHNICIAN > ASSISTANT
     */
    private User.UserRole determineHighestRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return User.UserRole.TECHNICIAN; // Default role
        }

        if (roles.contains("ADMIN")) {
            return User.UserRole.ADMIN;
        }
        if (roles.contains("MANAGER")) {
            return User.UserRole.MANAGER;
        }
        if (roles.contains("TECHNICIAN")) {
            return User.UserRole.TECHNICIAN;
        }
        if (roles.contains("ASSISTANT")) {
            return User.UserRole.ASSISTANT;
        }

        return User.UserRole.TECHNICIAN; // Default if no recognized role
    }

    /**
     * Extract roles from JWT realm_access.roles claim
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return (List<String>) realmAccess.get("roles");
        }
        return List.of();
    }

    /**
     * Capitalize first letter of a string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
