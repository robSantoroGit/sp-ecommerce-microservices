package com.ecommerce.productService.bin;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.ecommerce.productService.model.Category;
import com.ecommerce.productService.model.Product;
import com.ecommerce.productService.repository.CategoryRepository;
import com.ecommerce.productService.repository.ProductRepository;

@SpringBootApplication(scanBasePackages = {"com.ecommerce.productService.service","com.ecommerce.productService.controller",
		"com.ecommerce.productService.dto","com.ecommerce.productService.config","com.ecommerce.productService.exception"})
@EnableJpaRepositories(basePackages = "com.ecommerce.productService.repository")
@EntityScan(basePackages = "com.ecommerce.productService.model")
public class ProductServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
	}
	
	
	@Bean
    @Profile("dev")
    CommandLineRunner loadDevData(
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        
        return args -> {
            // Check se dati già esistono
            try {
				if (categoryRepository.count() > 0) {
				    System.out.println("\n" + "=".repeat(60));
				    System.out.println("⚠️  DEV DATA ALREADY EXISTS - Skipping data load");
				    System.out.println("=".repeat(60));
				    System.out.println("Current data:");
				    System.out.println("  • Categories: " + categoryRepository.count());
				    System.out.println("  • Products: " + productRepository.count());
				    System.out.println("\n💡 To reset database:");
				    System.out.println("   cd infrastructure/docker");
				    System.out.println("   docker-compose down -v");
				    System.out.println("   docker-compose up -d");
				    System.out.println("=".repeat(60) + "\n");
				    return;
				}
				
				System.out.println("\n" + "=".repeat(60));
				System.out.println("🔧 DEV MODE: Loading test data");
				System.out.println("=".repeat(60) + "\n");
				
				// ============ CATEGORIES ============
				System.out.println("📦 Creating categories...");
				
				Category electronics = new Category(
				    "Electronics", 
				    "Electronic devices, computers, and gadgets"
				);
				
				Category books = new Category(
				    "Books", 
				    "Physical and digital books, magazines"
				);
				
				Category clothing = new Category(
				    "Clothing", 
				    "Fashion, apparel, and accessories"
				);
				
				Category home = new Category(
				    "Home & Garden", 
				    "Furniture, decoration, and garden supplies"
				);
				
				categoryRepository.save(electronics);
				categoryRepository.save(books);
				categoryRepository.save(clothing);
				categoryRepository.save(home);
				
				System.out.println("   ✓ Created " + categoryRepository.count() + " categories");
				
				// ============ ELECTRONICS PRODUCTS ============
				System.out.println("\n💻 Creating Electronics products...");
				
				Product macbook = new Product(
				    "MacBook Pro 16", 
				    "Apple MacBook Pro 16-inch with M3 Max chip, 36GB RAM, 1TB SSD. " +
				    "Stunning Liquid Retina XDR display, advanced thermal design.",
				    new BigDecimal("2499.99"),
				    15
				);
				macbook.setCategory(electronics);
				
				Product iPhone = new Product(
				    "iPhone 15 Pro", 
				    "Latest iPhone with A17 Pro chip, titanium design, " +
				    "advanced camera system with 48MP main camera.",
				    new BigDecimal("1199.99"),
				    45
				);
				iPhone.setCategory(electronics);
				
				Product iPhoneInactive = new Product(
				    "iPhone 14", 
				    "Previous generation iPhone (discontinued)",
				    new BigDecimal("899.99"),
				    0
				);
				iPhoneInactive.setCategory(electronics);
				iPhoneInactive.setActive(false);  // Inactive product
				
				Product magicMouse = new Product(
				    "Magic Mouse", 
				    "Wireless, rechargeable mouse with Multi-Touch surface. " +
				    "Works seamlessly with Mac.",
				    new BigDecimal("79.99"),
				    120
				);
				magicMouse.setCategory(electronics);
				
				Product magicKeyboard = new Product(
				    "Magic Keyboard", 
				    "Wireless keyboard with numeric keypad, rechargeable battery. " +
				    "Extended layout, document navigation controls.",
				    new BigDecimal("149.99"),
				    80
				);
				magicKeyboard.setCategory(electronics);
				
				Product airpods = new Product(
				    "AirPods Pro", 
				    "Active Noise Cancellation, Transparency mode, " +
				    "Adaptive Audio, Personalized Spatial Audio.",
				    new BigDecimal("249.99"),
				    200
				);
				airpods.setCategory(electronics);
				
				Product ipad = new Product(
				    "iPad Air", 
				    "10.9-inch Liquid Retina display, M1 chip, " +
				    "Touch ID, 12MP camera with Center Stage.",
				    new BigDecimal("599.99"),
				    60
				);
				ipad.setCategory(electronics);
				
				productRepository.save(macbook);
				productRepository.save(iPhone);
				productRepository.save(iPhoneInactive);
				productRepository.save(magicMouse);
				productRepository.save(magicKeyboard);
				productRepository.save(airpods);
				productRepository.save(ipad);
				
				System.out.println("   ✓ Created 7 Electronics products (1 inactive)");
				
				// ============ BOOKS PRODUCTS ============
				System.out.println("\n📚 Creating Books products...");
				
				Product cleanCode = new Product(
				    "Clean Code", 
				    "A Handbook of Agile Software Craftsmanship by Robert C. Martin. " +
				    "Essential reading for any developer serious about their craft.",
				    new BigDecimal("45.99"),
				    150
				);
				cleanCode.setCategory(books);
				
				Product designPatterns = new Product(
				    "Design Patterns", 
				    "Elements of Reusable Object-Oriented Software by Gang of Four. " +
				    "Classic computer science text on design patterns.",
				    new BigDecimal("52.99"),
				    100
				);
				designPatterns.setCategory(books);
				
				Product refactoring = new Product(
				    "Refactoring", 
				    "Improving the Design of Existing Code by Martin Fowler. " +
				    "Learn how to improve code structure and maintainability.",
				    new BigDecimal("48.99"),
				    80
				);
				refactoring.setCategory(books);
				
				Product effectiveJava = new Product(
				    "Effective Java", 
				    "Best Practices for the Java Platform by Joshua Bloch. " +
				    "Essential for Java developers at all levels.",
				    new BigDecimal("49.99"),
				    120
				);
				effectiveJava.setCategory(books);
				
				Product springInAction = new Product(
				    "Spring in Action", 
				    "Spring Framework guide covering Spring Boot, microservices, " +
				    "reactive programming, and cloud-native development.",
				    new BigDecimal("44.99"),
				    90
				);
				springInAction.setCategory(books);
				
				productRepository.save(cleanCode);
				productRepository.save(designPatterns);
				productRepository.save(refactoring);
				productRepository.save(effectiveJava);
				productRepository.save(springInAction);
				
				System.out.println("   ✓ Created 5 Books products");
				
				// ============ CLOTHING PRODUCTS ============
				System.out.println("\n👕 Creating Clothing products...");
				
				Product tshirt = new Product(
				    "Classic Cotton T-Shirt", 
				    "100% cotton, comfortable fit, available in multiple colors. " +
				    "Perfect for casual everyday wear.",
				    new BigDecimal("19.99"),
				    500
				);
				tshirt.setCategory(clothing);
				
				Product jeans = new Product(
				    "Slim Fit Jeans", 
				    "Premium denim jeans with stretch, modern slim fit. " +
				    "Durable and comfortable for all-day wear.",
				    new BigDecimal("79.99"),
				    200
				);
				jeans.setCategory(clothing);
				
				Product jacket = new Product(
				    "Leather Jacket", 
				    "Genuine leather jacket, classic style, " +
				    "fully lined with interior pockets.",
				    new BigDecimal("299.99"),
				    30
				);
				jacket.setCategory(clothing);
				
				Product sneakers = new Product(
				    "Running Sneakers", 
				    "Lightweight running shoes with cushioned sole, " +
				    "breathable mesh upper, ideal for training.",
				    new BigDecimal("89.99"),
				    150
				);
				sneakers.setCategory(clothing);
				
				productRepository.save(tshirt);
				productRepository.save(jeans);
				productRepository.save(jacket);
				productRepository.save(sneakers);
				
				System.out.println("   ✓ Created 4 Clothing products");
				
				// ============ HOME & GARDEN PRODUCTS ============
				System.out.println("\n🏠 Creating Home & Garden products...");
				
				Product coffeeTable = new Product(
				    "Modern Coffee Table", 
				    "Scandinavian design coffee table, solid oak wood, " +
				    "minimalist style perfect for modern living rooms.",
				    new BigDecimal("349.99"),
				    25
				);
				coffeeTable.setCategory(home);
				
				Product deskLamp = new Product(
				    "LED Desk Lamp", 
				    "Adjustable LED desk lamp with touch control, " +
				    "multiple brightness levels, USB charging port.",
				    new BigDecimal("39.99"),
				    180
				);
				deskLamp.setCategory(home);
				
				Product plantPot = new Product(
				    "Ceramic Plant Pot Set", 
				    "Set of 3 modern ceramic pots with drainage holes, " +
				    "perfect for indoor plants and succulents.",
				    new BigDecimal("29.99"),
				    300
				);
				plantPot.setCategory(home);
				
				productRepository.save(coffeeTable);
				productRepository.save(deskLamp);
				productRepository.save(plantPot);
				
				System.out.println("   ✓ Created 3 Home & Garden products");
				
				// ============ SUMMARY ============
				System.out.println("\n" + "=".repeat(60));
				System.out.println("✅ DEV DATA LOADED SUCCESSFULLY!");
				System.out.println("=".repeat(60));
				
				long totalCategories = categoryRepository.count();
				long totalProducts = productRepository.count();
				long activeProducts = productRepository.findByActiveTrue().size();
				long inactiveProducts = totalProducts - activeProducts;
				
				System.out.println("Summary:");
				System.out.println("  • Categories: " + totalCategories);
				System.out.println("  • Total Products: " + totalProducts);
				System.out.println("  • Active Products: " + activeProducts);
				System.out.println("  • Inactive Products: " + inactiveProducts);
				
				System.out.println("\nBreakdown by category:");
				System.out.println("  • Electronics: " + 
				    productRepository.findByCategory(electronics).size());
				System.out.println("  • Books: " + 
				    productRepository.findByCategory(books).size());
				System.out.println("  • Clothing: " + 
				    productRepository.findByCategory(clothing).size());
				System.out.println("  • Home & Garden: " + 
				    productRepository.findByCategory(home).size());
				
				System.out.println("\n💡 Use Postman or Swagger UI to test the API:");
				System.out.println("   http://localhost:8082/swagger-ui.html");
				System.out.println("=".repeat(60) + "\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
        };
    }
    

}
