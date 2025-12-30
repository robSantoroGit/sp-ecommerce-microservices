package com.ecommerce.userService.controller;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.userService.dto.SecurityContext;
import com.ecommerce.userService.dto.UserRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;
import com.ecommerce.userService.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {
    
    private final UserService userService;
    
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    
    // Constructor injection
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    // RBAC
    /**
     * Helper method to create SecurityContext from headers
     */
//    private SecurityContext createSecurityContext(Long userId, String rolesHeader) {
//        List<String> roles = Arrays.asList(rolesHeader.split(","));
//        String username = "user-" + userId; // Username from header not available, use placeholder
//        return new SecurityContext(userId, username, roles);
//    }
    
    
    // RBAC + ABAC
    private SecurityContext createSecurityContext(Long userId, String scopesHeader) {
        List<String> scopes = Arrays.asList(scopesHeader.split(","));
        String username = "user-" + userId;
        return new SecurityContext(userId, username, scopes);
    }
    
    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user with the provided details - Admins only")
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody UserRequestDTO userRequestDTO,
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestHeader("X-User-Scopes") String scopesHeader) {
        
    	log.info("POST /api/users - Create user attempt");
    	SecurityContext context = createSecurityContext(authenticatedUserId, scopesHeader);
    	UserResponseDTO createdUser = userService.createUser(userRequestDTO, context);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }
    
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves a list of all users - Admins only")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(
    		@RequestHeader("X-User-Id") Long authenticatedUserId,
    		@RequestHeader("X-User-Scopes") String scopesHeader) {
    	log.info("GET /api/users - Get all users");
    	SecurityContext context = createSecurityContext(authenticatedUserId, scopesHeader);
        List<UserResponseDTO> users = userService.getAllUsers(context);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID - Admins or owner only")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id,
    		@RequestHeader("X-User-Id") Long authenticatedUserId,
    		@RequestHeader("X-User-Scopes") String scopesHeader) {
    	log.info("GET /api/users/{} - Get user by ID", id);
    	SecurityContext context = createSecurityContext(authenticatedUserId, scopesHeader);
    	UserResponseDTO user = userService.getUserById(id,context);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Retrieves a user by their username - Admins or owner only")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username,
    		@RequestHeader("X-User-Id") Long authenticatedUserId,
    		@RequestHeader("X-User-Scopes") String scopesHeader) {
        log.info("GET /api/users/username/{} - Get user by username", username);
        SecurityContext context = createSecurityContext(authenticatedUserId, scopesHeader);
    	UserResponseDTO user = userService.getUserByUsername(username,context);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", description = "Retrieves a user by their email - Admins or owner only")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email,
    		@RequestHeader("X-User-Id") Long authenticatedUserId,
    		@RequestHeader("X-User-Scopes") String scopesHeader) {
    	log.info("GET /api/users/email/{} - Get user by email", email);
    	SecurityContext context = createSecurityContext(authenticatedUserId, scopesHeader);
    	UserResponseDTO user = userService.getUserByEmail(email,context);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user's details - Admins or owner only")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO userRequestDTO,
            @RequestHeader("X-User-Id") Long authenticatedUserId,
            @RequestHeader("X-User-Scopes") String scopesHeader) {
        log.info("PUT /api/users/{} - Update user", id);
    	SecurityContext context = createSecurityContext(authenticatedUserId, scopesHeader);
        UserResponseDTO updatedUser = userService.updateUser(id, userRequestDTO, context);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user by their ID - Admins only")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
    		@RequestHeader("X-User-Id") Long authenticatedUserId,
    		@RequestHeader("X-User-Scopes") String scopesHeader) {
    	log.info("DELETE /api/users/{} - Delete user", id);
    	SecurityContext context = createSecurityContext(authenticatedUserId, scopesHeader);
    	userService.deleteUser(id,context);
        return ResponseEntity.noContent().build();
    }
}