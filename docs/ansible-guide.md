# Ansible Configuration Management Guide

## 1. Overview & Architecture

The Password Reset Platform (SSPR Platform) uses Ansible for automated configuration management, system provisioning, application deployment, health monitoring, and rollback operations.

### Target Architecture

```
                       +---------------------------------------+
                       |             Client / User             |
                       +-------------------+-------------------+
                                           |
                                           v
                       +---------------------------------------+
                       |         Nginx Reverse Proxy           |
                       |       (Frontend: Port 80/443)         |
                       +-------------------+-------------------+
                                           |
                                           v
                       +---------------------------------------+
                       |       Spring Boot Backend App         |
                       |             (Port 8080)               |
                       +-------------------+-------------------+
                                           |
                                           v
                       +---------------------------------------+
                       |        PostgreSQL 15 Database         |
                       |             (Port 5432)               |
                       +---------------------------------------+
```

---

## 2. Server Configuration Specification & Prerequisites

The playbooks manage and configure the target host with the following prerequisites:

| Component | Target Version | Purpose |
|---|---|---|
| **Operating System** | Ubuntu 22.04 LTS / Debian 12 (Darwin supported for dev) | Host operating system |
| **Java Runtime** | OpenJDK 17 | Backend runtime environment |
| **Build Tools** | Maven 3.9+, Git | Build automation and SCM |
| **Frontend Runtime** | Node.js 20.x + npm | Frontend application build & dependencies |
| **Web Server / Proxy** | Nginx | Production reverse proxy and static asset server |
| **Database** | PostgreSQL 15 | Relational data persistence |
| **Container Engine** | Docker Engine & Docker Compose Plugin | Containerized deployment runtime |
| **Application User** | `sspr` (group: `sspr`) | Non-privileged system user for running services |
| **Application Base Dir** | `/opt/sspr-platform` | Standardized application root filesystem |

### Directory Structure on Target Host
- `/opt/sspr-platform`: Application root
- `/opt/sspr-platform/backend`: Compiled JAR artifacts and runtime files
- `/opt/sspr-platform/frontend`: Built React static assets
- `/opt/sspr-platform/config`: Production configuration (`application.properties`)
- `/opt/sspr-platform/backups`: Versioned rollback backups
- `/opt/sspr-platform/scripts`: Maintenance and operations scripts
- `/var/log/sspr-platform`: Application stdout and stderr logs

---

## 3. Inventory & Configuration Files

The Ansible project structure is organized as follows:

```
ansible/
├── ansible.cfg                    # Ansible default runtime configuration
├── inventory.ini                  # Target hosts and inventory-level variables
├── group_vars/
│   └── all.yml                    # Global variables and platform configurations
├── templates/
│   ├── application.properties.j2  # Spring Boot configuration template
│   └── sspr-backend.service.j2    # Systemd unit file template
├── playbook.yml                   # Main system configuration and provisioning playbook
├── provision-and-deploy.yml       # End-to-end provisioning and Docker Compose deployment
├── health-check.yml               # Automated post-deployment health verification
└── rollback-playbook.yml          # Automated rollback to previous release
```

### Key Variables in `group_vars/all.yml`
- `app_name`: `sspr-platform`
- `backend_port`: `8080`
- `frontend_port`: `80`
- `db_host`: `localhost`
- `db_port`: `5432`
- `db_name`: `sspr_db`
- `db_user`: `sspr_user`
- `db_password`: `"SecretPassword123!"`
- `java_version`: `"17"`
- `node_version`: `"20"`

---

## 4. Execution Commands

All commands should be executed from within the `ansible/` directory.

### 4.1 Full Server Provisioning & Setup
Execute the complete multi-phase configuration playbook to configure system packages, users, Java 17, Node.js 20, PostgreSQL, Docker, firewall rules, and backend service:

```bash
# Full provisioning
ansible-playbook -i inventory.ini playbook.yml
```

### 4.2 End-to-End Provisioning & Docker Deployment
Execute the complete infrastructure setup followed by full-stack deployment via Docker Compose:

```bash
# Full provisioning and Docker deployment
ansible-playbook -i inventory.ini provision-and-deploy.yml
```

---

## 5. Selective Execution Using Tags

The main playbook is broken down into modular phases, each annotated with tags to allow executing specific subsets of tasks without running the full suite.

| Tag | Phase / Action | Description |
|---|---|---|
| `system`, `packages` | Phase 1: System Prerequisites | Update apt cache and install base OS tools (`curl`, `git`, `htop`, etc.) |
| `users` | Phase 2: User Creation | Create `sspr` system user and group |
| `directories` | Phase 2: Directory Setup | Create `/opt/sspr-platform` hierarchy with correct ownership |
| `java` | Phase 3: Java 17 | Install OpenJDK 17 and configure `JAVA_HOME` in `/etc/environment` |
| `nodejs` | Phase 4: Node.js 20 | Add NodeSource repository and install Node.js 20.x with npm |
| `database` | Phase 5: PostgreSQL 15 | Install PostgreSQL, ensure service is running, create database & user |
| `docker` | Phase 6: Docker | Add Docker official GPG key & repo, install engine, add `sspr` to docker group |
| `firewall` | Phase 7: Security / UFW | Allow ports `80`, `443`, `8080`, and `5432` |
| `deploy` | Phase 8: Application Deployment | Copy JAR, render config and systemd service, start backend service |
| `config` | Phase 8: Configuration | Re-render `application.properties` from template |
| `service` | Phase 8: Systemd Service | Manage `sspr-backend` systemd unit file and daemon |

