package com.solar.management.security;

import com.solar.management.entity.Invoice;
import com.solar.management.entity.Job;
import com.solar.management.entity.LocationTracking;
import com.solar.management.entity.User;
import com.solar.management.entity.WorkLog;
import com.solar.management.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Centralized utility for authentication and authorization operations
 * Provides methods to extract current user from JWT and validate resource access
 */
@Component
@RequiredArgsConstructor
public class AuthenticationHelper {

    private final UserService userService;

    /**
     * Get current authenticated User entity from JWT
     * @return User entity from database
     * @throws AccessDeniedException if user not found
     */
    public User getCurrentUser() {
        Jwt jwt = getCurrentJwt();
        String keycloakId = jwt.getSubject();
        return userService.getUserByKeycloakId(keycloakId)
                .orElseThrow(() -> new AccessDeniedException("User not found in database"));
    }

    /**
     * Get current JWT from SecurityContext
     * @return JWT token
     * @throws AccessDeniedException if no valid authentication found
     */
    public Jwt getCurrentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
            throw new AccessDeniedException("No valid authentication found");
        }
        return (Jwt) auth.getPrincipal();
    }

    /**
     * Check if current user is ADMIN or MANAGER
     * @return true if user has ADMIN or MANAGER role
     */
    public boolean isAdminOrManager() {
        User user = getCurrentUser();
        return user.getRole() == User.UserRole.ADMIN ||
               user.getRole() == User.UserRole.MANAGER;
    }

    /**
     * Check if current user is TECHNICIAN
     * @return true if user has TECHNICIAN role
     */
    public boolean isTechnician() {
        return getCurrentUser().getRole() == User.UserRole.TECHNICIAN;
    }

    /**
     * Validate that userId matches current user (for technicians)
     * Admins/Managers can access any userId
     * @param userId The user ID being accessed
     * @throws AccessDeniedException if technician tries to access another user's data
     */
    public void validateUserAccess(Long userId) {
        User currentUser = getCurrentUser();
        if (isTechnician() && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Technicians can only access their own data");
        }
    }

    /**
     * Validate that the current user has access to a specific job
     * Admins/Managers: full access
     * Technicians: only assigned jobs
     * @param job The job being accessed
     * @throws AccessDeniedException if technician is not assigned to the job
     */
    public void validateJobAccess(Job job) {
        User currentUser = getCurrentUser();
        if (isTechnician() && !job.getAssignedTechnicians().contains(currentUser)) {
            throw new AccessDeniedException("You do not have access to this job");
        }
    }

    /**
     * Validate worklog access
     * Admins/Managers: full access
     * Technicians: only own worklogs
     * @param workLog The work log being accessed
     * @throws AccessDeniedException if technician doesn't own the work log
     */
    public void validateWorkLogAccess(WorkLog workLog) {
        User currentUser = getCurrentUser();
        if (isTechnician() && !workLog.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this work log");
        }
    }

    /**
     * Validate invoice access
     * Admins/Managers: full access
     * Technicians: only own invoices
     * @param invoice The invoice being accessed
     * @throws AccessDeniedException if technician doesn't own the invoice
     */
    public void validateInvoiceAccess(Invoice invoice) {
        User currentUser = getCurrentUser();
        if (isTechnician() && !invoice.getTechnician().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this invoice");
        }
    }

    /**
     * Validate location tracking access
     * Admins/Managers: full access
     * Technicians: only own location data
     * @param location The location tracking record being accessed
     * @throws AccessDeniedException if technician doesn't own the location data
     */
    public void validateLocationTrackingAccess(LocationTracking location) {
        User currentUser = getCurrentUser();
        if (isTechnician() && !location.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this location data");
        }
    }
}
