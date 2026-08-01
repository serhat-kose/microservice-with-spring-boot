# payment-service

Simulates payment processing as a step in the order saga. Stateless - no database.

## Kafka

- Consumes `payment-request` (published by saga-orchestrator after stock is reserved)
  and publishes `payment-result` with status `SUCCESS`.
- Consumes `payment-refund` (the saga's compensating transaction when shipment fails)
  and publishes `payment-refund-result`.

## REST API

- `POST /api/payments/process` - process a payment directly (used for manual testing)

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `PAYMENT_SERVICE_PORT` | `8086` | HTTP port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_SERVER` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl payment-service -am spring-boot:run
```
