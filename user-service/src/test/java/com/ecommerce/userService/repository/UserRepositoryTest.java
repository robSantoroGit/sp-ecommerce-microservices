package com.ecommerce.userService.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;

import com.ecommerce.userService.bin.UserServiceApplication;
import com.ecommerce.userService.model.User;
import com.ecommerce.userService.model.UserRole;

@DataJpaTest
@ContextConfiguration(classes = UserServiceApplication.class) 
public class UserRepositoryTest {

	@Autowired
	private TestEntityManager entityManager;
	
	@Autowired
	private UserRepository userRepository;
	
	// Helper method
    private User createTestUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        return user;
    }
    
    @Test
    void testSaveUser() {
        // Given
        User user = createTestUser("john", "john@test.com");
        
        // When
        User savedUser = userRepository.save(user);
        
        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("john");
        assertThat(savedUser.getEmail()).isEqualTo("john@test.com");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void testFindById() {
        // Given
        User user = createTestUser("jane", "jane@test.com");
        User savedUser = entityManager.persistAndFlush(user);  // Salvo con entityManager
        
        // When
        Optional<User> foundUser = userRepository.findById(savedUser.getId());  // Cerco con repository
        
        // Then
        assertThat(foundUser).isPresent();  // Optional NON è vuoto
        assertThat(foundUser.get().getUsername()).isEqualTo("jane");
    }
    
    @Test
    void testFindByUsername() {
        // Given
        User user = createTestUser("bob", "bob@test.com");
        entityManager.persistAndFlush(user);
        
        // When
        Optional<User> foundUser = userRepository.findByUsername("bob");
        
        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("bob@test.com");
    }
    
    @Test
    void testFindByEmail() {
        // Given
        User user = createTestUser("alice", "alice@test.com");
        entityManager.persistAndFlush(user);
        
        // When
        Optional<User> foundUser = userRepository.findByEmail("alice@test.com");
        
        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("alice");
    }
    
    @Test
    void testFindByUsername_NotFound() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");
        
        // Then
        assertThat(foundUser).isEmpty();  // Optional vuoto
    }
    
    @Test
    void testExistsByUsername() {
        // Given
        User user = createTestUser("charlie", "charlie@test.com");
        entityManager.persistAndFlush(user);
        
        // When
        boolean exists = userRepository.existsByUsername("charlie");
        boolean notExists = userRepository.existsByUsername("nobody");
        
        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    
    @Test
    void testExistsByEmail() {
        // Given
        User user = createTestUser("dave", "dave@test.com");
        entityManager.persistAndFlush(user);
        
        // When
        boolean exists = userRepository.existsByEmail("dave@test.com");
        boolean notExists = userRepository.existsByEmail("nobody@test.com");
        
        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    
    @Test
    void testFindAll() {
        // Given
        User user1 = createTestUser("user1", "user1@test.com");
        User user2 = createTestUser("user2", "user2@test.com");
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        
        // When
        List<User> users = userRepository.findAll();
        
        // Then
        assertThat(users).hasSize(2);
    }
	
    @Test
    void testDeleteUser() {
        // Given
        User user = createTestUser("toDelete", "delete@test.com");
        User savedUser = entityManager.persistAndFlush(user);
        Long userId = savedUser.getId();
        
        // When
        userRepository.deleteById(userId);
        entityManager.flush();  // Forza scrittura su DB
        
        // Then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
    }
    
    @Test
    void testUpdateUser() {
        // Given
        User user = createTestUser("original", "original@test.com");
        User savedUser = entityManager.persistAndFlush(user);
        
        // When
        savedUser.setEmail("updated@test.com");
        savedUser.setFirstName("Updated");
        User updatedUser = userRepository.save(savedUser);
        entityManager.flush();
        
        // Then
        assertThat(updatedUser.getEmail()).isEqualTo("updated@test.com");
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
        assertThat(updatedUser.getUpdatedAt()).isAfter(updatedUser.getCreatedAt());
    }
    
}
