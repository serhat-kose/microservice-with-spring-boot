# Build context must be the repository root (parent pom.xml lives there).
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY ecommerce-commons/pom.xml ecommerce-commons/pom.xml
COPY ecommerce-commons/src ecommerce-commons/src
COPY saga-orchestrator/pom.xml saga-orchestrator/pom.xml
COPY saga-orchestrator/src saga-orchestrator/src
RUN mvn -B -pl saga-orchestrator -am package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/saga-orchestrator/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java","-jar","/app/app.jar"]
