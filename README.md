# E-Commerce Microservices Platform

Production-ready e-commerce microservices system with Spring Boot 4, Docker, Kubernetes, and AWS.

## 🎯 Project Overview

Enterprise-grade microservices architecture demonstrating modern Java development practices, event-driven design, and cloud-native deployment strategies. Built as a comprehensive portfolio project.

## 🏗️ Architecture
```
                    ┌─────────────────────┐
                    │   API Gateway       │
                    │  Spring Cloud GW    │
                    │                     │
                    └──────────┬──────────┘
                               │
          ┌────────────────────┼────────────────────┬──────────────┐
          │                    │                    │              │
     ┌────▼─────┐        ┌─────▼─────┐       ┌─────▼─────┐   ┌───▼────────┐
     │   User   │        │  Product  │       │   Order   │   │Notification│
     │  Service │        │  Service  │       │  Service  │   │  Service   │
     │  :8081   │        │  :8082    │       │  :8083    │   │  :8084     │
     │    ✅    │       |    ✅     │       │    ✅    │   │    📋      │
     └────┬─────┘        └─────┬─────┘       └─────┬─────┘   └───┬────────┘
          │                    │                    │              │
          └────────────────────┼────────────────────┴──────────────┘
                               │                          │
                      ┌────────▼─────────┐       ┌────────▼─────────┐
                      │   PostgreSQL     │       │  Apache Kafka    │
                      │   (per service)  │       │                  │
                      └──────────────────┘       └──────────────────┘
```

**Legend:**
- ✅ Completed & Tested
- 🚧 In Development
- 📋 Planned

## 🛠️ Tech Stack

### Backend
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 4.0.0
- **ORM:** Spring Data JPA + Hibernate
- **Database:** PostgreSQL 15
- **Validation:** Jakarta Validation (Bean Validation)
- **Documentation:** SpringDoc OpenAPI 3.0

### Infrastructure
- **Containerization:** Docker & Docker Compose
- **Orchestration:** Kubernetes 
- **Cloud Platform:** AWS EKS, RDS, ECR
- **Message Broker:** Apache Kafka 7.6.0 (KRaft mode, no Zookeeper)

### Development Tools
- **Build Tool:** Maven 3.9+
- **IDE:** Eclipse IDE for Enterprise Java
- **Version Control:** Git + GitHub
- **API Testing:** Postman
- **Monitoring:** Actuator, Prometheus, Grafana 

### Resilience & Reliability
- **Circuit Breaker:** Resilience4j
- **Retry Mechanism:** Resilience4j Retry
- **REST Client:** Spring RestTemplate

### Testing
- **Unit Testing:** JUnit 5
- **Mocking:** Mockito 5
- **Assertions:** AssertJ
- **Test DB:** H2 (in-memory)

## 📦 Microservices

### ✅ User Service (Port 8081) - COMPLETED

**Status:** Production Ready | **Tests:** 37/37 passing

**Purpose:** User management, authentication base, profile handling

**Features:**
- CRUD operations for users
- Duplicate validation (username, email)
- Soft delete (active flag)
- Input validation with Jakarta Validation
- Global exception handling
- RESTful API with OpenAPI documentation

**Tech:**
- Spring Boot 4.0.0
- Spring Data JPA
- PostgreSQL 15
- Docker ready

**Endpoints:**
- `POST /api/users` - Create user
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/email/{email}` - Find by email
- `PUT /api/users/{id}` - Update user
- `PATCH /api/users/{id}/deactivate` - Soft delete
- `DELETE /api/users/{id}` - Hard delete

**Documentation:**
- API Docs: http://localhost:8081/api-docs
- Swagger UI: http://localhost:8081/swagger-ui.html
- Health Check: http://localhost:8081/actuator/health

---

### ✅ Product Service (Port 8082) - COMPLETED

**Status:** - Production ready| **Tests:** 42/42 passed

**Purpose:** Product catalog management with categories

**Features:**
- Category management (CRUD)
- Product management (CRUD)
- Product-Category relationship (ManyToOne)
- Stock management
- Price handling (BigDecimal)
- Search by name, price range, category
- Soft delete support

**Tech:**
- Spring Boot 4.0.0
- Spring Data JPA
- PostgreSQL 15 (port 5433)
- Docker ready

**Endpoints:**

*Products (8 endpoints):*
- `POST /api/products` - Create product
- `GET /api/products` - Get all products
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/category/{categoryId}` - Filter by category
- `GET /api/products/search?name=...` - Search by name
- `PUT /api/products/{id}` - Update product
- `PUT /api/products/{id}/stock` - Update stock
- `DELETE /api/products/{id}` - Delete product

