# zivAI Core Backend (LMS)

Spring Boot LMS service for the zivAI platform. This service exposes REST APIs for user, subject, assessment, enrolment, resources, chat, notifications, and development-plan workflows.

## Requirements
- Java 17
- Maven (or the included Maven Wrapper `./mvnw`)
- PostgreSQL (or a compatible GaussDB/Postgres instance)

## Key Libraries
- Spring Boot 4.0.2
- Spring WebMVC
- Spring Data JPA (Hibernate)
- PostgreSQL JDBC driver
- Lombok
- Springdoc OpenAPI (Swagger UI)

## Setup
1) **Database**
   - Create the schemas and lookup tables using the provided DDL:
     - `postgres_ddl.sql` (at the repo root)
   - Ensure the schemas `lms` and `lookups` exist.

2) **Configuration**
   - Update `src/main/resources/application.properties` with your DB credentials, or override with environment variables:
     - `spring.datasource.url`
     - `spring.datasource.username`
     - `spring.datasource.password`

3) **Run the service**
```bash
cd core-backend
./mvnw spring-boot:run
```

The API will be available at:
- `http://localhost:5000/api`

Swagger UI:
- `http://localhost:5000/swagger-ui/index.html`

## Seeded Users (dev only)
The app seeds a small set of users and lookup values on startup.

- Teacher: `teacher@zivai.local` / `TempPass123!`
- Student: `student@zivai.local` / `TempPass123!`
- Admin: `admin@zivai.local` / `TempPass123!`

> These are temporary credentials for development only.

## Notes
- Authentication is currently mocked in `AuthService` (embedded passwords).
- Development plan endpoints currently return empty collections as stubs.


## Environment variables
You can configure database access using environment variables (recommended for local/dev and CI):

- `ZIVAI_DB_URL` (default: `jdbc:postgresql://localhost:5432/zivai`)
- `ZIVAI_DB_USERNAME` (default: `zivai`)
- `ZIVAI_DB_PASSWORD` (default: empty)
  - Fallbacks also supported for password: `SPRING_DATASOURCE_PASSWORD`, then `PGPASSWORD`

### Quick setup (local dev)
1) **Create a local database** (PostgreSQL example):
```bash
createdb zivai
```

2) **Export env vars (Linux/macOS)**:
```bash
export ZIVAI_DB_URL="jdbc:postgresql://localhost:5432/zivai"
export ZIVAI_DB_USERNAME="zivai"
export ZIVAI_DB_PASSWORD="your_password"
```

3) **Export env vars (Windows PowerShell)**:
```powershell
$env:ZIVAI_DB_URL="jdbc:postgresql://localhost:5432/zivai"
$env:ZIVAI_DB_USERNAME="zivai"
$env:ZIVAI_DB_PASSWORD="your_password"
```

4) **Run the service**:
```bash
./mvnw spring-boot:run
```

### Using a local properties file (optional)
Create a local config file that is **gitignored**:
```bash
cp src/main/resources/application.example.properties src/main/resources/application-local.properties
```
Edit `application-local.properties` with your credentials and run with:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

An example config is provided at:
- `src/main/resources/application.example.properties`

Do **not** commit real credentials in `application.properties` or `.env` files.


### Troubleshooting (DB auth)
If you see:
`The server requested SCRAM-based authentication, but no password was provided`

Set `ZIVAI_DB_PASSWORD` (and the correct user) before starting the app:
```bash
export ZIVAI_DB_URL="jdbc:postgresql://<host>:<port>/<db>"
export ZIVAI_DB_USERNAME="<db_user>"
export ZIVAI_DB_PASSWORD="<db_password>"
./mvnw spring-boot:run
```

PowerShell:
```powershell
$env:ZIVAI_DB_URL="jdbc:postgresql://<host>:<port>/<db>"
$env:ZIVAI_DB_USERNAME="<db_user>"
$env:ZIVAI_DB_PASSWORD="<db_password>"
./mvnw spring-boot:run
```
