# Eureka Server
Basit Eureka Server.

Local çalıştırma:
- mvn spring-boot:run

Docker:
- docker build -t eureka-server .
- docker run -p 8761:8761 --name eureka-server eureka-server

Not: docker-compose kullanıyorsanız servis ismini `eureka-server` yapın, diğer servislerin `eureka.client.serviceUrl.defaultZone` buna işaret etmeli.
