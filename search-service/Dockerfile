# Build context must be the repository root (parent pom.xml and ecommerce-commons live there).
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY ecommerce-commons/pom.xml ecommerce-commons/pom.xml
COPY ecommerce-commons/src ecommerce-commons/src
COPY search-service/pom.xml search-service/pom.xml
COPY search-service/src search-service/src
RUN mvn -B -pl search-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/search-service/target/*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java","-jar","/app/app.jar"]
