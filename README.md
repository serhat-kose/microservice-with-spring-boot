# microservice-with-spring-boot

An ecommerce platform built as Spring Boot microservices: catalog, cart, inventory,
checkout saga, payment, shipment, and notifications, fronted by an API gateway with
JWT-based auth and backed by Eureka service discovery, Kafka, and per-service Postgres
databases.

## Services

| Service | Port | Purpose |
|---|---|---|
| eureka-server | 8761 | Service registry |
| api-gateway | 8080 | Single entry point, routing, JWT validation |
| auth-service | 8081 | Registration/login, issues JWTs |
| order-service | 8082 | Order records |
| product-service | 8083 | Product catalog |
| stock-service | 8084 | Inventory / stock reservation |
| payment-service | 8086 | Payment processing (simulated) |
| cart-service | 8087 | Shopping cart |
| saga-orchestrator | 8088 | Drives the checkout saga across the services above |
| shipment-service | 8089 | Shipment scheduling (simulated) |
| notification-service | 8090 | Order-event emails |

Each service has its own README with endpoint and configuration details.

## Architecture

- **Sync**: clients call api-gateway, which routes to services via Eureka
  (`lb://<service>`) and validates JWTs on every route except `/api/auth/**`.
- **Async**: checkout, stock reservation, payment, and shipment are coordinated by
  saga-orchestrator over Kafka. See `saga-orchestrator/README.md` for the full event
  flow, including the compensating transactions (stock release on payment failure,
  refund on shipment failure).
- **Data**: database-per-service - each stateful service owns its own Postgres
  database; there is no shared database.

## Running locally with Docker Compose

```
cp .env.example .env
# edit .env and set a real JWT_SECRET (and POSTGRES_PASSWORD if you want to change it)
docker compose up -d --build
```

This starts Kafka, one Postgres instance per stateful service, and all 11
application services. `eureka-server` should be reachable at
http://localhost:8761 once healthy, and all API traffic should go through
http://localhost:8080 (api-gateway).

## Running a single service locally (without Docker)

Each service can be run individually against local infrastructure (Postgres, Kafka,
eureka-server) via:

```
mvn -pl <service-name> -am spring-boot:run
```

## Building everything

```
mvn -q clean package
```

Every service shares the root `pom.xml` (Spring Boot 3.1.4 / Spring Cloud 2022.0.4),
so dependency versions stay in sync across the fleet.
