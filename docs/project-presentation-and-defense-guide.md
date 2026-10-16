# Password Reset Platform (SSPR)
## Complete Project Presentation, Demonstration & Viva Defense Guide

> **DevOps / DevSecOps Course Project Defense**  
> **Student Guide & Comprehensive Teacher Walkthrough**

---

## 1. Project Overview & Executive Summary

The **Self-Service Password Reset (SSPR) Platform** is an enterprise-grade, secure, three-tier web application built following modern **DevSecOps** practices. It allows employees to securely request password resets while preventing **User Account Enumeration** attacks, and gives system administrators an audited dashboard to review, approve, complete, or reject reset requests.

### Core Stack & Architecture
- **Frontend**: React 19 + Vite + Modern CSS Design System (Inter typography, Slate & Dark Navy theme, real-time toast alerts, status badges, responsive dashboard).
- **Backend**: Java 17 + Spring Boot 4.x + Spring Data JPA + Hibernate + REST Controllers.
- **Database**: PostgreSQL 15 Alpine with persistent volume storage.
- **Continuous Integration / Testing**: Jenkins Declarative Pipeline + JUnit 5 + Selenium WebDriver (Headless Chrome) + REST Assured.
- **Containerization & Orchestration**: Multi-stage Dockerfiles (Eclipse Temurin JRE, Nginx Alpine), Docker Compose, and Docker Network.
- **Configuration Management**: Ansible 2.x Playbooks, Jinja2 templates, inventory variables, systemd services, automated health probes, and rollback scripts.

```
                         [ End-User / Administrator ]
                                      │
                                      ▼
                        ┌───────────────────────────┐
                        │   Frontend (React/Nginx)  │  Port 80 (Prod) / 5173 (Dev)
                        │     Modern SaaS UI/UX     │
                        └─────────────┬─────────────┘
                                      │ REST API (/api/v1/*)
                                      ▼
                        ┌───────────────────────────┐
                        │  Backend (Spring Boot)    │  Port 8080
                        │  Auth & Admin Controllers │
                        └─────────────┬─────────────┘
                                      │ JDBC / JPA
                                      ▼
                        ┌───────────────────────────┐
                        │  Database (PostgreSQL 15) │  Port 5432
                        │    sspr_db (Persistent)   │
                        └───────────────────────────┘
```

---

## 2. Detailed Task Breakdown & Implementation Details

### Task 10: Continuous Testing in Jenkins
- **Objective**: Integrate a Selenium browser test suite and REST Assured API test suite into the Jenkins pipeline, publish XML test reports, enforce test quality gates (failed tests halt deployment), introduce a deliberate defect, show the red pipeline, fix it, and show the green rerun.
- **Implementation**:
  - Standalone Maven test suite in `selenium-tests/` with `BaseTest.java`, `PasswordResetUITest.java`, `AdminDashboardUITest.java`, and `APIIntegrationTest.java`.
  - Configured with headless Chrome WebDriver and `WebDriverManager` for zero-configuration browser binary resolution.
  - Test reports published in Jenkins via `junit 'selenium-tests/target/surefire-reports/*.xml'` in the `post { always { ... } }` block.
  - **Deliberate Defect Workflow**:
    1. In `AdminController.java`, change `@GetMapping("/dashboard/summary")` to `@GetMapping("/dashboard/summry")`.
    2. Commit and push.
    3. Jenkins Stage *"Selenium & Integration Tests"* fails (HTTP 404), turning the build **RED** and immediately aborting before container build or deployment.
    4. Revert typo back to `/dashboard/summary`, commit and push.
    5. Jenkins pipeline runs, all tests pass, build turns **GREEN**, and deployment proceeds.
  - **Deliverables**:
    - [Jenkinsfile](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/Jenkinsfile)
    - [docs/defect-workflow.md](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/docs/defect-workflow.md)
    - [selenium-tests/pom.xml](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/selenium-tests/pom.xml)

---

