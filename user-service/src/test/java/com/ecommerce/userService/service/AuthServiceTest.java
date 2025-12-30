package com.ecommerce.userService.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User user;
    private UserResponseDTO userResponseDTO;
    private LoginRequestDTO loginRequest;
    private RegisterRequestDTO registerRequest;

    @BeforeEach
    void setUp() {
        // Setup user
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Setup DTO
        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(1L);
        userResponseDTO.setUsername("testuser");
        userResponseDTO.setEmail("test@example.com");
        userResponseDTO.setRole(UserRole.CUSTOMER);

        // Setup requests
        loginRequest = new LoginRequestDTO();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void login_WithUsername_Success() {
        // Arrange
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");
        when(jwtUtil.getExpirationTime()).thenReturn(86400000L);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        // Act
        LoginResponseDTO response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
        assertThat(response.getUser()).isNotNull();

        verify(userRepository).findByEmail("testuser");
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(jwtUtil).generateToken(user);
    }

    @Test
    void login_WithEmail_Success() {
        // Arrange
        loginRequest.setUsernameOrEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");
        when(jwtUtil.getExpirationTime()).thenReturn(86400000L);
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        // Act
        LoginResponseDTO response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");

        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username/email or password");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_UserInactive_ThrowsException() {
        // Arrange
        user.setActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("User account is inactive");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username/email or password");

        verify(jwtUtil, never()).generateToken(any());
    }

    // ==================== REGISTER TESTS ====================

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(any(User.class))).thenReturn(userResponseDTO);

        // Act
        UserResponseDTO response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");

        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}