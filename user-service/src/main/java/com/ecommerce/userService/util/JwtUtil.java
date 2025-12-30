package com.ecommerce.userService.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ecommerce.userService.model.User;
import com.ecommerce.userService.model.UserRole;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * Generate JWT token for user
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        // RBAC -> claims.put("roles", Arrays.asList(user.getRole().name()));
        // RBAC + ABAC
        List<String> permissions = mapRoleToPermissions(user.getRole());
        claims.put("scope", String.join(" ", permissions));  // OAuth2 standard

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    // nella realtà bisognere avere le 5 tabelle: user-role-permisson e le 2 associative
    private List<String> mapRoleToPermissions(UserRole role) {
        return switch (role) {
            case ADMIN -> List.of(
                "user.read", "user.write", "user.delete",
                "product.read", "product.write", "product.delete",
                "order.read", "order.write", "order.delete"
            );
            case SELLER -> List.of(
                "product.read", "product.write",
                "order.read"
            );
            case CUSTOMER -> List.of();
        };
    }
    
    /**
     * Get expiration time in milliseconds
     */
    public Long getExpirationTime() {
        return expiration;
    }
}