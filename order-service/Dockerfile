FROM maven:3.9.2-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# ...existing code...
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/target/order-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]

