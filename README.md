# Computer Store Management API

Spring Boot backend for managing a computer store. The MySQL domain schema and the JPA persistence layer are in place; there are no business endpoints and no authentication yet.

## Stack

- Java 21
- Spring Boot 4.1.1
- Maven (Wrapper)
- MySQL 8.4 via Docker Compose
- Flyway
- Lombok
- Testcontainers (persistence tests)

## Prerequisites

- JDK 21
- Docker Desktop (or Docker Engine + Compose)
- Git

## Setup

1. Clone the repository and switch to this branch:

```powershell
git checkout feature/spring-boot-api
```

2. Copy the environment template and adjust values if needed:

```powershell
Copy-Item .env.example .env
```

3. Confirm Java 21 is active:

```powershell
java -version
```

## Database (Docker)

Validate Compose configuration:

```powershell
docker compose config
```

Start MySQL in the background:

```powershell
docker compose up -d
```

Wait until the container reports healthy:

```powershell
docker compose ps
```

Stop MySQL when finished:

```powershell
docker compose down
```

## Run the API

```powershell
.\mvnw.cmd spring-boot:run
```

Health check (proves the server is up):

```powershell
curl http://localhost:8080/api/health
```

Expected JSON shape:

```json
{
  "status": "UP",
  "application": "Computer Store Management API",
  "timestamp": "..."
}
```

## Domain schema

Flyway owns the schema (`V1__baseline.sql`, `V2__create_core_schema.sql`). The core tables are:

- `user_accounts` — login identity with a unique email and an `ADMIN`/`CUSTOMER` role
- `customers` — customer profile, one-to-one with a user account (admins need no profile)
- `products` — unique SKU, `DECIMAL` price, stock quantity, `HARDWARE`/`SOFTWARE` category, and a `version` column for optimistic locking
- `purchase_orders` — belongs to a customer, with `PENDING`/`COMPLETED`/`CANCELLED` status and a `DECIMAL` total
- `order_items` — order lines referencing a product, with a positive quantity and a `DECIMAL` unit price

Money is always `BigDecimal`/`DECIMAL`, timestamps are stored as UTC `DATETIME(6)`, and Spring Data JPA
repositories exist for the four aggregates.

## Tests

```powershell
.\mvnw.cmd test
```

Persistence tests start a throwaway MySQL 8.4 container via Testcontainers, so Docker must be running.
They never touch the Compose database.

## Project layout

```text
src/main/java/com/tamar/computerstore/
  config/
  controller/
  dto/
  entity/
  exception/
  repository/
  service/
src/main/resources/
  application.yml
  db/migration/
docker-compose.yml
.env.example
pom.xml
```

## Configuration notes

- MySQL host, port, database, username, and password are read from environment variables (`MYSQL_*`).
- `docker-compose.yml` supplies local defaults via `.env` / Compose substitution.
- `.env` is gitignored — do not commit real secrets.
- Hibernate `ddl-auto` is `none`; Flyway owns schema evolution.
