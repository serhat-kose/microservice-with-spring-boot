# notification-service

Sends order-lifecycle emails and keeps a record of every notification attempt in its
own Postgres database (`notificationdb`).

## Kafka

Consumes `order-completed`, `order-failed`, `stock-reserved`, and `payment-result`
(only forwards on SUCCESS), looks up the recipient's email from auth-service when the
event doesn't already carry one, and sends an email via `spring-boot-starter-mail`.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `NOTIFICATION_SERVICE_PORT` | `8090` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/notificationdb` | Postgres connection |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `AUTH_SERVICE_URL` | `http://auth-service:8081` | Used to enrich notifications with the user's name/email |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | SMTP server (defaults target a local dev SMTP catcher, e.g. MailHog) |
| `EUREKA_SERVER` | `http://eureka-server:8761/eureka/` | Service registry URL |

## Run locally

```
mvn -pl notification-service -am spring-boot:run
```