### Task 11: Docker Image and Container Lifecycle
- **Objective**: Create Dockerfiles for the platform, build and tag images, run containers, map ports, inspect logs, demonstrate start/stop/restart/remove lifecycle, and document operations.
- **Implementation**:
  - **Backend Dockerfile** (`sspr-backend/Dockerfile`): Multi-stage build with `eclipse-temurin:17-jdk-jammy` builder stage and `eclipse-temurin:17-jre-jammy` runtime stage. Hardened with dedicated non-root user (`appuser:appgroup`), JVM container support flags (`-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`), and built-in healthcheck probe.
  - **Frontend Dockerfile** (`frontend/Dockerfile`): Multi-stage build with `node:20-alpine` (building Vite production bundle) and `nginx:alpine` (serving static assets and reverse-proxying `/api/*`).
  - **Container Lifecycle Demonstration**:
    ```bash
    # 1. Build and tag
    docker build -t sspr-backend:1.0.0 ./sspr-backend
    docker tag sspr-backend:1.0.0 sspr-backend:latest
    
    # 2. Run with port mapping and network
    docker run -d --name sspr-backend-app -p 8080:8080 --network sspr-network sspr-backend:1.0.0
    
    # 3. View status and inspect
    docker ps
    docker inspect sspr-backend-app
    
    # 4. View logs
    docker logs -f sspr-backend-app
    
    # 5. Stop, start, restart, and remove
    docker stop sspr-backend-app
    docker start sspr-backend-app
    docker restart sspr-backend-app
    docker rm -f sspr-backend-app
    ```
  - **Deliverables**:
    - [sspr-backend/Dockerfile](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/sspr-backend/Dockerfile)
    - [frontend/Dockerfile](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/frontend/Dockerfile)
    - [frontend/nginx.conf](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/frontend/nginx.conf)
    - [docker-compose.yml](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/docker-compose.yml)
    - [docs/docker-lifecycle.md](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/docs/docker-lifecycle.md)

---

### Task 12: Jenkins-Docker Continuous Deployment
- **Objective**: Extend Jenkins pipeline to automatically build versioned Docker images (`sspr-backend:${BUILD_NUMBER}`), publish them to a container registry, and deploy a fresh container automatically after all tests succeed.
- **Implementation**:
  - Added Stage **"Build Docker Image"**: Builds image tagged with `${BUILD_NUMBER}` and `latest`.
  - Added Stage **"Push to Registry"**: Pushes versioned tags to Docker Hub / registry.
  - Added Stage **"Deploy Container"**: Zero-downtime container replacement using environment parameters (`dev`, `staging`, `prod`) and isolated network.
  - Added Stage **"Health Check"**: Probes `http://localhost:${APP_PORT}/api/v1/admin/dashboard/summary` via curl with retries.
  - Added Automated Rollback in `post { failure { ... } }`: Automatically starts `sspr-backend-app-prev` if deployment or health check fails.
  - **Deliverable**: [Jenkinsfile](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/Jenkinsfile)

---

### Task 13: Configuration Management Script (Ansible)
- **Objective**: Identify all server prerequisites, users, folders, packages, ports, and services, and create an Ansible inventory and playbook.
- **Implementation**:
  - **Prerequisites Documented**:
    - Packages: `openjdk-17-jdk`, `nodejs 20.x`, `postgresql-15`, `docker-ce`, `git`, `curl`, `ufw`.
    - User/Group: Dedicated system account `sspr:sspr` with `/bin/bash` shell.
    - Directories: `/opt/sspr-platform` (`backend/`, `frontend/`, `config/`, `backups/`, `scripts/`) and `/var/log/sspr-platform`.
    - Ports: `80` (HTTP), `443` (HTTPS), `8080` (Backend API), `5432` (PostgreSQL).
  - **Ansible Structure**:
    - `inventory.ini`: Localhost/target node definition with variables.
    - `group_vars/all.yml`: Centralized platform parameters.
    - `templates/application.properties.j2`: Dynamic datasource configuration.
    - `templates/sspr-backend.service.j2`: Systemd unit file with automatic restart policies.
    - `playbook.yml`: 8-phase execution playbook with tags.
  - **Deliverables**:
    - [ansible/inventory.ini](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/ansible/inventory.ini)
    - [ansible/group_vars/all.yml](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/ansible/group_vars/all.yml)
    - [ansible/playbook.yml](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/ansible/playbook.yml)
    - [docs/ansible-guide.md](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/docs/ansible-guide.md)

