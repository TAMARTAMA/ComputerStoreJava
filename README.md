# Computer Store Management API

Spring Boot backend for managing a computer store. The MySQL domain schema, JPA persistence,
stateless JWT authentication, product catalog API, and transactional order processing are in
place.

## Stack

- Java 21
- Spring Boot 4.1.1
- Maven (Wrapper)
- MySQL 8.4 via Docker Compose
- Flyway
- Spring Security 7 with HS256 JWTs (Nimbus JOSE via `spring-security-oauth2-jose`)
- Lombok
- Testcontainers (persistence, authentication, product, and order API tests)

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

3. Generate a JWT signing secret and put it in `.env` as `JWT_SECRET`. There is no fallback in
   source or in `.env.example`, so the application refuses to start without one. It must be at
   least 32 characters (256 bits) for HS256:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

   Or, with OpenSSL available:

```powershell
openssl rand -base64 48
```

4. Confirm Java 21 is active:

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

Spring Boot imports an optional local `.env` file (`spring.config.import: optional:file:.env[.properties]`),
so `JWT_SECRET` and the other keys in `.env` are available without manually exporting them in the
shell. Docker Compose continues to read the same `.env` for MySQL. Never commit a real `.env`.

```powershell
.\mvnw.cmd spring-boot:run
```

You can still override any value by exporting an environment variable in the shell; process
environment variables take precedence over the file.

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

## Authentication

The API is stateless: there is no session and no login page. Clients register or log in once,
then send the returned token as `Authorization: Bearer <token>` on every later request.

