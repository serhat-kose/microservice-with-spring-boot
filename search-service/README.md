# search-service

Elasticsearch-backed catalog read model. This is the CQRS read side of the catalog: it
never writes to product-service's database and is never written to directly - it is built
entirely from `product-created` / `product-updated` / `product-deleted` events.

## Why it exists separately

`GET /api/products` in product-service can page and filter, but relevance ranking, typo
tolerance and facet counts are not things a relational query does well. Search traffic is
also the heaviest read on a storefront, so keeping it off the catalog's primary database
means a traffic spike on search cannot exhaust the connection pool that product writes need.

## API

`GET /api/search/products` - public, no authentication required.

| Parameter | Meaning |
|---|---|
| `q` | Free text; matched against name (boosted 3x) and description, with `AUTO` fuzziness |
| `categoryId` | Restrict to a category |
| `brand` | Repeatable; multiple values are OR-ed |
| `attribute` | Repeatable; variant attribute values such as `Blue`, `XL` |
| `minPrice`, `maxPrice`, `minRating` | Range filters |
| `sort` | `relevance` (default), `price_asc`, `price_desc`, `rating`, `newest` |
| `page`, `size` | Paging; `size` capped at 60 and the page clamped to Elasticsearch's 10 000 result window |

The response carries the hits plus facet counts (brands, categories, attributes) and the
min/max price of the matching set, so a results page renders its filter sidebar from one
request.

## Indexing behaviour

- Only `ACTIVE` products are indexed. `DRAFT` and `INACTIVE` products are removed from the
  index rather than filtered at query time, so no query pays for a status filter.
- Documents use Elasticsearch **external versioning**, with the source event's timestamp as
  the version. Kafka only orders within a partition and retries can replay older messages,
  so this makes an out-of-order update a no-op instead of overwriting newer data.
- Documents are denormalised (brand/category names, primary image, rating) so a search
  returns fully-formed result cards without calling back into product-service per hit.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `SEARCH_SERVICE_PORT` | `8091` | HTTP port |
| `ELASTICSEARCH_URIS` | `http://localhost:9200` | Elasticsearch endpoint |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_SERVER` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl search-service -am spring-boot:run
```