---

### Task 14: Automated Provisioning, Idempotency & Reliability Validation
- **Objective**: Provision a clean target environment, deploy application/containers, rerun automation to demonstrate idempotency, execute health check, and test rollback/recovery to previous stable release.
- **Implementation**:
  - **Provisioning & Deployment**: `ansible-playbook -i inventory.ini provision-and-deploy.yml` orchestrates full system setup and launches the Docker Compose application stack.
  - **Idempotency Proof**: Executing `ansible-playbook -i inventory.ini playbook.yml` a second time yields `changed=0`, proving safe re-execution without unexpected state changes.
  - **Automated Health Check**: `ansible-playbook -i inventory.ini health-check.yml` validates:
    - PostgreSQL service active
    - Spring Boot API returns HTTP 200 on `/api/v1/admin/dashboard/summary`
    - Docker container health
    - Listening network ports (`ss -tlnp`)
    - Disk and memory consumption
  - **Rollback & Recovery**: `ansible-playbook -i inventory.ini rollback-playbook.yml` handles:
    - Timestamped backup of existing deployment
    - Inspection of current container tag
    - Identification of previous stable Docker image
    - Safe stop and teardown of faulty container
    - Launch of previous stable container image
    - Post-rollback health validation
  - **Deliverables**:
    - [ansible/provision-and-deploy.yml](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/ansible/provision-and-deploy.yml)
    - [ansible/health-check.yml](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/ansible/health-check.yml)
    - [ansible/rollback-playbook.yml](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/ansible/rollback-playbook.yml)

---

## 3. UI/UX Transformation Highlights

The frontend was completely overhauled from an unstyled prototype into an enterprise SaaS application:
1. **Design System & Typography**: Integrated Google Fonts 'Inter', defined semantic tokens for primary (`#4f46e5`), slate background (`#f1f5f9`), card surfaces (`#ffffff`), and status badge colors (amber for PENDING, blue for APPROVED, green for COMPLETED, red for REJECTED).
2. **Sidebar Navigation**: Fixed left-side navigation in Dark Navy (`#0f172a`), brand icon (`🛡️ SecureReset`), section categories (`MAIN`, `ADMIN`), active navigation highlighting, user profile status indicator, and environment version tag (`v1.0.0 • DevSecOps`).
3. **Executive Dashboard**: 4 key metric cards (Total Users, Pending Requests, Completed, Rejected) with color-coded accent borders, real-time counts, and recent activity preview.
4. **Self-Service Password Reset Portal**: Centered card with animated security badge, email input with HTML5 validation, anti-enumeration confirmation alert, and loading states.
5. **Interactive Request Management**: Full-width data table featuring search by email, truncated tokens for security, dynamic status badges, and action buttons (Approve, Complete, Reject) that dynamically reflect valid state transitions.
6. **Toast Notification System**: Animated slide-in toasts in the top-right corner with auto-dismissal for all CRUD operations and error handling.

---

## 4. How to Present and Demonstrate to Your DevOps Teacher

Follow this structured 5-step walkthrough during your evaluation:

### Step 1: Explain the Architecture (2 Minutes)
> *"Good morning/afternoon, Professor. For this project, I have built and automated an enterprise-grade Self-Service Password Reset Platform. The architecture consists of a React 19 single-page application served via Nginx reverse proxy, a Java 17 Spring Boot REST API, and a PostgreSQL database. From a DevSecOps standpoint, our goal was to implement complete end-to-end continuous integration, continuous testing, container lifecycle management, containerized continuous deployment, and infrastructure-as-code configuration management with automated recovery."*