*Categories (5 endpoints):*
- `POST /api/categories` - Create category
- `GET /api/categories` - Get all categories
- `GET /api/categories/{id}` - Get category by ID
- `PUT /api/categories/{id}` - Update category
- `DELETE /api/categories/{id}` - Delete category

**Documentation:**
- API Docs: http://localhost:8082/api-docs
- Swagger UI: http://localhost:8082/swagger-ui.html
- Health Check: http://localhost:8082/actuator/health

---

### ✅ Order Service (Port 8083) - COMPLETED

**Status:** Production Ready | **Tests:** 48/48 passing

**Purpose:** Order processing, inventory management, event-driven order lifecycle

**Features:**
- Complete order lifecycle management (creation, payment, status updates, cancellation)
- Inter-service communication with User and Product services
- Automatic inventory management (stock validation and updates)
- Payment processing with status transitions (PENDING → PAID → SHIPPED → DELIVERED)
- Event-driven architecture with Kafka producers
- Resilience patterns (Circuit Breaker, Retry) on external calls
- Order cancellation with automatic stock restoration
- Input validation with Jakarta Validation
- Global exception handling
- RESTful API with OpenAPI documentation

**Tech:**
- Spring Boot 4.0.0
- Spring Data JPA
- Spring Kafka (Producer)
- Resilience4j (Circuit Breaker, Retry)
- PostgreSQL 15 (port 5434)
- Apache Kafka 7.6.0
- Docker ready

**Endpoints:**
- `POST /api/orders` - Create new order
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders?userId={userId}` - Get orders by user (paginated)
- `POST /api/orders/{id}/payment` - Process payment
- `PUT /api/orders/{id}/status` - Update order status
- `DELETE /api/orders/{id}` - Cancel order (restores stock)

**Kafka Events Published:**
- `order.created` - When order is successfully created
- `order.status.changed` - When order status changes
- `order.cancelled` - When order is cancelled

**External Dependencies:**
- User Service (validates user existence)
- Product Service (validates products, manages stock)
- Kafka (event publishing)

**Documentation:**
- API Docs: http://localhost:8083/api-docs
- Swagger UI: http://localhost:8083/swagger-ui.html
- Health Check: http://localhost:8083/actuator/health

---

### 📋 Notification Service (Port 8084) - PLANNED

**Status:** 

**Purpose:** Event-driven notifications via Kafka

**Features (Planned):**
- Email notifications
- SMS notifications (optional)
- Kafka event consumers
- Order confirmation emails
- User registration emails
- Async processing

---

## 🗂️ Project Structure
```
ecommerce-microservices/
│
├── user-service/                    ✅ (Completed)
│   ├── src/
│   │   ├── main/java/com/ecommerce/userService/
│   │   │   ├── bin/                 Application main class
│   │   │   ├── model/               JPA entities
│   │   │   ├── repository/          Data access layer
│   │   │   ├── service/             Business logic
│   │   │   ├── dto/                 Data Transfer Objects
│   │   │   ├── exception/           Custom exceptions
│   │   │   ├── controller/          REST controllers
│   │   │   └── config/              Configuration classes
│   │   └── test/                    Unit & integration tests
│   ├── Dockerfile
│   └── README.md
│
├── product-service/                 ✅ (Completed)
│   ├── src/
│   │   ├── main/java/com/ecommerce/productService/
│   │   │   ├── bin/
│   │   │   ├── model/               Category, Product entities
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   ├── controller/
│   │   │   └── config/
│   │   └── test/
│   ├── Dockerfile
│   └── README.md
│
├── order-service/                   ✅ (Completed)
│   ├── src/
│   │   ├── main/java/com/ecommerce/orderservice/
│   │   │   ├── bin/
│   │   │   ├── model/               Order, OrderItem, OrderStatus
│   │   │   ├── repository/
│   │   │   ├── service/             OrderService, KafkaProducerService
│   │   │   ├── dto/
│   │   │   ├── mapper/
│   │   │   ├── client/              UserServiceClient, ProductServiceClient
│   │   │   ├── event/               Kafka event models
│   │   │   ├── exception/
│   │   │   ├── controller/
│   │   │   └── config/
│   │   └── test/
│   ├── Dockerfile
│   └── README.md
├── notification-service/            📋 (Planned)
│   └── (same structure)
│
├── infrastructure/
│   ├── docker/
│   │   ├── docker-compose.yml       All services orchestration
│   │   ├── .env                     Environment variables
│   │   ├── .env.example             Template for .env
│   │   └── README.md                Docker setup instructions
│   └── kubernetes/                  📋 
│       ├── user-service/
│       ├── product-service/
│       └── ...
│
├── docs/
│   ├── architecture/
│   │   ├── system-design.md
│   │   └── data-model.md
│   ├── api/
│   │   └── endpoints.md
│   └── deployment/
│       ├── local-setup.md
│       ├── kubernetes.md
│       └── aws-deploy.md
│
├── .gitignore
└── README.md
```

## 🚀 Getting Started

### Prerequisites

**Required:**
- Java 21+ (JDK)
- Eclipse IDE for Enterprise Java (2023-12 or later)
- Docker Desktop (20.10+)
- Docker Compose (2.0+)
- Maven 3.9+

**Optional:**
- PostgreSQL client (for direct DB access)
- Postman (for API testing)
- Git CLI

### Local Development Setup

#### 1. Clone Repository
```bash
git clone https://github.com/robSantoroGit/sp-ecommerce-microservices.git
cd sp-ecommerce-microservices
```

#### 2. Start Infrastructure (Databases)
```bash
cd infrastructure/docker

