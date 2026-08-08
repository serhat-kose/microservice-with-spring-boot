# Build context must be the repository root (parent pom.xml lives there).
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY ecommerce-commons/pom.xml ecommerce-commons/pom.xml
COPY ecommerce-commons/src ecommerce-commons/src
COPY stock-service/pom.xml stock-service/pom.xml
COPY stock-service/src stock-service/src
RUN mvn -B -pl stock-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/stock-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]