### Selective Execution Examples

```bash
# Run only specific tags (e.g., Java and Docker installation)
ansible-playbook -i inventory.ini playbook.yml --tags "java,docker"

# Re-deploy only the backend application and configuration
ansible-playbook -i inventory.ini playbook.yml --tags "deploy,config"

# Only configure database and firewall
ansible-playbook -i inventory.ini playbook.yml --tags "database,firewall"

# Skip long-running system package updates
ansible-playbook -i inventory.ini playbook.yml --skip-tags "packages"
```

---

## 6. Understanding & Demonstrating Idempotency

### What is Idempotency?
Idempotency is the property of an operation whereby it can be applied multiple times without changing the result beyond the initial application. In Ansible:
- Running a playbook against a clean host will make changes (`changed=N`).
- Running the exact same playbook immediately afterward against the same host will make zero changes (`changed=0`, all states report `ok`).
- The system remains in the desired state without unnecessary restarts or duplicate modifications.

### Demonstrating Idempotency in Practice

1. **Step 1: Check Mode (Dry Run)**  
   Simulate execution without modifying the host to inspect what tasks would change:
   ```bash
   # Dry run (check mode) - demonstrates idempotency
   ansible-playbook -i inventory.ini playbook.yml --check
   ```

2. **Step 2: Initial Run**  
   Run the playbook to bring the host into the desired state:
   ```bash
   ansible-playbook -i inventory.ini playbook.yml
   ```
   *Expected outcome*: Several tasks report `changed: [localhost]`.

3. **Step 3: Verification Run**  
   Run the playbook a second time:
   ```bash
   # Run again to demonstrate idempotency (should show 'ok' not 'changed')
   ansible-playbook -i inventory.ini playbook.yml
   ```
   *Expected outcome*: All tasks report `ok: [localhost]` and `changed=0`.

---

## 7. Health Check Verification

The `health-check.yml` playbook performs non-destructive assertions and diagnostic queries across the entire platform.

### Monitored Checks
1. **PostgreSQL Service**: Verifies `postgresql.service` is actively running via `service_facts`.
2. **Backend Health Endpoint**: Performs an HTTP GET request to `http://localhost:8080/api/v1/admin/dashboard/summary`.
3. **Docker Containers**: Queries running container names and statuses via `docker ps`.
4. **Listening Network Ports**: Examines listening sockets via `ss -tlnp`.
5. **Disk & Memory Usage**: Reports filesystem capacity (`df -h /`) and RAM allocation (`free -m`).
6. **Summary Report**: Prints a high-level summary banner displaying API status and timestamp.

### Running Health Check
```bash
# Health check
ansible-playbook -i inventory.ini health-check.yml
```

Sample output:
```
TASK [Health Check Summary] ****************************************************
ok: [localhost] => {
    "msg": "============================================\nSSPR Platform Health Check Summary\n============================================\nBackend API: PASS\nTimestamp: 2026-10-14T15:30:00Z\n============================================\n"
}
```

---

## 8. Rollback Procedure

When a deployment fails health checks or produces regressions, the platform can be quickly rolled back using `rollback-playbook.yml`.

### Rollback Process
1. **Automated Backup**: Backs up the current JAR deployment to `/opt/sspr-platform/backups/sspr-backend-0.0.1-SNAPSHOT.jar.<timestamp>`.
2. **Container Inspection**: Identifies the currently running Docker container and image tag.
3. **Graceful Teardown**: Stops and removes the active container.
4. **Image Tag Selection**: Retrieves the previous Docker image tag from local image history.
5. **Re-deployment**: Starts the container using the previous image tag attached to `sspr-network` with proper database credentials.
6. **Health Verification**: Polls the health endpoint `http://localhost:8080/api/v1/admin/dashboard/summary` up to 3 times to confirm recovery.
7. **Summary Display**: Outputs previous vs. current image tags and health status.

### Running Rollback
```bash
# Rollback
ansible-playbook -i inventory.ini rollback-playbook.yml
```

---

## 9. Security Best Practices

1. **Ansible Vault for Secrets**:  
   In production environments, encrypt sensitive credentials (`db_password`) using Ansible Vault:
   ```bash
   ansible-vault encrypt_string 'SecretPassword123!' --name 'db_password'
   ```
2. **Principle of Least Privilege**:  
   The application runs under the dedicated unprivileged user `sspr` rather than `root`.
3. **Strict Permissions**:  
   Application configuration files containing credentials (`application.properties`) are secured with mode `0640` and owned by `sspr:sspr`.
