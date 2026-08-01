# Build context must be the repository root (parent pom.xml lives there).
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY cart-service/pom.xml cart-service/pom.xml
COPY cart-service/src cart-service/src
RUN mvn -B -pl cart-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/cart-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java","-jar","/app/app.jar"]
