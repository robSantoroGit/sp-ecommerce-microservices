package com.ecommerce.productService.controller;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productService.dto.CategoryRequestDTO;
import com.ecommerce.productService.dto.CategoryResponseDTO;
import com.ecommerce.productService.dto.SecurityContext;
import com.ecommerce.productService.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category", description = "Category management APIs")
public class CategoryController {

	private final CategoryService categoryService;
	private static final Logger log = LoggerFactory.getLogger(ProductController.class);

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}
	
	private SecurityContext createSecurityContext(Long userId, String scopesHeader) {
	    List<String> scopes = Arrays.asList(scopesHeader.split(","));
	    return new SecurityContext(userId, "user-" + userId, scopes);
	}

	/**
	 * Create a new category
	 * POST /api/categories
	 */
	@Operation(
			summary = "Create a new category",
			description = "Creates a new product category with the provided information"
			)
		@ApiResponses(value = {
			@ApiResponse(
					responseCode = "201",
					description = "Category created successfully",
					content = @Content(schema = @Schema(implementation = CategoryResponseDTO.class))
					),
			@ApiResponse(
					responseCode = "400",
					description = "Invalid input - validation failed"
					),
			@ApiResponse(
					responseCode = "409",
					description = "Category with this name already exists"
					)
	})
	@PostMapping
	public ResponseEntity<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryRequestDTO dto,
			@RequestHeader("X-User-Id") Long authenticatedUserId,
	        @RequestHeader("X-User-Scopes") String scopesHeader) {
		log.info("POST /api/categories - Create category attempt");
		SecurityContext securityContext = createSecurityContext(authenticatedUserId, scopesHeader);
		CategoryResponseDTO created = categoryService.createCategory(dto,securityContext);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	/**
	 * Get all categories
	 * GET /api/categories
	 */
	@Operation(
			summary = "Get all categories",
			description = "Retrieves a list of all product categories"
			)
		@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Categories retrieved successfully"
					)
	})
	@GetMapping
	public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
		log.info("GET /api/categories - Get all categories");
		List<CategoryResponseDTO> categories = categoryService.getAllCategories();
		return ResponseEntity.ok(categories);
	}

	/**
	 * Get category by ID
	 * GET /api/categories/{id}
	 */
	@Operation(
	        summary = "Get category by ID",
	        description = "Retrieves a specific category by its ID"
	    )
	    @ApiResponses(value = {
	        @ApiResponse(
	            responseCode = "200",
	            description = "Category found",
	            content = @Content(schema = @Schema(implementation = CategoryResponseDTO.class))
	        ),
	        @ApiResponse(
	            responseCode = "404",
	            description = "Category not found"
	        )
	    })
	@GetMapping("/{id}")
	public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
		log.info("GET /api/categories/{} - Get category by id",id);
		CategoryResponseDTO category = categoryService.getCategoryById(id);
		return ResponseEntity.ok(category);
	}

	/**
	 * Update category
	 * PUT /api/categories/{id}
	 */
	@Operation(
	        summary = "Update category",
	        description = "Updates an existing category with new information"
	    )
	    @ApiResponses(value = {
	        @ApiResponse(
	            responseCode = "200",
	            description = "Category updated successfully",
	            content = @Content(schema = @Schema(implementation = CategoryResponseDTO.class))
	        ),
	        @ApiResponse(
	            responseCode = "400",
	            description = "Invalid input - validation failed"
	        ),
	        @ApiResponse(
	            responseCode = "404",
	            description = "Category not found"
	        ),
	        @ApiResponse(
	            responseCode = "409",
	            description = "Category name already in use"
	        )
	    })
	@PutMapping("/{id}")
	public ResponseEntity<CategoryResponseDTO> updateCategory(@PathVariable Long id,
			@Valid @RequestBody CategoryRequestDTO dto,
			@RequestHeader("X-User-Id") Long authenticatedUserId,
	        @RequestHeader("X-User-Scopes") String scopesHeader) {
		log.info("PUT /api/categories/{} - Update category with id",id);
		SecurityContext securityContext = createSecurityContext(authenticatedUserId, scopesHeader);
		CategoryResponseDTO updated = categoryService.updateCategory(id, dto, securityContext);
		return ResponseEntity.ok(updated);
	}

	/**
	 * Delete category
	 * DELETE /api/categories/{id}
	 */
	 @Operation(
		        summary = "Delete category",
		        description = "Deletes a category by its ID"
		    )
		    @ApiResponses(value = {
		        @ApiResponse(
		            responseCode = "204",
		            description = "Category deleted successfully"
		        ),
		        @ApiResponse(
		            responseCode = "404",
		            description = "Category not found"
		        )
		    })
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCategory(@PathVariable Long id,
			@RequestHeader("X-User-Id") Long authenticatedUserId,
	        @RequestHeader("X-User-Scopes") String scopesHeader) {
		 log.info("DELETE /api/categories/{} - Delete category by id",id);
		 SecurityContext securityContext = createSecurityContext(authenticatedUserId, scopesHeader);
		 categoryService.deleteCategory(id,securityContext);
		return ResponseEntity.noContent().build();
	}
}