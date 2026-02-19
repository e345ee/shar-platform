FROM maven:3.9.0-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/course-management-1.0.0.jar app.jar

# Backend port is configurable via build arg/env
ARG SERVER_PORT
ENV SERVER_PORT=${SERVER_PORT}

EXPOSE ${SERVER_PORT}

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3     CMD sh -c "curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1"

ENTRYPOINT ["java", "-jar", "app.jar"]
