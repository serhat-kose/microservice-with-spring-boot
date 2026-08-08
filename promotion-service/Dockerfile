# Build context must be the repository root (parent pom.xml and ecommerce-commons live there).
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY ecommerce-commons/pom.xml ecommerce-commons/pom.xml
COPY ecommerce-commons/src ecommerce-commons/src
COPY promotion-service/pom.xml promotion-service/pom.xml
COPY promotion-service/src promotion-service/src
RUN mvn -B -pl promotion-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/promotion-service/target/*.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java","-jar","/app/app.jar"]
