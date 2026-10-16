# Selenium Test Plan

## Scope

The Selenium suite exercises the critical browser journeys in the Password Reset Platform through the React UI and the running Spring Boot API.

## Test Data

| Data | Purpose |
| --- | --- |
| `devsecops@test.com` | Seeded active user for the successful reset request |
| `missing-{timestamp}@example.com` | Unknown address used to verify anti-enumeration behavior |
| `selenium-user-{timestamp}@example.com` | New admin-created user |
| `workflow-{timestamp}@example.com` | User whose request is searched and moved through the workflow |

## Critical Journeys

| ID | Journey | Assertions |
| --- | --- | --- |
| SSPR-01 | Existing user submits a reset request | Generic success message is displayed |
| SSPR-02 | Unknown user submits a reset request | The same generic message is displayed, preventing account enumeration |
| SSPR-03 | Administrator creates a user | Success alert appears and dashboard total users is greater than zero |
| SSPR-04 | Administrator searches for a reset request and completes it | Search result contains the test email; status changes from `PENDING` to `APPROVED` to `COMPLETED` |

## Local Execution

Start Docker Desktop, then start PostgreSQL from the repository root:

```bash
docker compose up -d postgres-db
```

Start the backend in one terminal:

```bash
cd sspr-backend
./mvnw spring-boot:run
```

Start the frontend in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Run the Selenium integration suite in a third terminal:

```bash
cd sspr-backend
./mvnw verify -Pselenium
```

The browser runs headlessly. Selenium Manager resolves a compatible ChromeDriver. To test another frontend URL, pass `-Dselenium.baseUrl=http://localhost:5173`.

## Reports and Failure Evidence

- Maven Failsafe XML and text reports: `sspr-backend/target/failsafe-reports/`
- Failure screenshots: `sspr-backend/target/selenium-screenshots/`

The screenshot extension runs after each failed Selenium test and writes a PNG named after the test class and method. Browser startup failures before a WebDriver exists cannot produce a screenshot; Maven still records those failures in the Failsafe report.