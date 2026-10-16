# Docker Lifecycle & Operations Guide

This document provides a comprehensive operational guide for containerizing, managing, and troubleshooting the **Self-Service Password Reset (SSPR) Platform**.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Building Images](#building-images)
3. [Running Containers](#running-containers)
4. [Container Lifecycle Commands](#container-lifecycle-commands)
5. [Docker Compose Orchestration](#docker-compose-orchestration)
6. [Image Tagging Strategy (Semantic Versioning)](#image-tagging-strategy-semantic-versioning)
7. [Port Mapping Deep Dive](#port-mapping-deep-dive)
8. [Volume Management & Data Persistence](#volume-management--data-persistence)
9. [Networking & Network Inspection](#networking--network-inspection)
10. [Logging & Observability](#logging--observability)
11. [Health Checks & Verification](#health-checks--verification)
12. [Maintenance & Cleanup Operations](#maintenance--cleanup-operations)

---

## 1. Architecture Overview

The SSPR Platform consists of three tier-isolated containerized components connected via a dedicated bridge network (`sspr-network`):

- **Database (`sspr-postgres`)**: PostgreSQL 15 Alpine storing users, reset requests, and tokens with persistent volume storage (`postgres_data`).
- **Backend API (`sspr-backend`)**: Spring Boot Java 17 application running on port `8080` with database connectivity, migration, and health probe endpoints.
- **Frontend Web UI (`sspr-frontend`)**: React + Vite application served via Nginx on port `80`, featuring an embedded reverse proxy to `/api/*` endpoints and SPA route fallbacks.

```
       Browser / Client
              │
       Port 80 (HTTP)
              ▼
   ┌─────────────────────┐
   │    sspr-frontend    │  (Nginx Alpine: Static Assets + API Reverse Proxy)
   └──────────┬──────────┘
              │ /api/*
              │ (Internal port 8080)
              ▼
   ┌─────────────────────┐
   │    sspr-backend     │  (Eclipse Temurin JRE 17: Spring Boot API)
   └──────────┬──────────┘
              │ JDBC (Port 5432)
              ▼
   ┌─────────────────────┐
   │    sspr-postgres    │  (PostgreSQL 15 Alpine + postgres_data volume)
   └─────────────────────┘
```

---

## 2. Building Images

### Building Individual Images

#### Build Backend Image
From the project root:
```bash
docker build -t sspr-backend:1.0.0 ./sspr-backend
```

Build with build arguments or target stage (if needed):
```bash
# Explicitly targeting runtime stage and specifying custom build metadata
docker build \
  --target runtime \
  -t sspr-backend:1.0.0 \
  -t sspr-backend:latest \
  ./sspr-backend
```

#### Build Frontend Image
From the project root:
```bash
docker build -t sspr-frontend:1.0.0 ./frontend
```

Multi-tag build for registry distribution:
```bash
docker build \
  -t sspr-frontend:1.0.0 \
  -t sspr-frontend:latest \
  ./frontend
```

### Build All Images with Docker Compose
To build or rebuild all services defined in `docker-compose.yml`:
```bash
# Build all images without starting containers
docker-compose build

# Rebuild all images from scratch (bypassing layer cache)
docker-compose build --no-cache
```

---

## 3. Running Containers

### Running Containers Individually

#### 1. Start PostgreSQL Network & Container
Create the shared bridge network first:
```bash
docker network create sspr-network
```

Run PostgreSQL with a persistent named volume:
```bash
docker run -d \
  --name sspr-postgres \
  --network sspr-network \
  -p 5432:5432 \
  -e POSTGRES_DB=sspr_db \
  -e POSTGRES_USER=sspr_user \
  -e POSTGRES_PASSWORD=SecretPassword123! \
  -v postgres_data:/var/lib/postgresql/data \
  --restart unless-stopped \
  postgres:15-alpine
```

#### 2. Run Backend Container
```bash
docker run -d \
  --name sspr-backend \
  --network sspr-network \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://sspr-postgres:5432/sspr_db \
  -e SPRING_DATASOURCE_USERNAME=sspr_user \
  -e SPRING_DATASOURCE_PASSWORD=SecretPassword123! \
  --restart unless-stopped \
  sspr-backend:1.0.0
```

#### 3. Run Frontend Container
```bash
docker run -d \
  --name sspr-frontend \
  --network sspr-network \
  -p 80:80 \
  --restart unless-stopped \
  sspr-frontend:1.0.0
```

### Running with Docker Compose (Recommended)
Launch the entire multi-container stack in detached mode:
```bash
docker-compose up -d
```

---

## 4. Container Lifecycle Commands

### `docker build`
Compiles an image from a `Dockerfile` and context:
```bash
# Basic build with tag
docker build -t sspr-backend:1.0.0 ./sspr-backend

# Build with progress output and specific file
docker build -f ./sspr-backend/Dockerfile -t sspr-backend:1.0.0 ./sspr-backend

# Build without cache
docker build --no-cache -t sspr-backend:1.0.0 ./sspr-backend
```

### `docker run`
Creates and starts a container from an image:
```bash
# Run detached (-d), named (--name), with ports (-p), env vars (-e), and volume (-v)
docker run -d \
  --name sspr-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -v /local/logs:/app/logs \
  sspr-backend:1.0.0
```

### `docker ps` & `docker ps -a`
Inspect container statuses:
```bash
# List only running containers
docker ps

# List all containers (running, exited, stopped)
docker ps -a

# Custom formatting to view container ID, name, status, and ports
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### `docker logs`
Stream or display container output:
```bash
# View last 100 lines of logs
docker logs --tail 100 sspr-backend

# Follow logs in real time with timestamps
docker logs -f -t sspr-backend

# View logs since a specific time
docker logs --since 10m sspr-frontend
```

### `docker inspect`
Inspect low-level configuration, network settings, and state:
```bash
# View full JSON configuration
docker inspect sspr-backend

# Extract container IP address within bridge network
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' sspr-backend

# Inspect health check status and log
docker inspect --format='{{json .State.Health}}' sspr-backend | jq .
```

### `docker stop`
Gracefully stops running containers by sending `SIGTERM`, then `SIGKILL` after grace period:
```bash
# Graceful stop with default 10s grace period
docker stop sspr-backend

# Stop with customized timeout (e.g., 30s)
docker stop -t 30 sspr-backend
```

### `docker start`
Starts one or more previously stopped containers:
```bash
docker start sspr-backend
```

### `docker restart`
Restarts a running or stopped container:
```bash
docker restart sspr-backend
```

### `docker rm`
Removes stopped containers:
```bash
# Remove a single stopped container
docker rm sspr-backend

# Force remove a currently running container (sends SIGKILL)
docker rm -f sspr-backend

# Remove all stopped containers
docker container prune -f
```

### `docker rmi`
Removes local images:
```bash
# Remove image by tag
docker rmi sspr-backend:1.0.0

# Force remove image even if referenced by stopped containers
docker rmi -f sspr-backend:1.0.0

# Remove dangling (untagged) images
docker image prune -f
```

### `docker exec`
Executes interactive commands inside an active container:
```bash
# Open interactive shell in backend container (Alpine /bin/sh)
docker exec -it sspr-backend /bin/sh

# Open interactive shell in frontend container
docker exec -it sspr-frontend /bin/sh

# Execute psql inside postgres container
docker exec -it sspr-postgres psql -U sspr_user -d sspr_db

# Run curl check directly inside backend container
docker exec sspr-backend curl -s http://localhost:8080/api/v1/admin/dashboard/summary
```

### `docker tag`
Creates a target alias referencing a source image:
```bash
# Tag for Docker Hub registry
docker tag sspr-backend:1.0.0 devsecops/sspr-backend:1.0.0
docker tag sspr-backend:1.0.0 devsecops/sspr-backend:latest

# Tag for private registry (e.g., AWS ECR or Harbor)
docker tag sspr-backend:1.0.0 registry.example.com/sspr/backend:1.0.0
```

### `docker push`
Uploads tagged images to a container registry:
```bash
# Authenticate to registry
docker login registry.example.com

# Push specific version
docker push registry.example.com/sspr/backend:1.0.0

# Push latest tag
docker push registry.example.com/sspr/backend:latest
```

---

## 5. Docker Compose Orchestration

Docker Compose coordinates multi-container application lifecycles using `docker-compose.yml`.

### Common Compose Commands

```bash
# Start all services in the background, rebuilding images if source changed
docker-compose up -d --build

# View aggregated logs from all services (follow mode)
docker-compose logs -f

# View logs for a specific service
docker-compose logs -f sspr-backend

# Check container status and exposed ports
docker-compose ps

# Execute a one-off command in a service container
docker-compose exec sspr-backend env

# Stop containers without removing networks and volumes
docker-compose stop

# Start previously stopped compose containers
docker-compose start

# Restart a specific service (e.g. backend)
docker-compose restart sspr-backend

# Gracefully bring down all services and networks
docker-compose down

# Bring down services, networks, AND purge named volumes (data deletion)
docker-compose down -v
```

---

## 6. Image Tagging Strategy (Semantic Versioning)

A robust tagging convention ensures reproducibility, rollback capability, and traceable audit trails across staging and production environments:

### Tag Structure: `MAJOR.MINOR.PATCH` (SemVer)
- **`MAJOR`**: Incompatible API changes, major framework upgrades (e.g., Java/Spring or React migration).
- **`MINOR`**: Backwards-compatible feature additions (e.g., new admin endpoints, reset flows).
- **`PATCH`**: Backwards-compatible bug fixes or security patches (e.g., dependency CVE remediation).

### Recommended Tagging Pattern:
1. **Commit SHA Tag**: Immutably ties image to Git commit:
   `sspr-backend:sha-a1b2c3d`
2. **SemVer Tag**: Represents release milestone:
   `sspr-backend:1.0.0`
3. **Environment Tag**: Dynamic tag pointing to current release:
   `sspr-backend:staging`, `sspr-backend:production`
4. **`latest` Tag**: Always tracks the newest release on the main branch:
   `sspr-backend:latest`

#### Tagging Example in CI/CD:
```bash
export VERSION="1.0.0"
export COMMIT_SHA=$(git rev-parse --short HEAD)

docker build -t sspr-backend:${VERSION} -t sspr-backend:${COMMIT_SHA} -t sspr-backend:latest ./sspr-backend
```

---

## 7. Port Mapping Deep Dive

When publishing ports using `-p HOST_PORT:CONTAINER_PORT`:

```
┌───────────────────────────────────────────────┐
│ Host Machine                                  │
│                                               │
│   Port 80 ────────────► sspr-frontend:80      │
│   Port 8080 ──────────► sspr-backend:8080     │
│   Port 5432 ──────────► postgres-db:5432      │
└───────────────────────────────────────────────┘
```

- **`80:80` (Frontend)**:
  - Binds host port `80` to container port `80`. Users access web UI at `http://localhost`.
  - Nginx handles static assets directly and proxies `/api/*` internally to `sspr-backend:8080`.
- **`8080:8080` (Backend API)**:
  - Binds host port `8080` to backend container port `8080`.
  - Enables direct API testing, Postman access, and Swagger/OpenAPI exploration at `http://localhost:8080/api/v1/*`.
- **`5432:5432` (PostgreSQL)**:
  - Binds host port `5432` to PostgreSQL default container port `5432`.
  - Allows local database management tools (DBeaver, pgAdmin, IntelliJ Database Tool) to connect directly using `localhost:5432`.

> **Security Note for Production**:
> In production environments, do NOT expose port `5432` or `8080` to external host interfaces. Only port `80`/`443` on the frontend/ingress should be public, while database and backend communicate exclusively inside the private Docker network.

---

## 8. Volume Management & Data Persistence

PostgreSQL stores relational data inside `/var/lib/postgresql/data`. Containers are ephemeral; destroying a container will erase untracked storage unless backed by a Docker Volume.

### Inspecting and Managing Named Volumes
```bash
# List all Docker volumes
docker volume ls

# Inspect postgres_data metadata and mount point on the host filesystem
docker volume inspect password-reset-platform_postgres_data

# Create an ad-hoc volume
docker volume create sspr_backup_data

# Backup volume data to a compressed archive
docker run --rm \
  -v password-reset-platform_postgres_data:/volume \
  -v $(pwd):/backup \
  alpine tar -czvf /backup/postgres_backup_$(date +%Y%m%d).tar.gz -C /volume .

# Restore volume data from archive
docker run --rm \
  -v password-reset-platform_postgres_data:/volume \
  -v $(pwd):/backup \
  alpine sh -c "rm -rf /volume/* && tar -xzvf /backup/postgres_backup_20261014.tar.gz -C /volume"
```

---

## 9. Networking & Network Inspection

Docker Compose provisions an isolated user-defined bridge network named `sspr-network`. Containers on this network resolve each other via internal DNS using service names (`postgres-db`, `sspr-backend`, `sspr-frontend`).

### Network Diagnostic Commands
```bash
# List all active Docker networks
docker network ls

# Inspect network to view attached containers and assigned IP addresses
docker network inspect password-reset-platform_sspr-network

# Test connectivity from frontend container to backend service
docker exec sspr-frontend ping -c 3 sspr-backend

# Test TCP port resolution from backend to database
docker exec sspr-backend nc -zv postgres-db 5432
```

---

## 10. Logging & Observability

Standardizing on stdout/stderr container logs allows centralized aggregation (Fluentd, Promtail/Loki, Datadog, AWS CloudWatch).

### Best Practices for Log Inspection
```bash
# Follow live backend logs with timestamp
docker logs -f -t sspr-backend

# Filter backend logs for ERROR level messages
docker logs sspr-backend 2>&1 | grep -i "error"

# Inspect frontend Nginx access and error logs
docker logs --tail 50 sspr-frontend
```

---

## 11. Health Checks & Verification

Each service includes custom health checks to prevent routing traffic to unready or crashed containers.

### Health Check Implementations

1. **PostgreSQL**:
   ```yaml
   test: ["CMD-SHELL", "pg_isready -U sspr_user -d sspr_db"]
   interval: 10s
   timeout: 5s
   retries: 5
   ```
2. **Backend**:
   ```dockerfile
   HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
     CMD curl -f http://localhost:8080/api/v1/admin/dashboard/summary || exit 1
   ```
3. **Frontend**:
   ```dockerfile
   HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
     CMD curl -f http://localhost:80/ || exit 1
   ```

### Verifying Health Statuses
```bash
# View health status of all containers
docker ps --format "table {{.Names}}\t{{.Status}}"

# Query detailed health check log for backend container
docker inspect --format='{{range .State.Health.Log}}{{.Output}}{{end}}' sspr-backend
```

---

## 12. Maintenance & Cleanup Operations

Over time, unused images, intermediate build stages, stopped containers, and build cache accumulate on disk.

```bash
# Safe cleanup: remove stopped containers, dangling images, and unused networks
docker system prune -f

# Comprehensive cleanup: remove all unused containers, networks, and images (not just dangling)
docker system prune -a --volumes -f

# Check Docker disk usage breakdown
docker system df

# Check detailed disk usage per container, image, and volume
docker system df -v
```
