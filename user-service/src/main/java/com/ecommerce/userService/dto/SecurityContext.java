package com.ecommerce.userService.dto;

import java.util.Arrays;
import java.util.List;

import com.ecommerce.userService.security.Permission;

public class SecurityContext {

    private Long userId;
    private String username;
    // RBAC
    //private List<String> roles;
    // RBAC + ABAC
    private List<String> scopes;
    
    // Constructor
    public SecurityContext(Long userId, String username, List<String> scopes) {
        this.userId = userId;
        this.username = username;
        this.scopes = scopes;
    }

    // RBAC
    // System context for internal operations (admin privileges)
//    public static SecurityContext system() {
//        return new SecurityContext(0L, "SYSTEM", List.of(UserRole.ADMIN.name()));
//    }
    
    // RBAC + ABAC
    public static SecurityContext system() {
        return new SecurityContext(0L, "SYSTEM", List.of(
            Permission.USER_READ, Permission.USER_WRITE, Permission.USER_DELETE
        ));
    }

    // RBAC
    // Helper methods
//    public boolean hasRole(String role) {
//        return roles != null && roles.contains(role);
//    }
//
//    public boolean isAdmin() {
//        return hasRole("ADMIN");
//    }
    
    // RBAC + ABAC
    public boolean hasPermission(String permission) {
        return scopes != null && scopes.contains(permission);
    }

    public boolean hasAnyPermission(String... permissions) {
        return Arrays.stream(permissions)
                .anyMatch(this::hasPermission);
    }

    public boolean isOwner(Long resourceOwnerId) {
        return userId.equals(resourceOwnerId);
    }

    // Getters
    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getScopes() {
        return scopes;
    }
}