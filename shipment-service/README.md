# shipment-service

Simulates shipment scheduling as a step in the order saga. Stateless - no database.

## Kafka

Consumes `shipment-request` (published by saga-orchestrator once payment succeeds)
and publishes `shipment-result` with status `SCHEDULED`.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `SHIPMENT_SERVICE_PORT` | `8089` | HTTP port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_SERVER` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl shipment-service -am spring-boot:run
```
