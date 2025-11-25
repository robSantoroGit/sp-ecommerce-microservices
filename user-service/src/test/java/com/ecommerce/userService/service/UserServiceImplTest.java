package com.ecommerce.userService.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.userService.dto.UserMapper;
import com.ecommerce.userService.dto.UserRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;
import com.ecommerce.userService.model.User;
import com.ecommerce.userService.model.UserRole;
import com.ecommerce.userService.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserMapper userMapper;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private User testUser;
    private UserRequestDTO testRequestDTO;
    private UserResponseDTO testResponseDTO;
    
    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setActive(true);
        
        // Setup request DTO
        testRequestDTO = new UserRequestDTO();
        testRequestDTO.setUsername("testuser");
        testRequestDTO.setEmail("test@example.com");
        testRequestDTO.setPassword("password123");
        
        // Setup response DTO
        testResponseDTO = new UserResponseDTO();
        testResponseDTO.setId(1L);
        testResponseDTO.setUsername("testuser");
        testResponseDTO.setEmail("test@example.com");
        testResponseDTO.setRole(UserRole.CUSTOMER);
        testResponseDTO.setActive(true);
    }
    
    @Test
    void testCreateUser_Success() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(UserRequestDTO.class))).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(testResponseDTO);
        
        // When
        UserResponseDTO result = userService.createUser(testRequestDTO);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        
        verify(userMapper).toEntity(any(UserRequestDTO.class));
        verify(userMapper).toDTO(any(User.class));
    }
}
