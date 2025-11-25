# User Service

User management microservice for E-commerce platform.

## Features

- User CRUD operations
- Role-based access (CUSTOMER, ADMIN)
- Input validation
- Global exception handling
- OpenAPI/Swagger documentation
- Docker support

## Tech Stack

- Java 21
- Spring Boot 4.0.0
- Spring Data JPA
- PostgreSQL
- Maven
- Docker

## API Endpoints

### Users
- `POST /api/users` - Create user
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/username/{username}` - Get user by username
- `GET /api/users/email/{email}` - Get user by email
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

## Run Locally

### Prerequisites
- Java 21
- Maven
- PostgreSQL (or Docker)

### With Docker Compose (Recommended)
```bash
cd ../infrastructure/docker
docker-compose up -d
```

### Manual Setup
```bash
# Start PostgreSQL
docker run -d --name postgres -e POSTGRES_DB=userdb -e POSTGRES_PASSWORD=password -p 5432:5432 postgres:15-alpine

# Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Testing
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UserControllerTest
```

**Test Coverage:**
- Repository: 10 tests
- Service: 14 tests
- Controller: 13 tests
- **Total: 37 tests**

## API Documentation

Once running, access Swagger UI:
```
http://localhost:8081/swagger-ui.html
```

## Configuration

See `src/main/resources/application*.yml` for configuration options.

### Profiles
- `dev` - Development (H2 or local PostgreSQL)
- `prod` - Production (PostgreSQL)

## Docker

### Build Image
```bash
docker build -t user-service:1.0.0 .
```

### Run Container
```bash
docker run -d -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/userdb \
  user-service:1.0.0
```