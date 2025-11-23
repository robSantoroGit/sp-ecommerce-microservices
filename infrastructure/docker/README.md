# Docker Infrastructure

Local development environment with Docker Compose.

## Services

- **postgres-user**: PostgreSQL for User Service (port 5432)

## Usage

Start all services:
```bash
docker-compose up -d
```

Stop all services:
```bash
docker-compose down
```

Stop and remove volumes (⚠️ deletes data):
```bash
docker-compose down -v
```

View logs:
```bash
docker-compose logs -f postgres-user
```

## Database Access

**Connection details:**
- Host: `localhost`
- Port: `5432`
- Database: `userdb`
- User: `postgres`
- Password: `postgres`

**Connect with psql:**
```bash
docker exec -it postgres-user psql -U postgres -d userdb
```