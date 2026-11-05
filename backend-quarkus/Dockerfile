# Stage 1: build one runnable jar
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml ./
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package

# Stage 2: ship only a JRE and the jar
FROM eclipse-temurin:25-jre
COPY --from=build /src/target/app-runner.jar /app.jar
USER 65532
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
