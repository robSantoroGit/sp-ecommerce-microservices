# sp-ecommerce-microservices
Production-ready e-commerce microservices system with Spring Boot, Docker, Kubernetes, and AWS

## 🏗️ Architecture
```
                    ┌─────────────────┐
                    │   API Gateway   │
                    │ Spring Cloud GW │
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
     ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
     │  User   │       │ Product │       │  Order  │
     │ Service │       │ Service │       │ Service │
     └────┬────┘       └────┬────┘       └────┬────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                      ┌──────▼─────┐
                      │ PostgreSQL │
                      │    (RDS)   │
                      └────────────┘
```

## 🛠️ Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.2.x
- **Database:** PostgreSQL
- **Message Broker:** Apache Kafka
- **Containerization:** Docker
- **Orchestration:** Kubernetes
- **Cloud:** AWS (EKS, RDS, ECR)
- **CI/CD:** GitHub Actions
- **Monitoring:** Prometheus + Grafana
- **IDE:** Eclipse

## 📦 Microservices

### User Service
Authentication, user management, and profiles.

**Endpoints:**
- `POST /api/users` - Create user
- `GET /api/users` - List users
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Product Service
*(Coming soon)*

### Order Service
*(Coming soon)*

### Notification Service
*(Coming soon)*

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Eclipse IDE (2023-12 or later)
- Docker Desktop
- Maven 3.8+
- PostgreSQL client (optional, for direct DB access)

### Local Development Setup

1. **Clone repository**
```bash
git clone https://github.com/robSantoroGit/ecommerce-microservices.git
cd ecommerce-microservices
```

2. **Start infrastructure**
```bash
cd infrastructure/docker
docker-compose up -d
```

3. **Import in Eclipse**
   - File → Import → Maven → Existing Maven Projects
   - Select `user-service` folder
   - Click Finish

4. **Run User Service**
   - Right-click on project → Run As → Spring Boot App

5. **Access Swagger UI**
```
http://localhost:8081/swagger-ui.html
```

## 📖 Documentation

- [Architecture Details](docs/architecture.md) *(Coming soon)*
- [API Documentation](docs/api.md) *(Coming soon)*
- [Deployment Guide](docs/deployment.md) *(Coming soon)*

## 🧪 Testing
```bash
# Run all tests
./mvnw test

# Run specific service tests
cd user-service
./mvnw test
```

## 🐳 Docker

Build and run with Docker:
```bash
# Build image
docker build -t ecommerce-user-service ./user-service

# Run container
docker run -p 8081:8081 ecommerce-user-service
```

## ☸️ Kubernetes

*(Coming soon)*

## 🔐 Security

*(Coming soon)*

## 📊 Monitoring

*(Coming soon)*

## 🤝 Contributing

This is a portfolio/learning project. Feel free to fork and adapt for your own use.

## 📝 License

MIT License - see LICENSE file for details

## 👤 Author

**Rob** - Java/Spring Trainer & Developer Coach
- LinkedIn: https://www.linkedin.com/in/roberto-santoro-646a03/
- Email: robesantoro@gmail.com

---

**Status:** 🚧 Work in Progress - User Service ✅ Complete