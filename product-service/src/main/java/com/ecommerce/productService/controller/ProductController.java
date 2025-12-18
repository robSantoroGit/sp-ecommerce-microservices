package com.ecommerce.productService.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.productService.dto.ProductRequestDTO;
import com.ecommerce.productService.dto.ProductResponseDTO;
import com.ecommerce.productService.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Product management APIs")
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    
    /**
     * Create a new product
     * POST /api/products
     */
    @Operation(
            summary = "Create a new product",
            description = "Creates a new product with the provided information. Requires a valid category ID."
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "201",
                description = "Product created successfully",
                content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid input - validation failed"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Category not found"
            )
        })
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductRequestDTO dto) {
        
        ProductResponseDTO created = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    
    /**
     * Get all products
     * GET /api/products
     */
    @Operation(
            summary = "Get all products",
            description = "Retrieves a list of all products"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Products retrieved successfully"
            )
        })
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        List<ProductResponseDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    
    /**
     * Get product by ID
     * GET /api/products/{id}
     */
    @Operation(
            summary = "Get product by ID",
            description = "Retrieves a specific product by its ID"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Product found",
                content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Product not found"
            )
        })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {
        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    
    /**
     * Update product
     * PUT /api/products/{id}
     */
    @Operation(
            summary = "Update product",
            description = "Updates an existing product with new information"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Product updated successfully",
                content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid input - validation failed"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Product or category not found"
            )
        })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable Long id,@Valid @RequestBody ProductRequestDTO dto) {
        
        ProductResponseDTO updated = productService.updateProduct(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Delete product
     * DELETE /api/products/{id}
     */
    @Operation(
            summary = "Delete product",
            description = "Permanently deletes a product by its ID"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "204",
                description = "Product deleted successfully"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Product not found"
            )
        })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get products by category
     * GET /api/products/category/{categoryId}
     */
    @Operation(
            summary = "Get products by category",
            description = "Retrieves all products belonging to a specific category"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Products retrieved successfully"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Category not found"
            )
        })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByCategory(@PathVariable Long categoryId) {
        
        List<ProductResponseDTO> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }
    
    /**
     * Search products by name
     * GET /api/products/search?keyword={keyword}
     */
    @Operation(
            summary = "Search products by name",
            description = "Searches for products by name (case-insensitive partial match)"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Search completed successfully"
            )
        })
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDTO>> searchProducts(@RequestParam String keyword) {
        
        List<ProductResponseDTO> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }
    
    /**
     * Get products by price range
     * GET /api/products/price-range?min={min}&max={max}
     */
    @Operation(
            summary = "Get products by price range",
            description = "Retrieves products within a specified price range (inclusive)"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Products retrieved successfully"
            )
        })
    @GetMapping("/price-range")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByPriceRange(@RequestParam BigDecimal min,@RequestParam BigDecimal max) {
        
        List<ProductResponseDTO> products = productService.getProductsByPriceRange(min, max);
        return ResponseEntity.ok(products);
    }
    
    /**
     * Get only active products
     * GET /api/products/active
     */
    @Operation(
            summary = "Get active products only",
            description = "Retrieves all products that are currently active (not deactivated)"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Active products retrieved successfully"
            )
        })
    @GetMapping("/active")
    public ResponseEntity<List<ProductResponseDTO>> getActiveProducts() {
        List<ProductResponseDTO> products = productService.getActiveProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * Update product stock
     * PATCH /api/products/{id}/stock?quantity={quantity}
     */
    @Operation(
            summary = "Update product stock",
            description = "Updates the stock quantity for a specific product"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Stock updated successfully",
                content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Product not found"
            )
        })
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponseDTO> updateStock(@PathVariable Long id,@RequestParam Integer quantity) {
        
        ProductResponseDTO updated = productService.updateStock(id, quantity);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Deactivate product (soft delete)
     * PATCH /api/products/{id}/deactivate
     */
    @Operation(
            summary = "Deactivate product",
            description = "Soft deletes a product by setting its active flag to false"
        )
        @ApiResponses(value = {
            @ApiResponse(
                responseCode = "200",
                description = "Product deactivated successfully",
                content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Product not found"
            )
        })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProductResponseDTO> deactivateProduct(@PathVariable Long id) {
        ProductResponseDTO deactivated = productService.deactivateProduct(id);
        return ResponseEntity.ok(deactivated);
    }
}