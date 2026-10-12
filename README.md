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

## Jenkins Pipeline

The `Jenkinsfile` builds the backend and starts the packaged JAR on the selected embedded Tomcat port. Configure Maven in Jenkins with the tool name `maven`, then provide these pipeline parameters:

- `ENVIRONMENT`: `dev`, `staging`, or `prod`
- `APP_PORT`: the port used by the deployed application, defaulting to `8080`

The pipeline archives the generated JAR from `sspr-backend/target/` after each run.