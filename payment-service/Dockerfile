# Build context must be the repository root (parent pom.xml lives there).
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY ecommerce-commons/pom.xml ecommerce-commons/pom.xml
COPY ecommerce-commons/src ecommerce-commons/src
COPY payment-service/pom.xml payment-service/pom.xml
COPY payment-service/src payment-service/src
RUN mvn -B -pl payment-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/payment-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java","-jar","/app/app.jar"]