| Endpoint | Access |
| --- | --- |
| `GET /api/health` | public |
| `POST /api/auth/register` | public, creates a `CUSTOMER` account plus its customer profile |
| `POST /api/auth/login` | public, returns a signed JWT |
| `GET /api/auth/me` | requires a valid JWT |
| `GET /api/products` and `GET /api/products/{id}` | authenticated (`CUSTOMER` or `ADMIN`) |
| `POST` / `PUT` / `DELETE` `/api/products` | `ADMIN` only |
| Order endpoints | see [Orders](#orders) |
| everything else under `/api/**` | requires a valid JWT |

Register and receive a token:

```powershell
curl -X POST http://localhost:8080/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"email":"dana@example.com","password":"Str0ng-Passw0rd!","firstName":"Dana","lastName":"Levi"}'
```

Call a protected endpoint:

```powershell
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer <accessToken>"
```

Notes:

- Passwords are stored only as BCrypt hashes (strength 12) and never appear in a response.
- Password length is at least 12 characters and at most 72 UTF-8 bytes (BCrypt's input limit).
- Login answers every failure with the same `401` body, so it cannot be used to discover which
  addresses are registered.
- Unauthenticated and forbidden requests return JSON `401` and `403` envelopes, never HTML.
- CSRF protection is disabled because the API accepts only bearer tokens, which a browser never
  attaches automatically.

### Development admin account

There is no admin in any migration and no admin password in the repository. To create one locally,
run the application with the `local` profile and supply both variables (via `.env` or the shell):

```powershell
# In .env (recommended):
# LOCAL_ADMIN_EMAIL=admin@example.com
# LOCAL_ADMIN_PASSWORD=<a local-only password>

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The bootstrap is idempotent: it skips silently when either variable is empty and leaves an existing
account with that email untouched. Log in afterward with those credentials to obtain an ADMIN JWT
for product write operations and order status updates.

## Product catalog

All product endpoints require `Authorization: Bearer <accessToken>`. Reads are available to both
roles; creates, updates, and deletes are enforced for `ADMIN` in the service layer with
`@PreAuthorize` (not only at the HTTP filter).

| Method | Path | Roles |
| --- | --- | --- |
| `GET` | `/api/products` | `CUSTOMER`, `ADMIN` |
| `GET` | `/api/products/{id}` | `CUSTOMER`, `ADMIN` |
| `POST` | `/api/products` | `ADMIN` |
| `PUT` | `/api/products/{id}` | `ADMIN` |
| `DELETE` | `/api/products/{id}` | `ADMIN` |

List products (optional `category`, case-insensitive `q` against name or SKU, pagination):

```powershell
curl "http://localhost:8080/api/products?category=HARDWARE&q=board&page=0&size=20" `
  -H "Authorization: Bearer <accessToken>"
```

Default page size is 20; the maximum is 100. Results are sorted by `name` ascending, then `id`.

Create a product as ADMIN:

```powershell
curl -X POST http://localhost:8080/api/products `
  -H "Authorization: Bearer <adminAccessToken>" `
  -H "Content-Type: application/json" `
  -d '{"sku":"SKU-GPU-001","name":"Graphics Card","description":"High-end GPU","price":1299.99,"stockQuantity":7,"category":"HARDWARE"}'
```

Update requires the `version` value from the last read (optimistic locking). A stale `version`
returns `409 Conflict`. Deleting a product that is still referenced by an order item also returns
`409`.

## Orders

Order placement is a single `@Transactional` operation: validate products and stock, calculate
line prices and totals from the current server-side `Product.price`, create the order and items,
decrement stock, and save. Clients never supply price, total, customer id, or status. Concurrent
stock updates rely on Product `@Version` optimistic locking and map to HTTP `409`.

### Endpoints and authorization

| Method | Path | Roles | Notes |
| --- | --- | --- | --- |
| `POST` | `/api/orders` | `CUSTOMER` | Body: non-empty `{ "items": [ { "productId", "quantity" } ] }` |
| `GET` | `/api/orders/me` | `CUSTOMER` | Current customer's orders, newest first, paginated |
| `GET` | `/api/orders/{id}` | `CUSTOMER`, `ADMIN` | Customer may read only own order; another customer's order returns `404` |
| `GET` | `/api/orders` | `ADMIN` | All orders, newest first; optional `status` filter |
| `PATCH` | `/api/orders/{id}/status` | `ADMIN` | Body: `{ "status": "COMPLETED" \| "CANCELLED" }` |

There is no order deletion endpoint.

### Lifecycle

- New orders start as `PENDING`.
- Allowed transitions only: `PENDING -> COMPLETED`, `PENDING -> CANCELLED`.
- Any other transition returns HTTP `409`.
- Cancelling a `PENDING` order restores stock once in the same transaction.
- Completing an order does not change stock again (stock was already reserved at placement).

### Example: place an order

```powershell
curl -X POST http://localhost:8080/api/orders `
  -H "Authorization: Bearer <customerAccessToken>" `
  -H "Content-Type: application/json" `
  -d '{"items":[{"productId":1,"quantity":2}]}'
```

### Example: admin status update

```powershell
curl -X PATCH http://localhost:8080/api/orders/1/status `
  -H "Authorization: Bearer <adminAccessToken>" `
  -H "Content-Type: application/json" `
  -d '{"status":"COMPLETED"}'
```

### Order error mapping

| Situation | HTTP |
| --- | --- |
| Order not found / another customer's order | `404` |
| Insufficient stock | `409` |
| Invalid status transition | `409` |
| Optimistic inventory conflict | `409` |
| Invalid order request (empty items, duplicate product ids, non-positive qty) | `400` |

## Local fictional demo data

Demo seeding is **off by default** and never runs in tests, CI, the default profile, or production.
It activates only when **all** of the following are true:

1. Spring profile `local` is active
2. `APP_DEMO_DATA_ENABLED=true`
3. `DEMO_CUSTOMER_PASSWORD` is set to a strong password (at least 12 characters)

All seeded names, emails, phones, and addresses are **fictional** reserved demo values (for example
`noa.levi@example.test`, `+972-50-555-0101`). They are not real people or credentials.

Set these in `.env` (example):

```env
APP_DEMO_DATA_ENABLED=true
DEMO_CUSTOMER_PASSWORD=<choose-a-strong-local-only-password>
LOCAL_ADMIN_EMAIL=admin@example.com
LOCAL_ADMIN_PASSWORD=<choose-a-strong-local-only-password>
```

Then start with the local profile:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

What gets seeded (idempotent — safe to re-run; existing SKUs, emails, and demo orders are left alone):

- About 18 computer-store products (`DEMO-*` SKUs) across hardware and software
- Two fictional CUSTOMER accounts (`noa.levi@example.test`, `yonatan.cohen@example.test`) using `DEMO_CUSTOMER_PASSWORD`
- Several historical orders created through the real order service and status transitions, including at least one `COMPLETED`, one `PENDING`, and one `CANCELLED` (status changes require the local admin bootstrap)

## Domain schema

Flyway owns the schema (`V1__baseline.sql`, `V2__create_core_schema.sql`). The core tables are:

- `user_accounts` — login identity with a unique email and an `ADMIN`/`CUSTOMER` role
- `customers` — customer profile, one-to-one with a user account (admins need no profile)
- `products` — unique SKU, `DECIMAL` price, stock quantity, `HARDWARE`/`SOFTWARE` category, and a `version` column for optimistic locking
- `purchase_orders` — belongs to a customer, with `PENDING`/`COMPLETED`/`CANCELLED` status and a `DECIMAL` total
- `order_items` — order lines referencing a product, with a positive quantity and a `DECIMAL` unit price

Money is always `BigDecimal`/`DECIMAL`, timestamps are stored as UTC `DATETIME(6)`, and Spring Data JPA
repositories exist for the aggregates.

## Tests

```powershell
.\mvnw.cmd clean verify
```

Persistence, authentication, product, and order API tests start a throwaway MySQL 8.4 container via
Testcontainers, so Docker must be running. They never touch the Compose database. Surefire injects
a throwaway `JWT_SECRET` into the test JVM, so no local secret is needed to run the suite.

## Project layout

```text
src/main/java/com/tamar/computerstore/
  config/
  controller/
  dto/
  entity/
  exception/
  mapper/
  repository/
  security/
  service/
  validation/
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
- Spring Boot also imports `.env` when present (`optional:file:.env[.properties]`).
- `.env` is gitignored — do not commit real secrets.
- Hibernate `ddl-auto` is `none`; Flyway owns schema evolution.
- `JWT_SECRET` and `JWT_EXPIRATION` are the only sources of JWT configuration; `JWT_SECRET` has no
  default and is validated at startup.
- `LOCAL_ADMIN_EMAIL` and `LOCAL_ADMIN_PASSWORD` apply only under the `local` profile.
- `APP_DEMO_DATA_ENABLED` and `DEMO_CUSTOMER_PASSWORD` apply only under the `local` profile with the
  demo flag explicitly set to `true`.
