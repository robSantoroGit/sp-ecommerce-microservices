package com.ecommerce.userService.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.userService.dto.SecurityContext;
import com.ecommerce.userService.dto.UserMapper;
import com.ecommerce.userService.dto.UserRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;
import com.ecommerce.userService.exception.DuplicateResourceException;
import com.ecommerce.userService.exception.ForbiddenException;
import com.ecommerce.userService.exception.ResourceNotFoundException;
import com.ecommerce.userService.model.User;
import com.ecommerce.userService.repository.UserRepository;
import com.ecommerce.userService.security.Permission;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    // Constructor injection (no @Autowired needed in modern Spring)
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper,PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    // 0. createUser - Solo Admin può creare utenti
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO, SecurityContext securityContext) {
    	log.info("Creating new user with username: {}", userRequestDTO.getUsername());
        
        // RBAC: Only Admin can create users
//        if (!securityContext.isAdmin()) {
//            log.warn("Access denied: Non-admin user {} tried to create user", securityContext.getUserId());
//            throw new ForbiddenException("Admin access required");
//        }
    	
    	// RBAC + ABAC
    	if (!securityContext.hasPermission(Permission.USER_WRITE)) {
            log.warn("Access denied: user without permission {} tried to create user", securityContext.getUserId());
            throw new ForbiddenException("Admin access required");
        }
    	
    	// Check if username already exists
        if (userRepository.existsByUsername(userRequestDTO.getUsername())) {
            throw new DuplicateResourceException(
                    "Username already exists: " + userRequestDTO.getUsername());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already exists: " + userRequestDTO.getEmail());
        }
        
        // Convert DTO to entity
        User user = userMapper.toEntity(userRequestDTO);
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Save to database
        User savedUser = userRepository.save(user);
        
        log.info("User created successfully: {}", savedUser.getId());
        
        // Convert entity to DTO and return
        return userMapper.toDTO(savedUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    // 1. getUserById - User può vedere solo se stesso, Admin vede tutti
    public UserResponseDTO getUserById(Long id, SecurityContext securityContext) {
        log.info("Fetching user with id: {}", id);
        
        // RBAC: User can only view own profile, Admin can view all
        // if (!securityContext.isAdmin() && !securityContext.isOwner(id)) {
        // RBAC + ABAC
        if (!securityContext.hasPermission(Permission.USER_READ) && !securityContext.isOwner(id)) {
            log.warn("Access denied: User {} tried to access user {}", securityContext.getUserId(), id);
            throw new ForbiddenException("You can only access your own profile");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        return userMapper.toDTO(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    // 2. getAllUsers - Solo Admin vede tutti
    public List<UserResponseDTO> getAllUsers(SecurityContext securityContext) {
    	log.info("Fetching all users");
        
        // RBAC: Only Admin can view all users
        // if (!securityContext.isAdmin()) {
    	// RBAC + ABAC
    	if ( !securityContext.hasPermission(Permission.USER_READ)) {
            log.warn("Access denied: user {} tried to access all users", securityContext.getUserId());
            throw new ForbiddenException("Admin access required");
        }
        
    	List<User> users = userRepository.findAll();
        
        return users.stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    // 3. updateUser - User può modificare solo se stesso, Admin modifica tutti
    public UserResponseDTO updateUser(Long id, UserRequestDTO userRequestDTO, SecurityContext securityContext) {
    	
    	log.info("Updating user with id: {}", id);
        
        // RBAC: User can only update own profile, Admin can update all
        // if (!securityContext.isAdmin() && !securityContext.isOwner(id)) {
    	// RBAC + ABAC
    	if ( !securityContext.hasPermission(Permission.USER_WRITE) && !securityContext.isOwner(id)) {
            log.warn("Access denied: User {} tried to update user {}", securityContext.getUserId(), id);
            throw new ForbiddenException("You can only update your own profile");
        }
    	
    	// Find existing user
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
        
        // Check if new email conflicts with another user
        if (!user.getEmail().equals(userRequestDTO.getEmail()) &&
                userRepository.existsByEmail(userRequestDTO.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already exists: " + userRequestDTO.getEmail());
        }
        
        // Update user fields
        userMapper.updateEntity(user, userRequestDTO);
        
        // Save updated user
        User updatedUser = userRepository.save(user);
        
        log.info("User updated successfully: {}", updatedUser.getId());
        
        return userMapper.toDTO(updatedUser);
    }
    
    @Override
    // 4. deleteUser - Solo Admin (già protetto dal Gateway, ma aggiungiamo comunque)
    public void deleteUser(Long id, SecurityContext securityContext) {
    	log.info("Deleting user with id: {}", id);
        
        // RBAC: Only Admin can delete users
        // if (!securityContext.isAdmin()) {
    	// RBAC + ABAC
    	if ( !securityContext.hasPermission(Permission.USER_DELETE)) {
            log.warn("Access denied: user {} tried to delete user", securityContext.getUserId());
            throw new ForbiddenException("Admin access required");
        }
    	
    	// Check if user exists
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        
        userRepository.deleteById(id);
        log.info("User deleted successfully: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    // 6. getUserByUsername - User può vedere solo se stesso, Admin vede tutti
    public UserResponseDTO getUserByUsername(String username, SecurityContext securityContext) {
    	log.info("Fetching user with username: {}", username);
    	
    	User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username));
        
    	// RBAC: User can only view own profile, Admin can view all
        // if (!securityContext.isAdmin() && !securityContext.isOwner(user.getId())) {
    	// RBAC + ABAC
    	if ( !securityContext.hasPermission(Permission.USER_READ) && !securityContext.isOwner(user.getId())) {
            log.warn("Access denied: User {} tried to access user with username {}", 
                    securityContext.getUserId(), username);
            throw new ForbiddenException("You can only access your own profile");
        }
        
        return userMapper.toDTO(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    // 7. getUserByEmail - User può vedere solo se stesso, Admin vede tutti 
    public UserResponseDTO getUserByEmail(String email, SecurityContext securityContext) {
    	log.info("Fetching user with email: {}", email);
    	User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
    	
    	// RBAC: User can only view own profile, Admin can view all
        //if (!securityContext.isAdmin() && !securityContext.isOwner(user.getId())) {
    	// RBAC + ABAC
    	if ( !securityContext.hasPermission(Permission.USER_READ) && !securityContext.isOwner(user.getId())) {
            log.warn("Access denied: User {} tried to access user with email {}", 
                    securityContext.getUserId(), email);
            throw new ForbiddenException("You can only access your own profile");
        }
    	
        return userMapper.toDTO(user);
    }
}