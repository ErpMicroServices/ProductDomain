# Docker Configuration for ProductDomain

This document describes the Docker and Docker Compose setup for the ProductDomain microservice.

## Overview

The ProductDomain microservice uses Docker for containerization with separate configurations for development and production environments. The setup includes:

- Multi-stage Dockerfiles for optimal image size and build caching
- Docker Compose configurations for both development and production
- PostgreSQL 16 for data persistence
- Redis 7 for caching
- Health checks for all services
- Security best practices (non-root users, read-only filesystems)

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- 4GB RAM minimum (8GB recommended)
- 10GB free disk space

## Quick Start

### Development Environment

1. Copy the environment example file:
   ```bash
   cp .env.example .env
   ```

2. Start the development environment:
   ```bash
   docker-compose -f docker-compose.dev.yml up
   ```

3. Access the services:
   - API: http://localhost:8080
   - GraphiQL: http://localhost:8080/graphiql
   - UI: http://localhost:3000
   - PostgreSQL: localhost:5432
   - Redis: localhost:6379

### Production Environment

1. Build the images:
   ```bash
   # Build API image
   docker build -t productdomain/api:latest ./api
   
   # Build UI image
   docker build -t productdomain/ui:latest ./ui-components
   ```

2. Set production environment variables:
   ```bash
   cp .env.example .env.prod
   # Edit .env.prod with production values
   ```

3. Start production services:
   ```bash
   docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
   ```

## Architecture

### Services

1. **PostgreSQL Database**
   - Version: 16-alpine
   - Port: 5432
   - Volume: postgres_data
   - Database schema managed by JPA/Hibernate

2. **Redis Cache**
   - Version: 7-alpine
   - Port: 6379
   - Volume: redis_data
   - Configured with AOF persistence

3. **API Service**
   - Spring Boot 3.2.0 application
   - Port: 8080
   - GraphQL endpoint: /graphql
   - Health check: /actuator/health

4. **UI Service**
   - React 18 with Vite
   - Port: 3000 (dev) / 80 (prod)
   - Nginx reverse proxy to API

### Networks

- **backend**: Internal network for database and cache services
- **frontend**: Network for API and UI communication

### Volumes

- **postgres_data**: PostgreSQL data persistence
- **redis_data**: Redis data persistence
- **gradle_cache**: Gradle dependencies cache (dev only)
- **node_modules**: Node dependencies (dev only)

## Development Features

The development configuration includes:

- **Hot Reload**: Source code mounted as volumes
- **Debug Support**: Java debug port 5005
- **Live Reload**: UI changes reload automatically
- **GraphiQL**: Enabled at /graphiql
- **Continuous Build**: Gradle watches for changes

## Production Features

The production configuration includes:

- **Security Hardening**:
  - Non-root users for all services
  - Read-only filesystems
  - No new privileges flag
  - Network isolation

- **Resource Limits**:
  - CPU and memory limits per service
  - Reservation guarantees

- **High Availability**:
  - Restart policies
  - Health checks with proper intervals
  - Graceful shutdown handling

- **Performance Optimization**:
  - Multi-stage builds
  - Layer caching
  - Alpine-based images
  - JVM container optimizations

## Environment Variables

Key environment variables (see .env.example for full list):

- `POSTGRES_DB`: Database name (default: productdomain)
- `POSTGRES_USER`: Database user (default: productuser)
- `POSTGRES_PASSWORD`: Database password (required)
- `REDIS_PASSWORD`: Redis password (required)
- `SPRING_PROFILES_ACTIVE`: Spring profile (dev/prod)
- `API_BASE_URL`: API URL for UI
- `UI_PORT`: UI external port

## Commands

### Development

```bash
# Start all services
docker-compose -f docker-compose.dev.yml up

# Start in background
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f [service]

# Stop services
docker-compose -f docker-compose.dev.yml down

# Stop and remove volumes
docker-compose -f docker-compose.dev.yml down -v
```

### Production

```bash
# Deploy services
docker-compose -f docker-compose.prod.yml up -d

# Scale API service
docker-compose -f docker-compose.prod.yml up -d --scale api=3

# Update a service
docker-compose -f docker-compose.prod.yml pull api
docker-compose -f docker-compose.prod.yml up -d api

# View service health
docker-compose -f docker-compose.prod.yml ps
```

### Maintenance

```bash
# Database backup
docker-compose exec postgres pg_dump -U productuser productdomain > backup.sql

# Redis backup
docker-compose exec redis redis-cli BGSAVE

# View resource usage
docker stats

# Clean up unused resources
docker system prune -a
```

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 8080, 3000, 5432, 6379 are available
2. **Memory issues**: Increase Docker Desktop memory allocation
3. **Build failures**: Clear caches with `docker builder prune`
4. **Network issues**: Recreate networks with `docker-compose down && docker-compose up`

### Health Checks

Monitor service health:
```bash
# Check all services
docker-compose ps

# Check specific service health
docker inspect productdomain-api | jq '.[0].State.Health'

# View health check logs
docker-compose logs api | grep -i health
```

## Security Considerations

1. **Secrets Management**:
   - Never commit .env files
   - Use Docker secrets in Swarm mode
   - Consider external secret management

2. **Network Security**:
   - Services isolated by network
   - Only UI port exposed externally
   - Internal services not accessible

3. **Image Security**:
   - Regular base image updates
   - Vulnerability scanning
   - Minimal attack surface with Alpine

4. **Runtime Security**:
   - Non-root user execution
   - Read-only filesystems
   - No privilege escalation

## Monitoring

For production monitoring, consider adding:

- Prometheus for metrics collection
- Grafana for visualization
- Jaeger for distributed tracing
- ELK stack for log aggregation