# Copy environment template
cp .env.example .env

# Start PostgreSQL databases
docker-compose up -d postgres-user postgres-product postgres-order kafka
```

**Verify databases are healthy:**
```bash
docker-compose ps
# Both postgres-user and postgres-product should show "Up (healthy)"
```

#### 3. Import Services in Eclipse

**User Service:**
- File → Import → Maven → Existing Maven Projects
- Browse to `user-service/`
- Select `pom.xml`
- Click Finish

**Other Services:**
- Repeat for `{other}-service/`

#### 4. Run Services

**User Service:**
- Right-click on `UserServiceApplication.java`
- Run As → Java Application
- Verify: http://localhost:8081/actuator/health

**Product Service:**
- Right-click on `ProductServiceApplication.java`
- Run As → Java Application
- Verify: http://localhost:8082/actuator/health

**Order Service:**
- Right-click on `OrderServiceApplication.java`
- Run As → Java Application
- Verify: http://localhost:8083/actuator/health

#### 5. Access Services

**User Service:**
- Swagger UI: http://localhost:8081/swagger-ui.html
- API Docs: http://localhost:8081/api-docs
- Health: http://localhost:8081/actuator/health

**Product Service:**
- Swagger UI: http://localhost:8082/swagger-ui.html
- API Docs: http://localhost:8082/api-docs
- Health: http://localhost:8082/actuator/health

**Order Service:**
- Swagger UI: http://localhost:8083/swagger-ui.html
- API Docs: http://localhost:8083/api-docs
- Health: http://localhost:8083/actuator/health

## 🧪 Testing

### Run All Tests (per service)
```bash
# User Service
cd user-service
./mvnw test

# Product Service
cd product-service
./mvnw test

# Order Service
cd order-service
./mvnw test
```

### Test Coverage

**User Service:**
- Repository Tests: 10/10 passing
- Service Tests: 14/14 passing
- Controller Tests: 13/13 passing
- **Total: 37/37 (100%)**

**Product Service (Target):**
- Repository Tests: 12/12 passing
- Service Tests: 15/15 passing
- Controller Tests: 15/15 passing
- **Total: 42 target (100%)**

**Order Service:**
- Repository Tests: 8/8 passing
- Service Tests: 20/20 passing
- Controller Tests: 12/12 passing
- Integration Tests: 8/8 passing
- **Total: 48/48 (100%)**

## 🐳 Docker

### Build Images
```bash
# User Service
docker build -t ecommerce-user-service:latest ./user-service

# Product Service
docker build -t ecommerce-product-service:latest ./product-service

