package com.ecommerce.userService.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.userService.dto.SecurityContext;
import com.ecommerce.userService.dto.UserRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;
import com.ecommerce.userService.exception.DuplicateResourceException;
import com.ecommerce.userService.exception.ForbiddenException;
import com.ecommerce.userService.exception.GlobalExceptionHandler;
import com.ecommerce.userService.exception.ResourceNotFoundException;
import com.ecommerce.userService.model.UserRole;
import com.ecommerce.userService.security.Permission;
import com.ecommerce.userService.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UserController.class)
@Import({UserController.class, GlobalExceptionHandler.class})
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @MockitoBean
    private UserService userService;
    
    private UserRequestDTO testRequestDTO;
    private UserResponseDTO testResponseDTO;
    
    @BeforeEach
    void setUp() {
    	
    	objectMapper.findAndRegisterModules(); // For LocalDateTime serialization

    	
        testRequestDTO = new UserRequestDTO();
        testRequestDTO.setUsername("testuser");
        testRequestDTO.setEmail("test@example.com");
        testRequestDTO.setPassword("password123");
        testRequestDTO.setFirstName("Test");
        testRequestDTO.setLastName("User");
        
        testResponseDTO = new UserResponseDTO();
        testResponseDTO.setId(1L);
        testResponseDTO.setUsername("testuser");
        testResponseDTO.setEmail("test@example.com");
        testResponseDTO.setFirstName("Test");
        testResponseDTO.setLastName("User");
        testResponseDTO.setRole(UserRole.CUSTOMER);
        testResponseDTO.setActive(true);
        testResponseDTO.setCreatedAt(LocalDateTime.now());
        testResponseDTO.setUpdatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCreateUser_Success() throws Exception {
        // Given
        when(userService.createUser(any(UserRequestDTO.class), any(SecurityContext.class))).thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_WRITE ))        		
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }
    
    @Test
    void testCreateUser_ValidationError() throws Exception {
        // Given - DTO invalido
        UserRequestDTO invalidDTO = new UserRequestDTO();
        invalidDTO.setUsername("ab");  // Troppo corto (min 3)
        invalidDTO.setEmail("invalid-email");  // Email non valida
        invalidDTO.setPassword("123");  // Troppo corta (min 6)
        
        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_WRITE ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }
    
    @Test
    void testCreateUser_DuplicateUsername() throws Exception {
        // Given
        when(userService.createUser(any(UserRequestDTO.class), any(SecurityContext.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: testuser"));
        
        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_WRITE ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Username already exists: testuser")));
    }
    
    @Test
    void createUser_AsNonAdmin_Returns403() throws Exception {
        // Arrange
        when(userService.createUser(any(UserRequestDTO.class), any(SecurityContext.class)))
                .thenThrow(new ForbiddenException("Admin access required"));

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequestDTO))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden());
    }

    
    @Test
    void testGetAllUsers() throws Exception {
        // Given
        UserResponseDTO user2 = new UserResponseDTO();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        
        List<UserResponseDTO> users = Arrays.asList(testResponseDTO, user2);
        when(userService.getAllUsers(any(SecurityContext.class))).thenReturn(users);
        
        // When & Then
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("testuser")))
                .andExpect(jsonPath("$[1].username", is("user2")));
    }
    
    @Test
    void getAllUsers_AsNonAdmin_Returns403() throws Exception {
        // Arrange
        when(userService.getAllUsers(any(SecurityContext.class)))
                .thenThrow(new ForbiddenException("Admin access required"));

        // Act & Assert
        mockMvc.perform(get("/api/users")
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void testGetUserById_Success() throws Exception {
        // Given
        when(userService.getUserById(anyLong(),any(SecurityContext.class))).thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(get("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")));
    }
    
    @Test
    void getUserById_AsOwner_Success() throws Exception {
        // Arrange
        when(userService.getUserById(eq(1L), any(SecurityContext.class))).thenReturn(testResponseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/users/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value(testRequestDTO.getUsername()));

        verify(userService).getUserById(eq(1L), any(SecurityContext.class));
    }
    
    @Test
    void getUserById_AsOtherUser_Returns403() throws Exception {
        // Arrange
        when(userService.getUserById(eq(1L), any(SecurityContext.class)))
                .thenThrow(new ForbiddenException("You can only access your own profile"));

        // Act & Assert
        mockMvc.perform(get("/api/users/1")
                        .header("X-User-Id", "99")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden());
    }
    
    @Test
    void testGetUserById_NotFound() throws Exception {
        // Given
        when(userService.getUserById(anyLong(),any(SecurityContext.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));
        
        // When & Then
        mockMvc.perform(get("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_READ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("User not found with id: 999")));
    }
    
    @Test
    void testGetUserByUsername() throws Exception {
        // Given
        when(userService.getUserByUsername(anyString(),any(SecurityContext.class))).thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(get("/api/users/username/testuser")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")));
    }
    
    @Test
    void testGetUserByEmail() throws Exception {
        // Given
        when(userService.getUserByEmail(anyString(),any(SecurityContext.class))).thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(get("/api/users/email/test@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }
    
    @Test
    void testUpdateUser_Success() throws Exception {
        // Given
        when(userService.updateUser(eq(1L), any(UserRequestDTO.class), any(SecurityContext.class)))
                .thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_WRITE ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")));
    }
    
    @Test
    void updateUser_AsOwner_Success() throws Exception {
        // Arrange
        when(userService.updateUser(eq(1L), any(UserRequestDTO.class), any(SecurityContext.class)))
                .thenReturn(testResponseDTO);

        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequestDTO))
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).updateUser(eq(1L), any(UserRequestDTO.class), any(SecurityContext.class));
    }

    @Test
    void updateUser_AsOtherUser_Returns403() throws Exception {
        // Arrange
        when(userService.updateUser(eq(1L), any(UserRequestDTO.class), any(SecurityContext.class)))
                .thenThrow(new ForbiddenException("You can only update your own profile"));

        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequestDTO))
                        .header("X-User-Id", "99")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden());
    }

    
    @Test
    void testUpdateUser_NotFound() throws Exception {
        // Given
        when(userService.updateUser(anyLong(), any(UserRequestDTO.class), any(SecurityContext.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: "));
        
        // When & Then
        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequestDTO))
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_WRITE ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
    
    @Test
    void testDeleteUser_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_DELETE ))
                .andExpect(status().isNoContent());
    }
    
    @Test
    void deleteUser_AsNonAdmin_Returns403() throws Exception {
        // Arrange
        doThrow(new ForbiddenException("Admin access required"))
                .when(userService).deleteUser(eq(1L), any(SecurityContext.class));

        // Act & Assert
        mockMvc.perform(delete("/api/users/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Scopes", ""))
                .andExpect(status().isForbidden());
    }

    
    @Test
    void testDeleteUser_NotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(userService).deleteUser(anyLong(),any(SecurityContext.class));
        
        // When & Then
        mockMvc.perform(delete("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "999")
                .header("X-User-Scopes", Permission.USER_DELETE ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}