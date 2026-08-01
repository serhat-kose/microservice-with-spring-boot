# order-service

Owns order records in its own Postgres database (`orderdb`). Orders are created only
via the `order-create` Kafka event (published either directly through `POST /api/orders`
or by saga-orchestrator after a cart checkout), never written synchronously from the
REST layer, so there is a single write path.

## REST API

- `POST /api/orders` - publish an `order-create` event (202 Accepted, not synchronous)
- `GET /api/orders/{id}` / `GET /api/orders/user/{userId}` - read orders

## Kafka

- Consumes `order-create`, persists the order, publishes `order-created`.
- Consumes `order-completed`, `order-failed`, `payment-result`, `stock-reserved` to
  update order status as the saga progresses.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8082` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/orderdb` | Postgres connection |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_CLIENT_DEFAULTZONE` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl order-service -am spring-boot:run
```
