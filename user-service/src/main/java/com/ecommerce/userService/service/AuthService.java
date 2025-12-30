package com.ecommerce.userService.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.userService.dto.LoginRequestDTO;
import com.ecommerce.userService.dto.LoginResponseDTO;
import com.ecommerce.userService.dto.RegisterRequestDTO;
import com.ecommerce.userService.dto.UserMapper;
import com.ecommerce.userService.dto.UserResponseDTO;
import com.ecommerce.userService.exception.DuplicateResourceException;
import com.ecommerce.userService.exception.InvalidCredentialsException;
import com.ecommerce.userService.model.User;
import com.ecommerce.userService.model.UserRole;
import com.ecommerce.userService.repository.UserRepository;
import com.ecommerce.userService.util.JwtUtil;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    /**
     * Authenticate user and generate JWT token
     */
    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        // Find user by email or username
        Optional<User> userOptional = userRepository.findByEmail(request.getUsernameOrEmail());
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByUsername(request.getUsernameOrEmail());
        }

        User user = userOptional.orElseThrow(() -> {
            log.warn("Login failed: User not found: {}", request.getUsernameOrEmail());
            return new InvalidCredentialsException("Invalid username/email or password");
        });

        // Check if user is active
        if (!user.isActive()) {
            log.warn("Login failed: User account is inactive: {}", request.getUsernameOrEmail());
            throw new InvalidCredentialsException("User account is inactive");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Invalid password for: {}", request.getUsernameOrEmail());
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user);
        Long expiresIn = jwtUtil.getExpirationTime();

        // Map user to DTO
        UserResponseDTO userResponse = userMapper.toDTO(user);

        log.info("Login successful for user: {} (ID: {})", user.getUsername(), user.getId());

        return new LoginResponseDTO(token, expiresIn, userResponse);
    }
    
    /**
     * Register new user with hashed password
     */
    public UserResponseDTO register(RegisterRequestDTO request) {
        log.info("Registration attempt for username: {}, email: {}", 
                request.getUsername(), request.getEmail());

        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed: Username already exists: {}", request.getUsername());
            throw new DuplicateResourceException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed: Email already exists: {}", request.getEmail());
            throw new DuplicateResourceException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash password
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.CUSTOMER);
        
        // Save user
        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());

        return userMapper.toDTO(savedUser);
    }
}