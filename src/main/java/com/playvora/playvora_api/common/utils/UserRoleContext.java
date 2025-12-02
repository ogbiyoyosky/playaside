package com.playvora.playvora_api.common.utils;

import com.playvora.playvora_api.user.entities.UserRole;

import java.util.UUID;

/**
 * Thread-local storage for the current user's active role in the request context.
 * This allows services to access which role the user is acting under for the current request.
 */
public class UserRoleContext {
    
    private static final ThreadLocal<UserRole> currentUserRole = new ThreadLocal<>();
    
    /**
     * Set the current user role for this request
     */
    public static void setCurrentUserRole(UserRole userRole) {
        currentUserRole.set(userRole);
    }
    
    /**
     * Get the current user role for this request
     * @return The current UserRole or null if not set
     */
    public static UserRole getCurrentUserRole() {
        return currentUserRole.get();
    }
    
    /**
     * Get the current user role ID for this request
     * @return The current UserRole ID or null if not set
     */
    public static UUID getCurrentUserRoleId() {
        UserRole userRole = currentUserRole.get();
        return userRole != null ? userRole.getId() : null;
    }
    
    /**
     * Get the current community ID from the user role
     * @return The community ID or null if role is global or not set
     */
    public static UUID getCurrentCommunityId() {
        UserRole userRole = currentUserRole.get();
        if (userRole != null && userRole.getCommunity() != null) {
            return userRole.getCommunity().getId();
        }
        return null;
    }
    
    /**
     * Check if the current user role is a community-specific role
     * @return true if the role is tied to a community, false otherwise
     */
    public static boolean isCommunityRole() {
        UserRole userRole = currentUserRole.get();
        return userRole != null && userRole.getCommunity() != null;
    }
    
    /**
     * Clear the current user role (should be called at the end of request processing)
     */
    public static void clear() {
        currentUserRole.remove();
    }
    
    /**
     * Check if a user role context is set
     * @return true if a user role is set in the context
     */
    public static boolean hasUserRole() {
        return currentUserRole.get() != null;
    }
}

