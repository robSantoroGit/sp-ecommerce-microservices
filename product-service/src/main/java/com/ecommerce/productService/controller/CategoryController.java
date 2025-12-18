package com.ecommerce.productService.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productService.dto.CategoryRequestDTO;
import com.ecommerce.productService.dto.CategoryResponseDTO;
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

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
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
	public ResponseEntity<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryRequestDTO dto) {

		CategoryResponseDTO created = categoryService.createCategory(dto);
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
	public ResponseEntity<CategoryResponseDTO> updateCategory(@PathVariable Long id,@Valid @RequestBody CategoryRequestDTO dto) {

		CategoryResponseDTO updated = categoryService.updateCategory(id, dto);
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
	public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
		categoryService.deleteCategory(id);
		return ResponseEntity.noContent().build();
	}
}