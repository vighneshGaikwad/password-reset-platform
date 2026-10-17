# Password Reset Platform

A DevSecOps-oriented self-service password reset platform with a React frontend and a Spring Boot backend. Users can submit password reset requests, while administrators can create user records, review requests, search by email, and update request status.

## Architecture

- **Frontend:** React 19 with Vite, served by the Vite development server on port `5173`
- **Backend:** Spring Boot 4 with Java 17, served on port `8080`
- **Database:** PostgreSQL 15
- **CI/CD:** Jenkins pipeline that builds the backend and deploys the packaged JAR to embedded Tomcat

## Prerequisites

- Java 17 or newer
- Node.js and npm
- Docker Desktop (for PostgreSQL)
- Maven, or the included Maven wrapper

## Run Locally

1. Start PostgreSQL:

	```bash
	docker compose up -d postgres-db
	```

	The Compose file creates database `sspr_db` on port `5432` with the local development user `sspr_user`. Its password is intended only for local development; replace it with a secret-managed value before using this setup outside a local environment.

2. Start the backend:

	```bash
	cd sspr-backend
	./mvnw spring-boot:run
	```

3. In a second terminal, start the frontend:

	```bash
	cd frontend
	npm install
	npm run dev
	```

4. Open `http://localhost:5173` in a browser.

The backend expects PostgreSQL at `localhost:5432` and creates or updates its JPA tables automatically using the settings in `sspr-backend/src/main/resources/application.properties`.

## Available API Routes

### User

```text
POST /api/v1/auth/request-reset?email={email}
```

### Administration

```text
GET  /api/v1/admin/dashboard/summary
GET  /api/v1/admin/requests?search={email}
POST /api/v1/admin/users
PUT  /api/v1/admin/requests/{id}/status?status={status}
```

Supported workflow statuses include `APPROVED`, `COMPLETED`, and `REJECTED`.

## Validation and Builds

Run the backend tests and package the application:

```bash
cd sspr-backend
./mvnw clean verify
```

Run frontend linting and create a production build:

```bash
cd frontend
npm run lint
npm run build
```

Run the Selenium and REST-Assured integration suite against running services:

```bash
cd selenium-tests
mvn test \
	-Dselenium.baseUrl=http://localhost:5173 \
	-Dapi.base.url=http://localhost:8080
```

The Selenium suite uses WebDriverManager to obtain a compatible browser driver. Make sure a supported browser is installed before running it.

## Project Layout

```text
frontend/           React and Vite user interface
sspr-backend/       Spring Boot REST API and PostgreSQL integration
selenium-tests/     Selenium UI and REST API integration tests
ansible/            Provisioning, deployment, health-check, and rollback playbooks
docs/               Deployment, testing, defect workflow, and presentation guides
Jenkinsfile         CI/CD pipeline definition
docker-compose.yml  Local multi-container deployment
```

## Jenkins Pipeline, CI/CD and Operations

The `Jenkinsfile` builds and validates the backend and frontend, packages the application, and supports containerized deployment. Configure Maven in Jenkins with the tool name `maven`, then provide these pipeline parameters:

- `ENVIRONMENT`: `dev`, `staging`, or `prod`
- `APP_PORT`: the port used by the deployed application, defaulting to `8080`

The pipeline archives the generated JAR from `sspr-backend/target/` after each run.

The complete local stack can be started with:

```bash
docker compose up -d --build
docker compose ps
```

Stop the stack with `docker compose down`. For server provisioning and deployment, use the Ansible playbooks from `ansible/`:

```bash
cd ansible
ansible-playbook -i inventory.ini playbook.yml
ansible-playbook -i inventory.ini provision-and-deploy.yml
ansible-playbook -i inventory.ini health-check.yml
ansible-playbook -i inventory.ini rollback-playbook.yml
```

See the guides in `docs/` for deployment lifecycle details, defect injection and validation, Selenium coverage, and the project presentation walkthrough.

## CI/CD, Containerization & Configuration Management (Tasks 10 - 14)

The platform is integrated with an end-to-end DevSecOps delivery pipeline:

- **[docs/project-presentation-and-defense-guide.md](docs/project-presentation-and-defense-guide.md)**: **Master Student Guide & Teacher Defense Walkthrough** with 5-step live demonstration and top 10 viva defense questions.
- **[docs/defect-workflow.md](docs/defect-workflow.md)**: Step-by-step guide for **Task 10 (Deliberate Defect Workflow)** demonstrating how Selenium + REST Assured tests catch regressions and halt Jenkins deployments.
- **[docs/docker-lifecycle.md](docs/docker-lifecycle.md)**: Operations and command reference for **Task 11 & Task 12 (Docker Container Lifecycle & Continuous Deployment)**.
- **[docs/ansible-guide.md](docs/ansible-guide.md)**: Configuration specification, idempotency validation, health check, and rollback procedures for **Task 13 & Task 14 (Ansible Configuration Management & Reliability Validation)**.
- **[docs/selenium-test-plan.md](docs/selenium-test-plan.md)**: Test plan and critical browser journeys for automated Selenium verification.

### Docker Multi-Stage Builds & Compose
```bash
# Build production images
docker build -t sspr-backend:1.0.0 ./sspr-backend
docker build -t sspr-frontend:1.0.0 ./frontend

# Start full 3-tier container stack
docker-compose up -d
```

### Ansible Playbook Execution
```bash
cd ansible
# Provision server
ansible-playbook -i inventory.ini playbook.yml

# Health check
ansible-playbook -i inventory.ini health-check.yml

# Automated rollback
ansible-playbook -i inventory.ini rollback-playbook.yml
```