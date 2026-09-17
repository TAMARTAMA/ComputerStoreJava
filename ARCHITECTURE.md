# Architecture

Concise overview of the Computer Store Management API.

## Layering

```text
HTTP Client
    → Controllers (REST, validation annotations, OpenAPI metadata)
        → Services (business rules, @PreAuthorize, @Transactional)
            → Repositories (Spring Data JPA)
                → MySQL 8.4 (schema owned by Flyway)
```

- Controllers accept DTOs, return response DTOs, and do not talk to the database.
- Services own authorization beyond the HTTP filter (`@PreAuthorize`), inventory, and order lifecycle.
- Repositories map entities (`Product`, `PurchaseOrder`, `OrderItem`, `Customer`, `UserAccount`).
- Flyway migrations (`V1`, `V2`) own schema; Hibernate `ddl-auto` is `none`.

## JWT request flow

1. Client calls public `POST /api/auth/register` or `POST /api/auth/login`.
2. `AuthService` verifies credentials (BCrypt) and `JwtService` issues an HS256 JWT.
3. Later requests send `Authorization: Bearer <token>`.
4. `JwtAuthenticationFilter` validates the token, loads `UserDetails`, and populates the SecurityContext.
5. Method security enforces roles (`CUSTOMER` / `ADMIN`). Unauthenticated calls get JSON `401`; wrong role gets JSON `403`.

Swagger UI uses the same bearer scheme: paste the `accessToken` once under **Authorize**.

## Transactional order flow

1. Authenticated `CUSTOMER` posts `{ "items": [ { "productId", "quantity" } ] }` to `POST /api/orders`.
2. Inside one `@Transactional` boundary, `OrderService`:
   - Validates lines (non-empty, positive qty, no duplicate product ids)
   - Loads products, checks stock
   - Prices lines from current server-side `Product.price` (clients never send price/total/status)
   - Persists `PurchaseOrder` + `OrderItem` rows as `PENDING`
   - Decrements stock; `Product.@Version` optimistic locking maps conflicts to HTTP `409`
3. Admin may `PATCH /api/orders/{id}/status` to `COMPLETED` or `CANCELLED` from `PENDING` only.
4. Cancelling restores stock once in the same transaction; completing does not change stock again.

## Health and docs surfaces

| Surface | Purpose |
| --- | --- |
| `GET /api/health` | Legacy app health JSON (public, backward compatible) |
| `GET /actuator/health` | Actuator aggregate health (public; details never shown) |
| `GET /actuator/health/liveness` | Liveness probe |
| `GET /actuator/health/readiness` | Readiness probe |
| `GET /v3/api-docs` | OpenAPI 3 JSON |
| `/swagger-ui/index.html` | Interactive API explorer |

Only health-related Actuator endpoints are exposed over HTTP. Business `/api/**` routes stay authenticated except auth register/login and legacy health.
