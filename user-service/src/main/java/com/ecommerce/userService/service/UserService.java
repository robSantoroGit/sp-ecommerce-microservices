package com.ecommerce.userService.service;

import java.util.List;

import com.ecommerce.userService.dto.UserRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;

public interface UserService {
    
    UserResponseDTO createUser(UserRequestDTO userRequestDTO);
    
    UserResponseDTO getUserById(Long id);
    
    List<UserResponseDTO> getAllUsers();
    
    UserResponseDTO updateUser(Long id, UserRequestDTO userRequestDTO);
    
    void deleteUser(Long id);
    
    UserResponseDTO getUserByUsername(String username);
    
    UserResponseDTO getUserByEmail(String email);
}