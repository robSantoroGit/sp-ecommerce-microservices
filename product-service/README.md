# Product Service

Microservice for managing the product catalog of the e-commerce platform.

## Technologies

- **Java 21**
- **Spring Boot 4.0.0**
- **Spring Data JPA**
- **PostgreSQL 15**
- **Docker & Docker Compose**
- **Maven**

## Features

- Complete product catalog management (CRUD)
- Product category management with many-to-many relationships
- Optimized queries to prevent N+1 problems
- Input validation with Bean Validation
- Centralized exception handling
- Health check endpoint for monitoring

## Project Structure
```
product-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ecommerce/productservice/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── model/
│   │   │       ├── dto/
│   │   │       └── exception/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-docker.yml
│   └── test/
├── docker/
│   ├── docker-compose.yml
│   └── .env
├── Dockerfile
└── pom.xml
```

## Requirements

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15 (if running locally without Docker)

## Configuration

### Environment Variables (.env)
```env
# Database Configuration
PRODUCT_DB_NAME=product_db
PRODUCT_DB_PORT=5433
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Service Configuration
PRODUCT_SERVICE_PORT=8082
SPRING_PROFILE=docker
```

### Spring Profiles

- **default**: Configuration for local development
- **docker**: Configuration for container execution

## Running the Service

### With Docker Compose
```bash
# Navigate to docker directory
cd docker

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f product-service

# Stop services
docker-compose down

# Remove volumes
docker-compose down -v
```

### Local Development
```bash
# Build the project
mvn clean install

# Run the service
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot.run.profiles=default
```

## API Endpoints

### Products
```
GET    /api/products              - Get all products
GET    /api/products/{id}         - Get product by ID
POST   /api/products              - Create new product
PUT    /api/products/{id}         - Update product
DELETE /api/products/{id}         - Delete product
GET    /api/products/category/{categoryId} - Get products by category
```

### Categories
```
GET    /api/categories            - Get all categories
GET    /api/categories/{id}       - Get category by ID
POST   /api/categories            - Create new category
PUT    /api/categories/{id}       - Update category
DELETE /api/categories/{id}       - Delete category
```

### Health Check
```
GET    /actuator/health           - Service health status
```

## Database Schema

### Product Table
```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    sku VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Category Table
```sql
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Product-Category Junction Table
```sql
CREATE TABLE product_categories (
    product_id BIGINT REFERENCES products(id),
    category_id BIGINT REFERENCES categories(id),
    PRIMARY KEY (product_id, category_id)
);
```

## Testing
```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=ProductServiceTest
```

## Docker

### Build Image
```bash
docker build -t product-service:latest .
```

### Run Container
```bash
docker run -d \
  -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-product:5432/product_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  --name product-service \
  product-service:latest
```

## Development Notes

### Query Optimization

The service implements several JPA query optimization techniques:

- **Entity Graphs**: Prevent N+1 queries by eagerly fetching related entities
- **JOIN FETCH**: Explicit joins in JPQL queries
- **Batch Fetching**: Configured batch size for collection fetching
- **DTO Projections**: Direct query to DTO to avoid unnecessary entity loading

### Exception Handling

Centralized exception handling with:
- `@ControllerAdvice` for global exception handling
- Custom exception classes for business logic errors
- Standardized error response format
- Proper HTTP status codes

## Port Configuration

- **Application Port**: 8082
- **PostgreSQL Port**: 5433 (mapped from container's 5432)

## Network

All services communicate through the `ecommerce-network` bridge network.

## Author

Rob - Java/Spring Trainer & Developer Coach

## License

This project is part of a comprehensive e-commerce microservices training platform.