# auth-service

Issues and validates JWT access/refresh tokens for the ecommerce platform. Stores users in its own Postgres database (`authdb`).

## Endpoints

- `POST /api/auth/register` - create a user (always as `CUSTOMER`), returns an access/refresh token pair
- `POST /api/auth/login` - authenticate, returns an access/refresh token pair
- `POST /api/auth/refresh` - exchange a **refresh** token for a new token pair (an access token is rejected here)
- `GET /api/users/me` - the caller's own profile
- `GET /api/users/me/addresses`, `POST`, `PUT /{id}`, `DELETE /{id}` - the caller's address book
- `GET /api/users/{id}`, `GET /api/users/by-username/{username}` - **ADMIN only**

## Roles

`CUSTOMER` (default on self-registration), `SELLER` (manages catalog/stock), `ADMIN`
(full management). Roles are carried in the JWT and forwarded downstream by the gateway
as `X-User-Roles`; `SELLER`/`ADMIN` are granted out-of-band so that signing up cannot
escalate into catalog management.

## Authentication model

This service issues tokens but does not validate them for its own protected endpoints -
the gateway validates the JWT once and forwards the caller's identity as `X-User-Id` /
`X-User-Name` / `X-User-Roles`. Those headers are only trustworthy because the gateway
strips any client-supplied copy first, so this service must never be exposed directly.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8081` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/authdb` | Postgres connection |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `postgres` / `postgres` | Postgres credentials |
| `JWT_SECRET` | insecure default | HMAC signing key for JWTs - **must** be overridden with a long random value outside local development, and must match the value api-gateway is configured with |
| `EUREKA_SERVER` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl auth-service -am spring-boot:run
```
