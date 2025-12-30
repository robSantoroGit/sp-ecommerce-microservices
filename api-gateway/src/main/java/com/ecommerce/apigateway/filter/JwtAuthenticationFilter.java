package com.ecommerce.apigateway.filter;

import com.ecommerce.apigateway.security.Permission;
import com.ecommerce.apigateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health"
    );

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        String method = request.getMethod().name();

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("[{}] Public endpoint, skipping JWT validation: {}", correlationId, path);
            return chain.filter(exchange);
        }

        // Extract token from Authorization header
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("[{}] Missing or invalid Authorization header for: {}", correlationId, path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Validate token
        if (!jwtUtil.validateToken(token)) {
            log.warn("[{}] Invalid or expired JWT token for: {}", correlationId, path);
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        try {
            // Extract claims from token
            Long userId = jwtUtil.extractUserId(token);
            String username = jwtUtil.extractUsername(token);
            
            // RBAC
            //List<String> roles = jwtUtil.extractRoles(token);
            //log.info("[{}] JWT validated successfully - User: {} (ID: {}), Roles: {}", 
            //        correlationId, username, userId, roles);
            
            // RBAC + ABAC
            List<String> scopes = jwtUtil.extractScopes(token);
            log.info("[{}] JWT validated successfully - User: {} (ID: {}), Scopes: {}", 
                    correlationId, username, userId, scopes);
            
            // ==================== RBAC AUTHORIZATION ====================
            
            // Admin-only endpoints
//            if (requiresAdminRole(path, method)) {
//                if (!hasRole(roles, "ADMIN")) {
//                    log.warn("[{}] Access denied - Admin role required: {} {}", correlationId, method, path);
//                    return onError(exchange, "Admin access required", HttpStatus.FORBIDDEN);
//                }
//            }
            
            // ============================================================

            // ==================== RBAC + ABAC AUTHORIZATION ====================
            
            if (requiresPermission(path, method)) {
            	 String requiredPermission = getRequiredPermission(path, method);
                if (!hasPermission(scopes, requiredPermission)) {
                    log.warn("[{}] Access denied - " + requiredPermission + " required: {} {}", correlationId, method, path);
                    return onError(exchange, requiredPermission + " required", HttpStatus.FORBIDDEN);
                }
            }
            
            // ============================================================
            
            
            // Add user info to headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId.toString())
                    .header("X-Username", username)
                    // RBAC .header("X-User-Roles", String.join(",", roles))
                    .header("X-User-Scopes", String.join(",", scopes))
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error("[{}] Error processing JWT token: {}", correlationId, e.getMessage());
            return onError(exchange, "Error processing token", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorResponse = String.format("{\"error\":\"%s\",\"status\":%d}", 
                message, status.value());
        
        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(errorResponse.getBytes())));
    }

    @Override
    public int getOrder() {
        return 0; // Execute after enrichment and logging
    }
    
    // RBAC + ABAC
    /**
     * Check if endpoint requires specific permission
     */
    private boolean requiresPermission(String path, String method) {
        return getRequiredPermission(path, method) != null;
    }

    /**
     * Get required permission for endpoint
     */
    private String getRequiredPermission(String path, String method) {
        // /api/users
        if (path.startsWith("/api/users")) {
            return switch (method) {
                case "POST" -> Permission.USER_WRITE;
                case "DELETE" -> Permission.USER_DELETE;
                default -> null; // GET/PUT → Layer 2 checks
            };
        }
        
        // /api/products and /api/categories
        if (path.startsWith("/api/products") || path.startsWith("/api/categories")) {
            return switch (method) {
                case "POST", "PUT" -> Permission.PRODUCT_WRITE;
                case "DELETE" -> Permission.PRODUCT_DELETE;
                default -> null; // GET → everyone
            };
        }
        
        return null;
    }

    /**
     * Check if user has specific permission
     */
    private boolean hasPermission(List<String> scopes, String requiredPermission) {
        return scopes != null && scopes.contains(requiredPermission);
    }
    
    // RBAC	
//    private boolean requiresAdminRole(String path, String method) {
//        // /api/users - ADMIN operations
//        if (path.startsWith("/api/users")) {
//            // POST (create user) → ADMIN only
//            // DELETE (delete user) → ADMIN only
//            // GET/PUT → pass to Layer 2 (user can access own data, admin can access all)
//            return method.equals("POST") || method.equals("DELETE");
//        }
//        
//        // /api/products and /api/categories - ADMIN only for modifications
//        if (path.startsWith("/api/products") || path.startsWith("/api/categories")) {
//            // GET → everyone (authenticated)
//            // POST/PUT/DELETE → ADMIN only
//            return method.equals("POST") || method.equals("PUT") || method.equals("DELETE");
//        }
//        
//        return false;
//    }
//    
//    /**
//     * Check if user has specific role
//     */
//    private boolean hasRole(List<String> roles, String requiredRole) {
//        return roles != null && roles.contains(requiredRole);
//    }
}