### Step 2: Live Demo of the Application & UI/UX (2 Minutes)
1. Open your browser to `http://localhost:5173` (or port 80).
2. Show the **User Reset Portal**:
   - Enter `devsecops@test.com` and click **Submit Request**.
   - Point out the security message: *"If the email exists in our system, a password reset link has been sent."* Explain: *"Notice that even if an unknown email is submitted, the system returns the exact same message. This implements Anti-Enumeration security to prevent malicious actors from guessing valid enterprise email addresses."*
3. Switch to the **Admin Dashboard**:
   - Show the 4 metric cards update immediately.
   - Go to **Manage Requests**, show the newly created pending request.
   - Click **Approve**, then click **Complete**. Point out how the status badges and dashboard counts update in real time.
4. Go to **Create User**:
   - Create a test user, show the success toast notification.

### Step 3: Demonstrate Task 10 (Continuous Testing & Deliberate Defect) (3 Minutes)
1. Open [`Jenkinsfile`](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/Jenkinsfile) and show Stage 5 *"Selenium & Integration Tests"*.
2. Show [`selenium-tests/src/test/java/com/devsecops/selenium/PasswordResetUITest.java`](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/selenium-tests/src/test/java/com/devsecops/selenium/PasswordResetUITest.java) and `APIIntegrationTest.java`.
3. Explain the Deliberate Defect:
   > *"Professor, to demonstrate continuous testing quality gates as required by Task 10, we introduced a deliberate typo into `AdminController.java`, changing `/dashboard/summary` to `/dashboard/summry`. When we ran the pipeline, the Selenium and REST Assured tests caught this regression, failed the stage with an HTTP 404, published the test failure XML to Jenkins, and aborted the pipeline immediately before any Docker image was built or deployed. After correcting the typo back to `/dashboard/summary`, the pipeline rerun was completely green."*
