
FROM maven:3.9.0-eclipse-temurin-17 AS builder

WORKDIR /app


COPY pom.xml .
RUN mvn dependency:go-offline


COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true




FROM eclipse-temurin:17-jre

WORKDIR /app


COPY --from=builder /app/target/course-management-1.0.0.jar app.jar


EXPOSE 8080


HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1


ENTRYPOINT ["java", "-jar", "app.jar"]
