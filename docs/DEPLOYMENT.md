# Deployment Guide

## Prerequisites

- Docker and Docker Compose
- JDK 17 (for local development)
- MySQL 8.0
- Redis 7

## Environment Setup

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and set required variables:
   - `MYSQL_PASSWORD`: MySQL root password
   - `AUTH_SIGNING_KEY`: JWT signing key (min 32 bytes)
   - `MINIMAX_API_KEY`: Your AI model API key

## Docker Compose Deployment

Start all services:
```bash
docker-compose up -d
```

Check health:
```bash
docker-compose ps
curl http://localhost:8080/actuator/health
```

View logs:
```bash
docker-compose logs -f backend
```

Stop services:
```bash
docker-compose down
```

## Manual Deployment

### Build

```bash
mvn clean package -DskipTests
```

### Run

```bash
java -jar target/hbti-coach-1.0-SNAPSHOT.jar
```

Required environment variables must be set before running.

## Database Migration

Flyway migrations run automatically on startup. To check migration status:

```bash
mvn flyway:info
```

## Health Checks

- Application health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- API docs: `http://localhost:8080/doc.html`

## Production Checklist

- [ ] Set `AUTH_SECURE_COOKIES=true`
- [ ] Use strong `AUTH_SIGNING_KEY` (min 32 bytes)
- [ ] Configure MySQL with strong password
- [ ] Enable Redis persistence if needed
- [ ] Configure CORS for production domain
- [ ] Set up SSL/TLS termination
- [ ] Configure log aggregation
- [ ] Set up monitoring and alerts
- [ ] Test backup and restore procedures
- [ ] Document rollback process
- [ ] Run load tests
- [ ] Complete security review
