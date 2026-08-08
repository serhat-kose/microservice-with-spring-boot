# review-service

Product reviews and ratings, in its own Postgres database (`reviewdb`).

## Verified purchases

A review is only accepted from a customer whose **completed order contained that product**.
Without that rule a product's rating can be manufactured by throwaway accounts, which is the
main thing that makes ratings worth showing at all.

Eligibility is not queried from order-service on demand. This service consumes
`order-completed` and keeps its own `verified_purchases` table, so reviewing does not depend
on order-service being reachable and the check is a local index lookup rather than a
cross-service call on every submission. Recording is idempotent, so a redelivered event is
harmless.

## Rating aggregation

Averages are **recomputed from the rows** on every change rather than maintained as a running
counter. A counter drifts from reality the first time a review is edited, deleted or
moderated; recomputing cannot. Only `APPROVED` **and** verified reviews count towards the
average.

The result is published as `product-rating-updated`, which product-service and search-service
consume to update their denormalised copies. Both apply it as a partial update:

- product-service writes only the rating columns, so it does not bump the product's
  optimistic-lock version and cause an unrelated seller edit to fail.
- search-service patches the indexed document, so it does not disturb the external version
  that orders catalog updates.

A lost publish is self-correcting - the next review on that product republishes the full
recomputed figures, so copies converge rather than drifting permanently.

## API

| Endpoint | Access |
|---|---|
| `GET /api/reviews/product/{productId}` | Public - approved reviews, paged |
| `GET /api/reviews/product/{productId}/summary` | Public - average, count, star breakdown |
| `GET /api/reviews/my` | Authenticated - the caller's own reviews |
| `POST /api/reviews` | Authenticated, verified purchase required |
| `PUT`/`DELETE /api/reviews/{id}` | Authenticated - own review only |
| `GET /api/reviews/moderation/pending`, `PUT /api/reviews/{id}/moderation` | ADMIN |

One review per customer per product, enforced by a unique constraint so two concurrent
submissions cannot both succeed.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `REVIEW_SERVICE_PORT` | `8092` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/reviewdb` | Postgres connection |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_SERVER` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl review-service -am spring-boot:run
```
