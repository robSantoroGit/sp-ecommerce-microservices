# Docker Infrastructure

Local development environment with Docker Compose for all microservices.

## Prerequisites
- Docker Engine 20.10+
- Docker Compose 2.0+

## Services

| Service | Container | Host Port | Internal Port | Database | Status |
|---------|-----------|-----------|---------------|----------|--------|
| User DB | postgres-user | 5432 | 5432 | userdb | ✅ Active |
| Product DB | postgres-product | 5433 | 5432 | productdb | ✅ Active |
| User Service | user-service | 8081 | 8081 | - | ✅ Deployed |
| Product Service | product-service | 8082 | 8082 | - | 🚧 Development |

## Environment Configuration

All configurations are managed via `.env` file.

### Initial Setup
```bash
# Copy example file
cp .env.example .env

# Edit if needed (optional for local dev)
nano .env
```

### Environment Variables

See `.env.example` for all available configurations:
- Database credentials
- Service ports
- Database names
- Spring profiles

**⚠️ Security Note:** Never commit `.env` to Git. Only commit `.env.example`.

## Usage

### Start All Databases
```bash
docker-compose up -d postgres-user postgres-product
```

### Start Specific Database
```bash
# User Service DB only
docker-compose up -d postgres-user

# Product Service DB only
docker-compose up -d postgres-product
```

### Start Everything

When services are containerized:
```bash
docker-compose up -d --build
```

### Verify Services
```bash
# Check status
docker-compose ps

# All containers should show "Up (healthy)"
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f postgres-user
docker-compose logs -f postgres-product
docker-compose logs -f user-service
docker-compose logs -f product-service
```

### Stop Services
```bash
# Stop all
docker-compose down

# Stop and remove volumes (⚠️ deletes all data)
docker-compose down -v
```

## Database Access

### User Service Database

**Connection details:**
- Host: `localhost`
- Port: `5432`
- Database: `userdb`
- User: `postgres`
- Password: `postgres` (see `.env`)

**Connect with psql:**
```bash
docker exec -it postgres-user psql -U postgres -d userdb
```

**JDBC URL (local development):**
```
jdbc:postgresql://localhost:5432/userdb
```

**JDBC URL (within Docker network):**
```
jdbc:postgresql://postgres-user:5432/userdb
```

### Product Service Database

**Connection details:**
- Host: `localhost`
- Port: `5433`
- Database: `productdb`
- User: `postgres`
- Password: `postgres` (see `.env`)

**Connect with psql:**
```bash
docker exec -it postgres-product psql -U postgres -d productdb
```

**JDBC URL (local development):**
```
jdbc:postgresql://localhost:5433/productdb
```

**JDBC URL (within Docker network):**
```
jdbc:postgresql://postgres-product:5432/productdb
```

## Health Checks

All services include health checks:
```bash
# Check User Service DB
docker inspect postgres-user | grep Health -A 10

# Check Product Service DB
docker inspect postgres-product | grep Health -A 10
```

Or use Docker Compose:
```bash
docker-compose ps
# Look for "(healthy)" in State column
```

## Network

All services communicate via `ecommerce-network` (bridge driver).

Services can reach each other using container names:
- `postgres-user:5432`
- `postgres-product:5432`
- `user-service:8081`
- `product-service:8082`

## Volumes

Persistent data storage:

| Volume | Purpose | Container |
|--------|---------|-----------|
| user-data | User Service database | postgres-user |
| product-data | Product Service database | postgres-product |

**List volumes:**
```bash
docker volume ls | grep docker
```

**Inspect volume:**
```bash
docker volume inspect docker_user-data
docker volume inspect docker_product-data
```

## Troubleshooting

### Port Already in Use

**Problem:** `Bind for 0.0.0.0:5432 failed: port is already allocated`

**Solution:**
```bash
# Check what's using the port
lsof -i :5432
# Or on Windows
netstat -ano | findstr :5432

# Kill process or change port in .env
```

### Container Unhealthy

**Problem:** Container shows "unhealthy" status

**Solution:**
```bash
# Check logs
docker-compose logs postgres-user

# Restart container
docker-compose restart postgres-user

# If persists, recreate
docker-compose up -d --force-recreate postgres-user
```

### Connection Refused

**Problem:** Application can't connect to database

**Solution:**
1. Verify container is healthy: `docker-compose ps`
2. Check connection string matches port in `.env`
3. Check credentials in `.env` vs `application.yml`
4. Try connecting manually with psql

### Reset Everything

**Nuclear option - deletes all data:**
```bash
# Stop everything
docker-compose down -v

# Remove all containers, networks, volumes
docker system prune -a --volumes

# Restart
docker-compose up -d --build
```

## Development Workflow

### Daily Routine

**Morning:**
```bash
cd infrastructure/docker
docker-compose up -d postgres-user postgres-product
```

**Run services locally:**
```bash
# Terminal 1 - User Service
cd user-service
mvn spring-boot:run

# Terminal 2 - Product Service
cd product-service
mvn spring-boot:run
```

**Evening:**
```bash
docker-compose down
# Or leave running for next day
```

### Adding New Services

When adding Order Service, Notification Service, etc:

1. Update `.env` with new DB configuration
2. Add service to `docker-compose.yml`
3. Update this README

## Future Services

| Service | Port | Database | Status |
|---------|------|----------|--------|
| Order Service | 8083 | orderdb (5434) | 📋 Planned |
| Notification Service | 8084 | notificationdb (5435) | 📋 Planned |
| API Gateway | 8090 | - | 📋 Planned |
| Kafka | 9092 | - | 📋 Planned |

## Quick Reference
```bash
# Start databases only
docker-compose up -d postgres-user postgres-product

# Start everything
docker-compose up -d --build

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Stop
docker-compose down

# Reset (⚠️ deletes data)
docker-compose down -v
```

## Support

For issues:
1. Check logs: `docker-compose logs -f`
2. Verify health: `docker-compose ps`
3. Review troubleshooting section above
4. Check `.env` configuration matches `application.yml`