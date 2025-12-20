FROM maven:3.8.6-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM openjdk:17-jre-slim
WORKDIR /app
# hedef jar ismi projeye göre değişebilir; wildcard kullanarak herhangi bir jar'ı alıyoruz
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","/app/app.jar"]