4. Refer to [`docs/defect-workflow.md`](file:///Users/vighneshmac/Documents/DevOps-Project/password-reset-platform/docs/defect-workflow.md) for full screenshots and log evidence.

### Step 4: Demonstrate Task 11 & 12 (Docker & Continuous Deployment) (2 Minutes)
1. Show the multi-stage Dockerfiles (`sspr-backend/Dockerfile` and `frontend/Dockerfile`):
   - Explain multi-stage benefits: *"Notice we separate the build stage from the runtime stage. The builder stage has the full JDK and Maven, but the runtime image only contains the lightweight JRE on an Ubuntu Jammy base. Furthermore, the container runs under a non-root `appuser` for container hardening."*
2. Run in terminal:
   ```bash
   docker images | grep sspr
   docker ps
   ```
3. Show how the Jenkins pipeline automatically builds `sspr-backend:${BUILD_NUMBER}`, tags it, pushes it, stops the old container, launches the new one, and runs an automated curl health check.

### Step 5: Demonstrate Task 13 & 14 (Ansible, Idempotency & Rollback) (3 Minutes)
1. Show `ansible/playbook.yml`, `inventory.ini`, and `group_vars/all.yml`.
2. Explain:
   > *"For configuration management, we wrote Ansible playbooks that manage the entire target node. It creates the dedicated `sspr` user, directories, installs Java 17, Node.js 20, PostgreSQL, Docker, and configures firewall rules."*
3. Demonstrate **Idempotency**:
   - Explain: *"A cornerstone of DevOps configuration management is idempotency: running the playbook multiple times produces the exact same system state without redundant modifications. The second run reports `changed=0`."*
4. Demonstrate **Reliability & Rollback**:
   - Show `ansible/health-check.yml` executing health assertions against ports, services, and endpoints.
   - Show `ansible/rollback-playbook.yml`: *"If a broken build ever passes into production, our rollback playbook queries Docker image history, gracefully stops the failing container, and re-launches the previous stable container image with automatic verification."*

---

## 5. Teacher Viva & Defense Q&A Preparation

Here are the 10 most common questions your professor might ask, along with exact answers:

#### Q1: Why did you use Multi-Stage Docker builds instead of a single Dockerfile?
> **Answer**: Multi-stage builds dramatically reduce the final image size and attack surface. In the first stage (builder), we use the full JDK and Maven to compile the source code. In the final stage (runtime), we only copy over the compiled JAR file into a minimal JRE base image. Build tools, source code, and intermediate caches are discarded, resulting in faster download times, reduced vulnerability exposure, and production security compliance.

#### Q2: What is the significance of the anti-enumeration feature in the password reset endpoint?
> **Answer**: Account enumeration occurs when an authentication endpoint leaks whether a specific email or username exists in the system by returning different responses (e.g., *"User found, email sent"* vs *"Email not found"*). Attackers abuse this to harvest valid employee emails. Our endpoint always returns the identical generic message: *"If the email exists in our system, a password reset link has been sent"*, regardless of whether the email is present in the database.

#### Q3: How do the Selenium and REST Assured tests prevent broken code from being deployed?
> **Answer**: In our Jenkins declarative pipeline, the Selenium and integration test stage runs before the Docker build and deployment stages. Maven Failsafe and Surefire return a non-zero exit code if any test assertion fails. Jenkins interprets this exit code as a build failure, halts execution of subsequent stages, marks the build status as RED, and archives the JUnit test XML report. Deployment cannot occur unless 100% of the automated tests pass.

#### Q4: Why did you configure the Docker container to run as a non-root user?
> **Answer**: By default, Docker containers run as `root` (UID 0), which poses a significant security risk. If an application contains a remote code execution vulnerability and runs as root, an attacker who escapes the container could potentially gain root privileges on the host kernel. By creating a dedicated `appuser` and specifying `USER appuser` in the Dockerfile, we enforce the principle of least privilege.

#### Q5: What is Idempotency in Ansible and why is it essential?
> **Answer**: Idempotency means that an operation can be applied multiple times without changing the result beyond the initial application. In Ansible, modules check the current state of the target before making changes. If a user, package, or directory already matches the desired configuration, Ansible reports `ok` and leaves it untouched (`changed=0`). This ensures that applying playbooks repeatedly does not cause unintended configuration drift or service disruptions.

#### Q6: How does the Nginx container communicate with the Spring Boot backend?
> **Answer**: Both containers run on a shared Docker bridge network (`sspr-network`). Docker's built-in DNS resolves container names to their internal IP addresses. In our `nginx.conf`, requests starting with `/api/` are reverse-proxied to `http://sspr-backend:8080`, allowing the browser to interact with a single port (80) while shielding the backend service.

#### Q7: How does automated rollback work in your pipeline and playbooks?
> **Answer**: We implement rollback at two levels: in the Jenkins pipeline's `post { failure { ... } }` block, and through Ansible's `rollback-playbook.yml`. The rollback mechanism inspects the currently running Docker container tag, queries previous image revisions using `docker images`, stops the faulty container, deploys the previous stable container tag, and verifies health via an HTTP probe.

#### Q8: Why did you use `nohup` and `JENKINS_NODE_COOKIE` in Stage 4 of Jenkins?
> **Answer**: By default, Jenkins uses a ProcessTreeKiller that automatically terminates any child processes spawned during build steps when a stage ends. Setting `JENKINS_NODE_COOKIE = 'dontKillMe'` signals Jenkins to allow the background Spring Boot test instance to continue running detached while the Selenium tests run in the next stage. We then explicitly kill the PID in the `post { always { ... } }` block.

#### Q9: What happens if the PostgreSQL database container goes down?
> **Answer**: The backend Docker container includes a health check and depends on the database health check via `condition: service_healthy` in Docker Compose. In production, Docker's restart policy (`restart: unless-stopped`) automatically attempts container recovery, and the data remains intact because PostgreSQL data is persisted on a named volume (`postgres_data`) decoupled from container lifecycles.

#### Q10: How does your UI handle role-based state transitions?
> **Answer**: In our React application, the administrator interface validates valid request states. A reset token starts as `PENDING`. Administrators can transition it to `APPROVED` or `REJECTED`. Once approved and the user resets their password, it transitions to `COMPLETED`. Action buttons dynamically disable or hide to prevent invalid transitions, such as re-approving an already completed or rejected token.
