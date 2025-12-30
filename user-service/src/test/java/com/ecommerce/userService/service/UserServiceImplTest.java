package com.ecommerce.userService.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.userService.dto.SecurityContext;
import com.ecommerce.userService.dto.UserMapper;
import com.ecommerce.userService.dto.UserRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;
import com.ecommerce.userService.exception.DuplicateResourceException;
import com.ecommerce.userService.exception.ForbiddenException;
import com.ecommerce.userService.exception.ResourceNotFoundException;
import com.ecommerce.userService.model.User;
import com.ecommerce.userService.model.UserRole;
import com.ecommerce.userService.repository.UserRepository;
import com.ecommerce.userService.security.Permission;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private User testUser;
    private UserRequestDTO testRequestDTO;
    private UserResponseDTO testResponseDTO;
    
    private SecurityContext adminContext;
    private SecurityContext userContext;
    private SecurityContext otherUserContext;
    
    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setActive(true);
        
        // RBAC
        // Setup security contexts
//        adminContext = new SecurityContext(2L, "admin", List.of(UserRole.ADMIN.name()));
//        userContext = new SecurityContext(1L, "testuser", List.of(UserRole.CUSTOMER.name()));
//        otherUserContext = new SecurityContext(99L, "otheruser", List.of(UserRole.CUSTOMER.name()));
        
        // RBAC + ABAC
        adminContext = new SecurityContext(999L, "admin", List.of(
        	    Permission.USER_WRITE, Permission.USER_DELETE, Permission.USER_READ
        	));

        	userContext = new SecurityContext(1L, "testuser", List.of(
        		
        	));

        	otherUserContext = new SecurityContext(99L, "otheruser", List.of(
        		
        	));
        
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
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        
        // When
        UserResponseDTO result = userService.createUser(testRequestDTO, adminContext);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        
        verify(userMapper).toEntity(any(UserRequestDTO.class));
        verify(userMapper).toDTO(any(User.class));
        verify(passwordEncoder).encode(anyString());
    }
    
    @Test
    void createUser_AsNonAdmin_ThrowsForbiddenException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(testRequestDTO, userContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Admin access required");

        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testCreateUser_DuplicateUsername() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(true);  // Username esiste!
        
        // When & Then
        assertThatThrownBy(() -> userService.createUser(testRequestDTO, adminContext))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");
        
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));  // save() NON deve essere chiamato
    }
    
    @Test
    void testGetUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());  // Non trovato
        
        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L,adminContext))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
        
        verify(userRepository).findById(999L);
        verify(userMapper, never()).toDTO(any(User.class));  // toDTO() NON chiamato
    }
    
    @Test
    void getUserById_AsOwner_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(any(User.class))).thenReturn(testResponseDTO);

        // When
        UserResponseDTO result = userService.getUserById(1L, userContext);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_AsAdmin_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(any(User.class))).thenReturn(testResponseDTO);

        // When
        UserResponseDTO result = userService.getUserById(1L, adminContext);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_AsOtherUser_ThrowsForbiddenException() {
        
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(1L, otherUserContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only access your own profile");
    }

    @Test
    void testGetAllUsers() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        
        UserResponseDTO responseDTO2 = new UserResponseDTO();
        responseDTO2.setId(2L);
        responseDTO2.setUsername("user2");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));
        when(userMapper.toDTO(testUser)).thenReturn(testResponseDTO);
        when(userMapper.toDTO(user2)).thenReturn(responseDTO2);
        
        // When
        List<UserResponseDTO> result = userService.getAllUsers(adminContext);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
        assertThat(result.get(1).getUsername()).isEqualTo("user2");
        
        verify(userRepository).findAll();
        verify(userMapper, times(2)).toDTO(any(User.class));  // Chiamato 2 volte
    }
    
    @Test
    void getAllUsers_AsNonAdmin_ThrowsForbiddenException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.getAllUsers(userContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Admin access required");

        verify(userRepository, never()).findAll();
    }

    @Test
    void testUpdateUser_Success() {
        // Given
        UserRequestDTO updateDTO = new UserRequestDTO();
        updateDTO.setUsername("testuser");
        updateDTO.setEmail("newemail@example.com");  // Email nuova
        updateDTO.setPassword("password12345");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);  // Email non esiste
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(testResponseDTO);
        
        // When
        UserResponseDTO result = userService.updateUser(1L, updateDTO, adminContext);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testResponseDTO.getEmail());
        
        verify(userRepository).findById(1L);
        verify(userMapper).updateEntity(testUser, updateDTO);  // Verifica chiamata a updateEntity
        verify(userRepository).save(testUser);
    }
    
    @Test
    void updateUser_AsOwner_Success() {
        // Given
        UserRequestDTO updateDTO = new UserRequestDTO();
        updateDTO.setUsername("testuser");
        updateDTO.setEmail("newemail@example.com");  // E-mail nuova
        updateDTO.setPassword("password12345");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);  // Email non esiste
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(testResponseDTO);
        
        // When
        UserResponseDTO result = userService.updateUser(1L, updateDTO, userContext);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testResponseDTO.getEmail());
        
        verify(userRepository).findById(1L);
        verify(userMapper).updateEntity(testUser, updateDTO);  // Verifica chiamata a updateEntity
        verify(userRepository).save(testUser);
    }
    
    @Test
    void updateUser_AsOtherUser_ThrowsForbiddenException() {
        
        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, testRequestDTO, otherUserContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only update your own profile");
    }
    
    @Test
    void testDeleteUser_Success() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);  // doNothing() per metodi void
        
        // When
        userService.deleteUser(1L, adminContext);
        
        // Then
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_AsNonAdmin_ThrowsForbiddenException() {
        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(1L, userContext))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Admin access required");

        verify(userRepository, never()).delete(any(User.class));
    }
    

}
