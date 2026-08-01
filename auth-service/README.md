# auth-service

Issues and validates JWT access/refresh tokens for the ecommerce platform. Stores users in its own Postgres database (`authdb`).

## Endpoints

- `POST /api/auth/register` - create a user, returns an access/refresh token pair
- `POST /api/auth/login` - authenticate, returns an access/refresh token pair
- `POST /api/auth/refresh` - exchange a refresh token for a new token pair
- `GET /api/users/{id}`, `GET /api/users/by-username/{username}` - fetch a user profile (requires a valid access token)

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
