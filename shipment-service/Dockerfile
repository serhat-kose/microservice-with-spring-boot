# Build context must be the repository root (parent pom.xml lives there).
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY shipment-service/pom.xml shipment-service/pom.xml
COPY shipment-service/src shipment-service/src
RUN mvn -B -pl shipment-service -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/shipment-service/target/*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java","-jar","/app/app.jar"]
