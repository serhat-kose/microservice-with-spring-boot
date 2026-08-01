# stock-service

Tracks inventory quantity per product in its own Postgres database (`stockdb`), and
handles stock reservation/release as part of the order saga.

## REST API

- `POST /api/stocks` - create a stock record for a product
- `GET /api/stocks` / `GET /api/stocks/{productId}` - list / fetch stock
- `GET /api/stocks/{productId}/available?quantity=N` - availability check
- `PUT /api/stocks/{productId}` - set absolute quantity
- `DELETE /api/stocks/{productId}` - remove a stock record

## Kafka

- Consumes `stock-reserve-request`, atomically decrements stock per item (see
  `StockRepository.decrementIfAvailable`, a single conditional `UPDATE` rather than a
  separate read-then-write, to avoid overselling under concurrent requests), and
  publishes `stock-reserved` or `stock-reservation-failed`.
- Consumes `stock-release` (the saga's compensating transaction when a later step
  fails) and increments stock back.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `STOCK_SERVICE_PORT` | `8084` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/stockdb` | Postgres connection |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_SERVER` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl stock-service -am spring-boot:run
```
