package com.ecommerce.userService.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.userService.dto.UserMapper;
import com.ecommerce.userService.dto.UserRequestDTO;
import com.ecommerce.userService.dto.UserResponseDTO;
import com.ecommerce.userService.exception.DuplicateResourceException;
import com.ecommerce.userService.exception.ResourceNotFoundException;
import com.ecommerce.userService.model.User;
import com.ecommerce.userService.repository.UserRepository;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    // Constructor injection (no @Autowired needed in modern Spring)
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
    
    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
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
        
        // TODO: Hash password here (for now it's plain text)
        // user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Save to database
        User savedUser = userRepository.save(user);
        
        // Convert entity to DTO and return
        return userMapper.toDTO(savedUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
        
        return userMapper.toDTO(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public UserResponseDTO updateUser(Long id, UserRequestDTO userRequestDTO) {
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
        
        return userMapper.toDTO(updatedUser);
    }
    
    @Override
    public void deleteUser(Long id) {
        // Check if user exists
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        
        userRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username));
        
        return userMapper.toDTO(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
        
        return userMapper.toDTO(user);
    }
}