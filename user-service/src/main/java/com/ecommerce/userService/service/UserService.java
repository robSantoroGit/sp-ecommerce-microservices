package com.ecommerce.userService.service;

import java.util.List;

import com.ecommerce.userService.dto.SecurityContext;
import com.ecommerce.userService.dto.UserRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;

public interface UserService {
    
    UserResponseDTO createUser(UserRequestDTO userRequestDTO, SecurityContext securityContext);
    
    UserResponseDTO getUserById(Long id, SecurityContext securityContext);
    
    List<UserResponseDTO> getAllUsers(SecurityContext securityContext);
    
    UserResponseDTO updateUser(Long id, UserRequestDTO userRequestDTO, SecurityContext securityContext);
    
    void deleteUser(Long id, SecurityContext securityContext);
    
    UserResponseDTO getUserByUsername(String username, SecurityContext securityContext);
    
    UserResponseDTO getUserByEmail(String email, SecurityContext securityContext);
}