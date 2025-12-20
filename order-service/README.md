# Order Service

Microservice for managing orders and order processing in the e-commerce platform.

## Technologies

- **Java 21**
- **Spring Boot 4.0.0**
- **Spring Data JPA**
- **PostgreSQL 15**
- **Apache Kafka 7.6.0 (KRaft mode)**
- **Spring Kafka**
- **Resilience4j** (Circuit Breaker, Retry)
- **Docker & Docker Compose**
- **Maven**

## Features

- Complete order lifecycle management (creation, payment, status updates, cancellation)
- Inter-service communication with User and Product services via REST
- Automatic inventory management with stock validation and updates
- Event-driven architecture with Kafka producers
- Payment processing with status transitions
- Resilience patterns (Circuit Breaker, Retry) for external service calls
- Order cancellation with automatic stock restoration
- Input validation with Bean Validation
- Centralized exception handling
- Health check endpoint for monitoring

## Project Structure
```
order-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ecommerce/orderservice/
│   │   │       ├── controller/      # REST endpoints
│   │   │       ├── service/         # Business logic
│   │   │       ├── repository/      # JPA repositories
│   │   │       ├── model/           # Entities (Order, OrderItem, OrderStatus)
│   │   │       ├── dto/             # Request/Response DTOs + inter-service DTOs
│   │   │       ├── mapper/          # Entity ↔ DTO mappers
│   │   │       ├── client/          # REST clients (UserServiceClient, ProductServiceClient)
│   │   │       ├── event/           # Kafka event models
│   │   │       └── exception/       # Custom exceptions + GlobalExceptionHandler
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-docker.yml
│   └── test/
│       ├── java/
│       │   └── com/ecommerce/orderservice/
│       │       ├── repository/      # Repository tests
│       │       ├── service/         # Service tests with mocked clients
│       │       ├── controller/      # Controller tests
│       │       └── integration/     # Integration tests
│       └── resources/
│           ├── application.yml
│           └── schema.sql
├── docker/
│   ├── docker-compose.yml
│   └── .env
├── Dockerfile
├── .dockerignore
└── pom.xml
```

## Requirements

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15 (if running locally without Docker)
- Apache Kafka 7.6+ with KRaft mode (if running locally without Docker)

## Configuration

### Environment Variables (.env)
```env
# Database Configuration
ORDER_DB_NAME=orderdb
ORDER_DB_PORT=5434
ORDER_DB_USER=orderuser
ORDER_DB_PASSWORD=orderpass

# Service Configuration
ORDER_SERVICE_PORT=8083
SPRING_PROFILE=docker

# External Services
USER_SERVICE_URL=http://user-service:8081
PRODUCT_SERVICE_URL=http://product-service:8082

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

### Spring Profiles

- **default**: Configuration for local development
- **docker**: Configuration for container execution
- **test**: Configuration for test execution (H2 in-memory database)

## Running the Service

### With Docker Compose (Recommended)
```bash
# Navigate to docker directory
cd docker

# Start all services (Order Service + PostgreSQL + Kafka)
docker-compose up -d

# View logs
docker-compose logs -f order-service

# Stop services
docker-compose down

# Remove volumes
docker-compose down -v
```

### Local Development
```bash
# Prerequisites: PostgreSQL and Kafka must be running

# Build the project
mvn clean install

# Run the service
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=default
```

## API Endpoints

### Orders
```
POST   /api/orders                    - Create new order
GET    /api/orders/{id}               - Get order by ID
GET    /api/orders?userId={userId}    - Get orders by user (paginated)
POST   /api/orders/{id}/payment       - Process payment for order
PUT    /api/orders/{id}/status        - Update order status
DELETE /api/orders/{id}               - Cancel order (restores stock)
```

### Health Check
```
GET    /actuator/health               - Service health status
```

### Example Requests

#### Create Order
```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "deliveryAddress": "Via Roma 1, Naples, Italy",
    "items": [
      {
        "productId": 1,
        "quantity": 2
      },
      {
        "productId": 3,
        "quantity": 1
      }
    ]
  }'
```

#### Process Payment
```bash
curl -X POST http://localhost:8083/api/orders/1/payment \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "CREDIT_CARD",
    "amount": 1500.00
  }'
```

#### Get Orders by User (Paginated)
```bash
curl "http://localhost:8083/api/orders?userId=1&page=0&size=10"
```

## Database Schema

### Order Table
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    delivery_address TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
```

### OrderItem Table
```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
```

## Order Status Flow

```
PENDING → PAID → SHIPPED → DELIVERED
   ↓
CANCELLED (only from PENDING)
```

**Valid Status Transitions:**
- PENDING → PAID (after successful payment)
- PAID → SHIPPED (when order is shipped)
- SHIPPED → DELIVERED (when order arrives)
- PENDING → CANCELLED (cancellation before payment)

## External Service Integration

### User Service Client
- **Purpose**: Validate user existence before creating order
- **Endpoint**: `GET /api/users/{userId}`
- **Resilience**: Circuit Breaker + Retry (3 attempts, 1s delay)

