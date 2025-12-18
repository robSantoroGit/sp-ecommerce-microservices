package com.ecommerce.userService.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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
import com.ecommerce.userService.exception.GlobalExceptionHandler;
import com.ecommerce.userService.model.UserRole;
import com.ecommerce.userService.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest( controllers = UserController.class)
@Import({UserController.class,GlobalExceptionHandler.class})
public class UserControllerTest2 {


	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	//@Autowired
	private ObjectMapper objectMapper = new ObjectMapper();


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
		when(userService.createUser(any(UserRequestDTO.class))).thenReturn(testResponseDTO);

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


}