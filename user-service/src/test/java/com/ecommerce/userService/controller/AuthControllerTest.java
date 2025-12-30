package com.ecommerce.userService.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.userService.dto.LoginRequestDTO;
import com.ecommerce.userService.dto.LoginResponseDTO;
import com.ecommerce.userService.dto.RegisterRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;
import com.ecommerce.userService.exception.DuplicateResourceException;
import com.ecommerce.userService.exception.GlobalExceptionHandler;
import com.ecommerce.userService.exception.InvalidCredentialsException;
import com.ecommerce.userService.model.UserRole;
import com.ecommerce.userService.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@Import({AuthController.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    private LoginRequestDTO loginRequest;
    private LoginResponseDTO loginResponse;
    private RegisterRequestDTO registerRequest;
    private UserResponseDTO userResponse;

    @BeforeEach
    void setUp() {
        // Setup login request
        loginRequest = new LoginRequestDTO();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        // Setup user response
        userResponse = new UserResponseDTO();
        userResponse.setId(1L);
        userResponse.setUsername("testuser");
        userResponse.setEmail("test@example.com");
        userResponse.setRole(UserRole.CUSTOMER);
        userResponse.setActive(true);

        // Setup login response
        loginResponse = new LoginResponseDTO();
        loginResponse.setToken("jwt-token-example");
        loginResponse.setType("Bearer");
        loginResponse.setExpiresIn(86400000L);
        loginResponse.setUser(userResponse);

        // Setup register request
        registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void login_Success() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-example"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400000))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));

        verify(authService, times(1)).login(any(LoginRequestDTO.class));
    }

    @Test
    void login_InvalidCredentials_Returns401() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new InvalidCredentialsException("Invalid username/email or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(authService, times(1)).login(any(LoginRequestDTO.class));
    }

    @Test
    void login_MissingUsernameOrEmail_Returns400() throws Exception {
        // Arrange
        loginRequest.setUsernameOrEmail("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequestDTO.class));
    }

    @Test
    void login_MissingPassword_Returns400() throws Exception {
        // Arrange
        loginRequest.setPassword("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequestDTO.class));
    }

    // ==================== REGISTER TESTS ====================

    @Test
    void register_Success() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(authService, times(1)).register(any(RegisterRequestDTO.class));
    }

    @Test
    void register_DuplicateUsername_Returns409() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("Username already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));

        verify(authService, times(1)).register(any(RegisterRequestDTO.class));
    }

    @Test
    void register_InvalidEmail_Returns400() throws Exception {
        // Arrange
        registerRequest.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequestDTO.class));
    }

    @Test
    void register_PasswordTooShort_Returns400() throws Exception {
        // Arrange
        registerRequest.setPassword("12345"); // Less than 6 characters

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequestDTO.class));
    }

    @Test
    void register_MissingUsername_Returns400() throws Exception {
        // Arrange
        registerRequest.setUsername("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequestDTO.class));
    }
}