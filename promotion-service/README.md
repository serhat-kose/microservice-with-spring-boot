# promotion-service

Coupons and discounts, in its own Postgres database (`promotiondb`).

## Concurrency and idempotency

Two things here are easy to get wrong under real traffic, and both are handled at the
database rather than in service code:

- **Usage limits.** Consuming a use is a single conditional `UPDATE`
  (`SET used_count = used_count + 1 WHERE used_count < usage_limit`). A read-check-write
  would let concurrent checkouts all read the same pre-increment count and together exceed
  the limit.
- **Redemption.** A unique constraint on (coupon, order) makes redeeming idempotent, so a
  checkout retried after a timeout returns the discount already recorded instead of
  consuming a second use.

A fixed-amount coupon larger than the basket is capped at the basket total, so the payable
amount can never go negative.

## Saga participation

Consumes `order-failed` and releases the redemption for that order, so a customer does not
lose a coupon use on an order that never completed. Once the redemption row is gone a
redelivered event finds nothing to release, so the compensation is safe to repeat.

## API

| Endpoint | Access |
|---|---|
| `POST /api/promotions/validate` | Authenticated - quotes a code against a basket without consuming it |
| `POST /api/promotions/redeem` | Authenticated - consumes the coupon for an order |
| `GET`/`POST /api/promotions`, `PUT`/`DELETE /api/promotions/{id}` | ADMIN |

`validate` returns `valid: false` with a readable reason rather than an error status -
"this coupon has expired" is a normal outcome of a shopper trying a code, not a failure.
The caller is always taken from the verified identity, so nobody can inspect another
customer's remaining allowance.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `PROMOTION_SERVICE_PORT` | `8093` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/promotiondb` | Postgres connection |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_SERVER` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl promotion-service -am spring-boot:run
```
