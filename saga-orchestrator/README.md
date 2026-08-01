# saga-orchestrator

Drives the order checkout saga by reacting to Kafka events and publishing the next
step, with each order's current step persisted to its own Postgres database
(`sagadb`) so the saga's progress survives a restart.

## Flow

```
cart-checkout -> order-create -> order-created -> stock-reserve-request
  -> stock-reserved -> payment-request -> payment-result
      SUCCESS -> shipment-request -> shipment-result
          SCHEDULED/SUCCESS -> order-completed
          otherwise -> payment-refund (compensation) + order-failed
      FAILURE -> stock-release (compensation) + order-failed
  -> stock-reservation-failed -> order-failed
```

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8088` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/sagadb` | Postgres connection |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_SERVER` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl saga-orchestrator -am spring-boot:run
```