# Order Service
docker build -t ecommerce-order-service:latest ./order-service
```

### Run with Docker Compose
```bash
cd infrastructure/docker
docker-compose up -d --build
```

This starts:
- PostgreSQL for User Service (port 5432)
- PostgreSQL for Product Service (port 5433)
- PostgreSQL for Order Service (port 5434)
- Apache Kafka 7.6.0 in KRaft mode (port 9092, no Zookeeper)
- User Service container (port 8081)
- Product Service container (port 8082)
- Order Service container (port 8083)

## 📖 Documentation

### Architecture
- [System Design](docs/architecture/system-design.md) *(Coming soon)*
- [Data Model](docs/architecture/data-model.md) *(In progress)*
- [API Contracts](docs/api/endpoints.md) *(Coming soon)*

### Deployment
- [Local Setup](docs/deployment/local-setup.md) *(Coming soon)*
- [Kubernetes Guide](docs/deployment/kubernetes.md) 
- [AWS Deployment](docs/deployment/aws-deploy.md) 

### Service Documentation
- [User Service README](user-service/README.md) ✅
- [Product Service README](product-service/README.md) ✅
- [Order Service README](order-service/README.md) ✅
- [Notification Service README](notification-service/README.md) 📋

## 📊 Development Status

### Completed ✅
- [x] Project architecture design
- [x] Repository setup & structure
- [x] Docker infrastructure (databases)
- [x] User Service - Backend core
- [x] User Service - REST API
- [x] User Service - Tests (37/37)
- [x] User Service - Docker containerization
- [x] User Service - Documentation
- [x] Product Service - Project setup
- [x] Product Service - Docker database
- [x] Product Service - Backend core
- [x] Product Service - REST API
- [x] Product Service - Tests
- [x] Order Service - Project setup
- [x] Order Service - Docker database
- [x] Order Service - Backend core (entities, repositories)
- [x] Order Service - REST clients (User/Product services)
- [x] Order Service - Resilience4j integration
- [x] Order Service - Kafka producer
- [x] Order Service - REST API (6 endpoints)
- [x] Order Service - Tests (48/48)
- [x] Order Service - Docker containerization
- [x] Order Service - Documentation
- [x] Kafka upgrade to 7.6.0 KRaft mode (removed Zookeeper)


### In Progress 🚧

### Planned 📋
- [ ] Notification Service
- [ ] Kafka integration
- [ ] API Gateway
- [ ] Security (JWT/OAuth2)
- [ ] Kubernetes deployment
- [ ] AWS cloud deployment
- [ ] CI/CD pipeline
- [ ] Monitoring & Logging

## 🔐 Security

Security features:
- JWT-based authentication
- OAuth2 integration
- Role-based access control (RBAC)
- Password hashing with BCrypt
- HTTPS/TLS configuration
- API rate limiting

## 📊 Monitoring & Observability

Monitoring stack:
- **Health Checks:** Spring Actuator (already configured)
- **Metrics:** Prometheus
- **Visualization:** Grafana dashboards
- **Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing:** Distributed tracing with Zipkin/Jaeger

## 🤝 Contributing

This is a portfolio project. The project demonstrates:
- Enterprise Java development best practices
- Microservices architecture patterns
- Cloud-native application design
- DevOps & deployment strategies

Feel free to fork and adapt for your own learning or portfolio.

## 📝 License

MIT License - see [LICENSE](LICENSE) file for details.

## 👤 Author

**Roberto Santoro** - Java/Spring Trainer & Developer Coach

Specializing in enterprise training, with focus on Java/Spring, microservices, and cloud technologies.

- **LinkedIn:** https://www.linkedin.com/in/roberto-santoro-646a03/
- **Email:** robesantoro@gmail.com
- **GitHub:** https://github.com/robSantoroGit

### About This Project

This project demonstrates:

✅ Production-ready microservices architecture
✅ Modern Java development (Java 21, Spring Boot 4)
✅ Test-driven development (comprehensive test coverage)
✅ DevOps practices (Docker, Kubernetes, CI/CD)
✅ Cloud deployment (AWS)

**Target Audience:** Junior-to-mid Java developers, recruiters evaluating technical skills, companies seeking enterprise Java expertise.

---

**Project Status:** 🚧 Active Development
**Current Focus:** API Gateway & Security (JWT)
**Last Updated:** December 2025

---