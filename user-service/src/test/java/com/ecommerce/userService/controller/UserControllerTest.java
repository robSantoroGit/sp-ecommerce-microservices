package com.ecommerce.userService.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

import com.ecommerce.userService.dto.UserRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;
import com.ecommerce.userService.exception.DuplicateResourceException;
import com.ecommerce.userService.exception.GlobalExceptionHandler;
import com.ecommerce.userService.exception.ResourceNotFoundException;
import com.ecommerce.userService.model.UserRole;
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
        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequestDTO)))
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
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")));
    }
    
    @Test
    void testCreateUser_DuplicateUsername() throws Exception {
        // Given
        when(userService.createUser(any(UserRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: testuser"));
        
        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequestDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Username already exists: testuser")));
    }
    
    @Test
    void testGetAllUsers() throws Exception {
        // Given
        UserResponseDTO user2 = new UserResponseDTO();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        
        List<UserResponseDTO> users = Arrays.asList(testResponseDTO, user2);
        when(userService.getAllUsers()).thenReturn(users);
        
        // When & Then
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("testuser")))
                .andExpect(jsonPath("$[1].username", is("user2")));
    }
    
    @Test
    void testGetUserById_Success() throws Exception {
        // Given
        when(userService.getUserById(1L)).thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(get("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")));
    }
    
    @Test
    void testGetUserById_NotFound() throws Exception {
        // Given
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));
        
        // When & Then
        mockMvc.perform(get("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("User not found with id: 999")));
    }
    
    @Test
    void testGetUserByUsername() throws Exception {
        // Given
        when(userService.getUserByUsername("testuser")).thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(get("/api/users/username/testuser")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")));
    }
    
    @Test
    void testGetUserByEmail() throws Exception {
        // Given
        when(userService.getUserByEmail("test@example.com")).thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(get("/api/users/email/test@example.com")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }
    
    @Test
    void testUpdateUser_Success() throws Exception {
        // Given
        when(userService.updateUser(eq(1L), any(UserRequestDTO.class)))
                .thenReturn(testResponseDTO);
        
        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")));
    }
    
    @Test
    void testUpdateUser_NotFound() throws Exception {
        // Given
        when(userService.updateUser(eq(999L), any(UserRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));
        
        // When & Then
        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequestDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
    
    @Test
    void testDeleteUser_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
    
    @Test
    void testDeleteUser_NotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(userService).deleteUser(999L);
        
        // When & Then
        mockMvc.perform(delete("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}