### Product Service Client
- **Operations**:
  1. Get product details (validate existence, get price)
  2. Validate stock availability
  3. Update stock after order creation/cancellation
- **Endpoints**:
  - `GET /api/products/{productId}`
  - `PUT /api/products/{productId}/stock`
- **Resilience**: Circuit Breaker + Retry (3 attempts, 1s delay)

### Resilience4j Configuration
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10000ms
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 1000ms
```

## Kafka Event Publishing

### Events Published

1. **order.created** - Published when order is successfully created
   ```json
   {
     "eventType": "ORDER_CREATED",
     "orderId": 1,
     "userId": 1,
     "totalAmount": 1500.00,
     "timestamp": "2024-12-20T10:30:00Z"
   }
   ```

2. **order.status.changed** - Published when order status changes
   ```json
   {
     "eventType": "ORDER_STATUS_CHANGED",
     "orderId": 1,
     "oldStatus": "PENDING",
     "newStatus": "PAID",
     "timestamp": "2024-12-20T10:35:00Z"
   }
   ```

3. **order.cancelled** - Published when order is cancelled
   ```json
   {
     "eventType": "ORDER_CANCELLED",
     "orderId": 1,
     "userId": 1,
     "refundAmount": 1500.00,
     "timestamp": "2024-12-20T10:40:00Z"
   }
   ```

### Testing Kafka Events
```bash
# List topics
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume order.created events
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order.created \
  --from-beginning

# Consume all order events
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order.created,order.status.changed,order.cancelled \
  --from-beginning
```

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=OrderServiceTest

# Run integration tests only
mvn test -Dtest=OrderIntegrationTest
```

### Test Coverage
- **Repository Tests**: JPA query validation with `@DataJpaTest`
- **Service Tests**: Business logic with mocked repositories and clients
- **Controller Tests**: REST endpoints with `@WebMvcTest`
- **Integration Tests**: Full workflow with `@SpringBootTest`

## Business Logic

### Order Creation Flow
1. Validate user exists (UserServiceClient)
2. For each order item:
   - Validate product exists
   - Get current price
   - Validate stock availability
3. Calculate total amount
4. Create order with status PENDING
5. Update product stock (reduce)
6. Publish `order.created` event to Kafka

### Payment Processing Flow
1. Validate order exists and status is PENDING
2. Process payment (embedded logic)
3. Update order status to PAID
4. Publish `order.status.changed` event to Kafka

### Order Cancellation Flow
1. Validate order exists and status is PENDING
2. For each order item:
   - Restore product stock (increase)
3. Update order status to CANCELLED
4. Publish `order.cancelled` event to Kafka

## Docker

### Build Image
```bash
docker build -t order-service:latest .
```

### Run Container
```bash
docker run -d \
  -p 8083:8083 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-order:5432/orderdb \
  -e SPRING_DATASOURCE_USERNAME=orderuser \
  -e SPRING_DATASOURCE_PASSWORD=orderpass \
  -e USER_SERVICE_URL=http://user-service:8081 \
  -e PRODUCT_SERVICE_URL=http://product-service:8082 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  --name order-service \
  order-service:latest
```

## Development Notes

### N+1 Query Prevention

The service prevents N+1 queries using:
- **JOIN FETCH**: In `OrderRepository.findByIdWithItems()` to eagerly load OrderItems
- **Entity Graph**: In `findByUserIdWithItems()` for paginated queries

### Exception Handling

Custom exceptions for specific business scenarios:
- `OrderNotFoundException`: Order not found by ID
- `UserNotFoundException`: User validation failed
- `ProductNotFoundException`: Product validation failed
- `InsufficientStockException`: Not enough stock for order
- `InvalidOrderStatusException`: Invalid status transition
- `PaymentProcessingException`: Payment failed
- `OrderCancellationException`: Cancellation not allowed

All exceptions handled by `GlobalExceptionHandler` with proper HTTP status codes.

### Transaction Management

- `@Transactional` on service methods for consistency
- Stock updates are atomic within order creation/cancellation
- Rollback on any failure (database, external service, Kafka)

## Port Configuration

- **Application Port**: 8083
- **PostgreSQL Port**: 5434 (mapped from container's 5432)

## Network

All services communicate through the `ecommerce-network` bridge network.

## Dependencies

This service depends on:
- **postgres-order**: PostgreSQL database (healthy)
- **user-service**: User validation (started)
- **product-service**: Product operations (started)
- **kafka**: Event publishing (healthy)

## Future Enhancements

- [ ] Separate Payment Service (currently embedded)
- [ ] Order tracking updates
- [ ] Email notifications on order events (via Kafka consumer)
- [ ] Order history with detailed audit log
- [ ] Support for multiple payment methods
- [ ] Inventory reservation before payment
- [ ] Shipping integration
- [ ] Invoice generation

## Author

Rob - Java/Spring Trainer & Developer Coach

## License

This project is part of a comprehensive e-commerce microservices training platform.